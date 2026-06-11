/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms.parser.speclib;

import org.apache.logging.log4j.Logger;
import org.labkey.api.util.logging.LogHelper;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Builds the metadata cache for an EncyclopeDIA {@code .elib} spectrum library (see {@link ElibCache}).
 *
 * <p>The cache is a small SQLite file holding just the per-row metadata the spectrum viewer needs
 * ({@code PeptideSeq, PeptideModSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score}) with
 * covering indexes, so the viewer's lookups become index-only reads instead of hundreds of scattered
 * reads of uncovered columns in the {@code .elib}. Peak blobs are intentionally not copied; they are
 * still read from the {@code .elib} one at a time.
 *
 * <p>The cache table is named {@code entries} with the same column names as the {@code .elib} so the
 * reader can run identical metadata SQL against either source.
 *
 * <p>This class only knows how to write a cache given a source and a target path. Deciding whether a
 * cache is needed or current, and triggering builds, is {@link ElibCache}'s job.
 */
public class ElibCacheWriter
{
    private static final Logger LOG = LogHelper.getLogger(ElibCacheWriter.class, "Builds the metadata cache for large EncyclopeDIA .elib spectrum libraries");

    // Bump when the cache schema or contents change so that out-of-date caches are detected and rebuilt.
    static final int FORMAT_VERSION = 1;

    // Number of rows per insert batch when building the cache.
    private static final int BUILD_BATCH_SIZE = 5000;

    private ElibCacheWriter() {}

    /**
     * Builds the cache for {@code source} into a temp file in {@code target}'s directory, then atomically
     * renames it onto {@code target} so a partial build is never visible to readers. Does nothing if the
     * source is missing or empty.
     */
    public static void build(File source, File target) throws SQLException, IOException
    {
        if (!source.exists() || source.length() == 0)
        {
            return;
        }

        long start = System.currentTimeMillis();
        // Capture the source size/mtime up front so a concurrent re-import that changes the file is caught
        // by the staleness check rather than baked in as "current".
        long sourceSize = source.length();
        long sourceModified = source.lastModified();

        Path dir = target.getAbsoluteFile().toPath().getParent();
        Path temp = Files.createTempFile(dir, target.getName() + ".", ".tmp");

        long rows = 0;
        try
        {
            try (Connection src = LibSpectrumReader.getLibConnection(source.getAbsolutePath());
                 Connection dst = openWritableConnection(temp.toFile().getAbsolutePath()))
            {
                initSchema(dst);
                rows = copyEntries(src, dst);
                createIndexes(dst);
                writeCacheInfo(dst, sourceSize, sourceModified);
            }

            try
            {
                Files.move(temp, target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException atomicFailed)
            {
                // Some filesystems do not support atomic moves; fall back to a plain replace.
                Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            LOG.info("Built spectrum library cache " + target.getAbsolutePath() + " (" + rows + " rows) in "
                    + (System.currentTimeMillis() - start) + " ms");
        }
        finally
        {
            Files.deleteIfExists(temp); // No-op if the move succeeded.
        }
    }

    private static void initSchema(Connection dst) throws SQLException
    {
        try (Statement stmt = dst.createStatement())
        {
            // Temp file we rebuild on any failure, so skip the journal and fsync for a faster build.
            stmt.execute("PRAGMA journal_mode = OFF");
            stmt.execute("PRAGMA synchronous = OFF");
            // Column names match the .elib "entries" table so the reader runs identical metadata SQL
            // against either the .elib or this cache. Peak blobs are intentionally not copied.
            stmt.execute("CREATE TABLE entries ("
                    + "PeptideSeq TEXT, "
                    + "PeptideModSeq TEXT, "
                    + "PrecursorCharge INTEGER, "
                    + "PrecursorMz REAL, "
                    + "SourceFile TEXT, "
                    + "RTInSeconds REAL, "
                    + "Score REAL)");
        }
    }

    private static long copyEntries(Connection src, Connection dst) throws SQLException
    {
        String selectSql = "SELECT PeptideSeq, PeptideModSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score FROM entries";
        String insertSql = "INSERT INTO entries (PeptideSeq, PeptideModSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score) VALUES (?, ?, ?, ?, ?, ?, ?)";

        boolean autoCommit = dst.getAutoCommit();
        dst.setAutoCommit(false);
        long rows = 0;
        try (Statement selectStmt = src.createStatement();
             ResultSet rs = selectStmt.executeQuery(selectSql);
             PreparedStatement insert = dst.prepareStatement(insertSql))
        {
            int batch = 0;
            while (rs.next())
            {
                insert.setString(1, rs.getString("PeptideSeq"));
                insert.setString(2, rs.getString("PeptideModSeq"));
                insert.setInt(3, rs.getInt("PrecursorCharge"));
                insert.setDouble(4, rs.getDouble("PrecursorMz"));
                insert.setString(5, rs.getString("SourceFile"));
                insert.setDouble(6, rs.getDouble("RTInSeconds"));
                insert.setDouble(7, rs.getDouble("Score"));
                insert.addBatch();
                rows++;
                if (++batch >= BUILD_BATCH_SIZE)
                {
                    insert.executeBatch();
                    batch = 0;
                }
            }
            if (batch > 0)
            {
                insert.executeBatch();
            }
            dst.commit();
        }
        catch (SQLException e)
        {
            dst.rollback();
            throw e;
        }
        finally
        {
            dst.setAutoCommit(autoCommit);
        }
        return rows;
    }

    private static void createIndexes(Connection dst) throws SQLException
    {
        try (Statement stmt = dst.createStatement())
        {
            // Covers getMatchingModSeqLookupSql: SELECT PeptideModSeq FROM entries WHERE PeptideSeq = ?
            stmt.execute("CREATE INDEX idx_pepseq ON entries (PeptideSeq, PeptideModSeq)");
            // Covers readElibSpectrum (WHERE PeptideModSeq=? AND PrecursorCharge=? [AND SourceFile=?]) and
            // readRetentionTimes (WHERE PeptideModSeq=?). Leading WHERE columns first, then the remaining
            // selected columns so both reads are index-only.
            stmt.execute("CREATE INDEX idx_modseq ON entries "
                    + "(PeptideModSeq, PrecursorCharge, SourceFile, PrecursorMz, RTInSeconds, Score)");
        }
    }

    private static void writeCacheInfo(Connection dst, long sourceSize, long sourceModified) throws SQLException
    {
        try (Statement stmt = dst.createStatement())
        {
            stmt.execute("CREATE TABLE cache_info (SourceSize INTEGER, SourceModified INTEGER, FormatVersion INTEGER)");
        }
        try (PreparedStatement stmt = dst.prepareStatement("INSERT INTO cache_info (SourceSize, SourceModified, FormatVersion) VALUES (?, ?, ?)"))
        {
            stmt.setLong(1, sourceSize);
            stmt.setLong(2, sourceModified);
            stmt.setInt(3, FORMAT_VERSION);
            stmt.executeUpdate();
        }
    }

    private static Connection openWritableConnection(String filePath) throws SQLException
    {
        SQLiteConfig config = new SQLiteConfig();
        return DriverManager.getConnection("jdbc:sqlite:/" + filePath, config.toProperties());
    }
}
