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
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.logging.LogHelper;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates the metadata cache for an EncyclopeDIA {@code .elib} spectrum library: it decides whether a
 * usable cache exists, opens it, and otherwise triggers a background build via {@link ElibCacheWriter}.
 *
 * <p>An {@code .elib} on network storage (GPFS) is slow to read for the spectrum viewer because the
 * per-peptide row metadata ({@code Score, RTInSeconds, SourceFile, ...}) is not in the index, so each of
 * the hundreds of matching rows is a separate scattered read. The cache holds just that metadata with
 * covering indexes, so the same queries become index-only reads of a few contiguous pages.
 *
 * <p>The cache is named {@code <library>.elib.cache} and lives in the same directory as the {@code .elib}.
 * Its lifecycle is fully automatic: each Skyline run has its own extraction directory, so deleting or
 * re-importing the document removes or clears the cache along with the {@code .elib}; no bookkeeping,
 * refcounting, or cleanup task is needed.
 *
 * <p>The cache is built lazily on first access, on a background thread, single-flight (one build per file
 * at a time). While a build is in progress the caller reads from the {@code .elib} as before.
 */
public class ElibCache
{
    private static final Logger LOG = LogHelper.getLogger(ElibCache.class, "Coordinates the metadata cache that speeds up reading large EncyclopeDIA .elib spectrum libraries");

    private static final String CACHE_SUFFIX = ".cache";

    // Only build a cache for libraries large enough that the scattered reads are actually slow. This
    // matches the size at which guests are asked to log in (see LibrarySpectrumMatchGetter); smaller
    // libraries are already fast to read in place, so a cache would just add clutter.
    private static final long MIN_SOURCE_SIZE = 500L * 1024 * 1024; // 500 MB

    // One build per cache file at a time. Holds the absolute cache path while a build is running.
    private static final Set<String> BUILDS_IN_PROGRESS = ConcurrentHashMap.newKeySet();

    // Low-priority background pool for cache builds (one scan of a multi-GB .elib can take tens of seconds).
    private static final JobRunner BUILDER = new JobRunner("ElibCacheBuilder", 2);

    private ElibCache() {}

    /**
     * Returns an open, read-only connection to a valid cache for the given local {@code .elib} path, or
     * null if no usable cache exists. When null is returned and the library is large enough to be worth
     * caching, a background build is started (single-flight) so subsequent reads can use the cache; the
     * current read should fall back to the {@code .elib} itself.
     *
     * <p>The caller owns the returned connection and must close it.
     */
    @Nullable
    public static Connection openIfReady(String localLibPath)
    {
        if (localLibPath == null || !localLibPath.toLowerCase().endsWith(".elib"))
        {
            return null;
        }

        File source = new File(localLibPath);
        File cache = new File(localLibPath + CACHE_SUFFIX);

        if (cache.exists())
        {
            Connection conn = openValidCache(cache, source);
            if (conn != null)
            {
                return conn; // Valid cache: serve metadata from it.
            }
            // Present but stale or unreadable: fall through to rebuild.
        }

        requestBuild(source, cache);
        return null;
    }

    /**
     * Opens the cache read-only and returns the connection if it is current for the source file (matching
     * size, modified time, and format version). Otherwise closes the connection and returns null.
     */
    @Nullable
    private static Connection openValidCache(File cache, File source)
    {
        Connection conn = null;
        try
        {
            conn = LibSpectrumReader.getLibConnection(cache.getAbsolutePath());
            if (isCurrent(conn, source))
            {
                return conn;
            }
            conn.close();
            return null;
        }
        catch (SQLException e)
        {
            LOG.debug("Could not open spectrum library cache " + cache.getAbsolutePath() + "; will rebuild", e);
            closeQuietly(conn);
            return null;
        }
    }

    private static boolean isCurrent(Connection conn, File source)
    {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SourceSize, SourceModified, FormatVersion FROM cache_info"))
        {
            if (rs.next())
            {
                return rs.getInt("FormatVersion") == ElibCacheWriter.FORMAT_VERSION
                        && rs.getLong("SourceSize") == source.length()
                        && rs.getLong("SourceModified") == source.lastModified();
            }
        }
        catch (SQLException e)
        {
            // Missing cache_info table (partial or foreign file): treat as not current.
            return false;
        }
        return false;
    }

    private static void requestBuild(File source, File cache)
    {
        if (source.length() < MIN_SOURCE_SIZE)
        {
            return; // Small library: reading in place is already fast.
        }

        String key = cache.getAbsolutePath();
        if (!BUILDS_IN_PROGRESS.add(key))
        {
            return; // A build for this cache is already running.
        }

        BUILDER.execute(() -> {
            try
            {
                ElibCacheWriter.build(source, cache);
            }
            catch (Exception e)
            {
                LOG.warn("Could not build spectrum library cache for " + source.getAbsolutePath(), e);
            }
            finally
            {
                BUILDS_IN_PROGRESS.remove(key);
            }
        });
    }

    private static void closeQuietly(@Nullable Connection conn)
    {
        if (conn != null)
        {
            try
            {
                conn.close();
            }
            catch (SQLException ignored) {}
        }
    }
}
