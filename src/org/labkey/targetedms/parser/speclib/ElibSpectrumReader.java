/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.targetedms.parser.speclib.LibSpectrum.RedundantSpectrum;
import org.labkey.targetedms.parser.speclib.LibSpectrum.SpectrumKey;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter.PeptideIdRtInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// EncyclopeDIA file format documentation: https://bitbucket.org/searleb/encyclopedia/wiki/EncyclopeDIA%20File%20Formats
@SuppressWarnings("SqlResolve")
public class ElibSpectrumReader extends LibSpectrumReader
{
    @Override
    protected @Nullable Connection openMetadataConnection(Container container, String localLibPath)
    {
        // Serve the row-metadata queries from a covering-indexed cache next to the .elib when one is
        // available (built lazily in the background on first access). Peaks are still read from the .elib.
        return ElibCache.openIfReady(localLibPath);
    }

    @Override
    protected @Nullable ElibSpectrum readSpectrum(Connection metaConn, Connection libConn, SpectrumKey spectrumKey, Path libPath) throws DataFormatException, SQLException
    {
        return readElibSpectrum(metaConn, libConn, spectrumKey, true);
    }

    @Override
    protected @Nullable Path getRedundantLibPath(Container container, Path libPath)
    {
        return libPath; // EncyclopeDIA does not have a separate redundant spectra file
    }

    @Override
    public @Nullable ElibSpectrum readRedundantSpectrum(Connection metaConn, Connection libConn, SpectrumKey spectrumKey) throws SQLException, DataFormatException
    {
        return readElibSpectrum(metaConn, libConn, spectrumKey, false);
    }

    @Override
    protected String getMatchingModSeqLookupSql()
    {
        return "SELECT PeptideModSeq FROM entries WHERE PeptideSeq = ?";
    }

    // Reads the row metadata from metaConn (the .elib cache when available, otherwise the .elib itself) and
    // the peaks for the best-scoring spectrum from libConn (always the .elib).
    private ElibSpectrum readElibSpectrum(Connection metaConn, Connection libConn, SpectrumKey spectrumKey, boolean getRedundant) throws SQLException, DataFormatException
    {
        StringBuilder sql = new StringBuilder("SELECT PeptideModSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score FROM entries")
                         .append(" WHERE PeptideModSeq = ?").append(" AND PrecursorCharge = ?");
            if(spectrumKey.hasSourceFile())
            {
                sql.append(" AND SourceFile = ?");
            }

        List<ElibSpectrum> spectra = new ArrayList<>();
        try (PreparedStatement stmt = metaConn.prepareStatement(sql.toString()))
        {
            stmt.setString(1, spectrumKey.getModifiedPeptide());
            stmt.setInt(2, spectrumKey.getCharge());
            if(spectrumKey.hasSourceFile())
            {
                stmt.setString(3, spectrumKey.getSourceFile());
            }
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    ElibSpectrum spectrum = new ElibSpectrum();
                    spectrum.setPeptideModSeq(spectrumKey.getModifiedPeptide());
                    spectrum.setPrecursorCharge(rs.getInt("PrecursorCharge"));
                    spectrum.setPrecursorMz(rs.getDouble("PrecursorMz"));
                    double rt = rs.getDouble("RTInSeconds");
                    spectrum.setRetentionTime(rt / 60.0);
                    spectrum.setSourceFile(rs.getString("SourceFile"));
                    spectrum.setScore(rs.getDouble("Score"));
                    spectra.add(spectrum);
                }
            }
        }

        if(!spectra.isEmpty())
        {
            sortElibSpectra(spectra);
            ElibSpectrum bestSpectrum = spectra.get(0);
            readPeaks(libConn, bestSpectrum);

            if(getRedundant)
            {
                AtomicInteger id = new AtomicInteger(1);
                List<RedundantSpectrum> redundantSpectra = spectra.stream()
                        .map(s -> {
                            RedundantSpectrum rSpec = new RedundantSpectrum();
                            rSpec.setBestSpectrum(id.get() == 1);
                            rSpec.setRetentionTime(s.getRetentionTime());
                            rSpec.setSourceFile(s.getSourceFile());
                            rSpec.setRedundantRefSpectrumId(id.getAndIncrement());
                            return rSpec;
                        })
                        .collect(Collectors.toList());
                bestSpectrum.setRedundantSpectrumList(redundantSpectra);
            }

            return bestSpectrum;
        }
        return null;
    }

    // Peaks always come from the .elib (libConn); the cache holds metadata only.
    private void readPeaks(Connection libConn, ElibSpectrum spectrum) throws SQLException, DataFormatException
    {
        try (PreparedStatement stmt = libConn.prepareStatement("SELECT MassEncodedLength, MassArray, IntensityEncodedLength, IntensityArray FROM entries " +
                "WHERE PrecursorCharge = ? AND PeptideModSeq = ? AND SourceFile = ?"))
        {
            stmt.setInt(1, spectrum.getPrecursorCharge());
            stmt.setString(2, spectrum.getPeptideModSeq());
            stmt.setString(3, spectrum.getSourceFile());
            try(ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] mzArray = rs.getBytes("MassArray");
                    byte[] intensityArray = rs.getBytes("IntensityArray");

                    double[] peakMzs = extractMassArray(mzArray, rs.getInt("MassEncodedLength"));
                    float[] peakIntensities = extractIntensityArray(intensityArray, rs.getInt("IntensityEncodedLength"));

                    spectrum.setMzAndIntensity(peakMzs, peakIntensities);
                }
            }
        }
    }

    private static double[] extractMassArray(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
        // Based on the code provided on the EncyclopeDIA documentation page: https://bitbucket.org/searleb/encyclopedia/wiki/EncyclopeDIA%20File%20Formats
        byte[] uncompressedData = uncompress(compressedData, uncompressedLength);
        double[] mzArray = new double[uncompressedData.length / 8];
        ByteBuffer bb = ByteBuffer.wrap(uncompressedData);
        bb.order(ByteOrder.BIG_ENDIAN);
        DoubleBuffer buffer = bb.asDoubleBuffer();
        buffer.get(mzArray);
        return mzArray;
    }

    private static float[] extractIntensityArray(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
        // Based on the code provided on the EncyclopeDIA documentation page: https://bitbucket.org/searleb/encyclopedia/wiki/EncyclopeDIA%20File%20Formats
        byte[] uncompressedData = uncompress(compressedData, uncompressedLength);
        float[] intensities = new float[uncompressedData.length / 4];
        ByteBuffer bb = ByteBuffer.wrap(uncompressedData);
        bb.order(ByteOrder.BIG_ENDIAN);
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.get(intensities);
        return intensities;
    }

    private static byte[] uncompress(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
        byte[] uncompressed = new byte[uncompressedLength];

        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        inflater.inflate(uncompressed);
        inflater.end();
        return uncompressed;
    }

    private void sortElibSpectra(List<ElibSpectrum> spectra)
    {
        spectra.sort(Comparator.comparing(ElibSpectrum::getPeptideModSeq)
                .thenComparing(ElibSpectrum::getPrecursorCharge)
                .thenComparing(ElibSpectrum::getScore) // Assuming this is Qvalue; "Score" is not a nullable column;
                                                       // ascending sort will give us the best scoring spectrum
                                                       // for a modified sequence + charge at the top
                .thenComparing(ElibSpectrum::getSourceFile, Comparator.nullsLast(Comparator.naturalOrder())));
                                                       // Final tie-breaker on SourceFile so the chosen "best"
                                                       // spectrum and the ordering of the redundant list are fully
                                                       // determined by the data, not by the order rows happen to
                                                       // come back from the query. Without this, two rows with an
                                                       // identical Score would keep their query order (List.sort is
                                                       // stable), and the .elib and the metadata cache use different
                                                       // indexes, so they can return such ties in a different order.
    }

    @Override
    protected @NotNull List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> readRetentionTimes(Connection metaConn, String modifiedPeptide, String libPath) throws SQLException
    {
        List<ElibSpectrum> spectra = new ArrayList<>();

        try(PreparedStatement stmt = metaConn.prepareStatement("SELECT PeptideModSeq, PrecursorCharge, RTInSeconds, SourceFile, Score FROM entries WHERE PeptideModSeq = ?"))
        {
            stmt.setString(1, modifiedPeptide);
            try(ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    ElibSpectrum spectrum = new ElibSpectrum();
                    spectrum.setPeptideModSeq(rs.getString("PeptideModSeq"));
                    spectrum.setPrecursorCharge(rs.getInt("PrecursorCharge"));
                    spectrum.setSourceFile(rs.getString("SourceFile"));
                    double rt = rs.getDouble("RTInSeconds");
                    spectrum.setRetentionTime(rt / 60.0);
                    spectrum.setScore(rs.getDouble("Score"));
                    spectra.add(spectrum);
                }
            }
        }

        // Sort the spectra by charge and then by spectrum score (best to worst)
        sortElibSpectra(spectra);
        List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> retentionTimes = new ArrayList<>();
        int lastCharge = Integer.MAX_VALUE;
        for(var spectrum: spectra)
        {
            LibrarySpectrumMatchGetter.PeptideIdRtInfo rtInfo = new LibrarySpectrumMatchGetter.PeptideIdRtInfo(spectrum.getSourceFileName(), spectrum.getPeptideModSeq(),
                    spectrum.getPrecursorCharge(), spectrum.getRetentionTime(),
                    spectrum.getPrecursorCharge() != lastCharge // First spectrum for a charge will be the best spectrum for the modified sequence + charge combo
            );
            retentionTimes.add(rtInfo);
            lastCharge = spectrum.getPrecursorCharge();
        }

        return Collections.unmodifiableList(retentionTimes);
    }

    /**
     * Verifies the two correctness properties the cache depends on:
     * <ul>
     *   <li>{@link #sortElibSpectra} is fully deterministic - the chosen best spectrum and the order of
     *       the redundant (dropdown) list are decided by the data, not by the order rows happen to come
     *       back from the query;</li>
     *   <li>reading row metadata from the cache produces output identical to reading it from the
     *       {@code .elib} (best spectrum, redundant list, peaks, and retention times).</li>
     * </ul>
     */
    public static class TestCase extends Assert
    {
        @Test
        public void testTieBreakerIsDeterministic()
        {
            ElibSpectrumReader reader = new ElibSpectrumReader();

            // Same modified sequence + charge + score; only SourceFile differs. Without the SourceFile
            // tie-breaker the result would depend on the input order (List.sort is stable), which the
            // cache and the .elib can differ on because they use different indexes.
            List<ElibSpectrum> one = new ArrayList<>(List.of(
                    spectrum("PEPK", 2, "c.mzML", 0.05),
                    spectrum("PEPK", 2, "a.mzML", 0.05),
                    spectrum("PEPK", 2, "b.mzML", 0.05)));
            reader.sortElibSpectra(one);
            assertEquals(List.of("a.mzML", "b.mzML", "c.mzML"), sourceFiles(one));

            // A different starting permutation must yield the same order.
            List<ElibSpectrum> two = new ArrayList<>(List.of(
                    spectrum("PEPK", 2, "b.mzML", 0.05),
                    spectrum("PEPK", 2, "c.mzML", 0.05),
                    spectrum("PEPK", 2, "a.mzML", 0.05)));
            reader.sortElibSpectra(two);
            assertEquals(sourceFiles(one), sourceFiles(two));
        }

        @Test
        public void testScoreOrdersBeforeSourceFile()
        {
            ElibSpectrumReader reader = new ElibSpectrumReader();
            // The lowest score wins even though its SourceFile sorts last - Score is the primary key.
            List<ElibSpectrum> list = new ArrayList<>(List.of(
                    spectrum("PEPK", 2, "a.mzML", 0.10),
                    spectrum("PEPK", 2, "z.mzML", 0.01)));
            reader.sortElibSpectra(list);
            assertEquals("z.mzML", list.get(0).getSourceFile());
        }

        @Test
        public void testCacheAndElibReturnIdenticalSpectrum() throws Exception
        {
            Path dir = Files.createTempDirectory("elibreader");
            try
            {
                File elib = dir.resolve("test.elib").toFile();
                File cache = new File(elib.getAbsolutePath() + ".cache");
                createSourceElibWithPeaks(elib);
                ElibCacheWriter.build(elib, cache);

                ElibSpectrumReader reader = new ElibSpectrumReader();
                SpectrumKey key = new SpectrumKey("PEPTIDEK", 2);

                try (Connection elibConn = LibSpectrumReader.getLibConnection(elib.getAbsolutePath());
                     Connection cacheConn = LibSpectrumReader.getLibConnection(cache.getAbsolutePath()))
                {
                    ElibSpectrum fromElib = reader.readElibSpectrum(elibConn, elibConn, key, true);
                    ElibSpectrum fromCache = reader.readElibSpectrum(cacheConn, elibConn, key, true);

                    assertNotNull(fromElib);
                    assertNotNull(fromCache);

                    // Best spectrum is the lowest-scoring row (b.mzML, 0.01) from both sources.
                    assertEquals("b.mzML", fromElib.getSourceFile());
                    assertEquals(fromElib.getSourceFile(), fromCache.getSourceFile());
                    assertEquals(fromElib.getPrecursorCharge(), fromCache.getPrecursorCharge());
                    assertEquals(fromElib.getPrecursorMz(), fromCache.getPrecursorMz(), 0.0);
                    assertEquals(fromElib.getScore(), fromCache.getScore(), 0.0);

                    // The redundant (dropdown) list matches in order and best-flag, and the peaks - read
                    // from the .elib in both cases - are identical.
                    assertEquals(redundantKeys(fromElib), redundantKeys(fromCache));
                    assertEquals(peakKeys(fromElib), peakKeys(fromCache));
                }
            }
            finally
            {
                deleteQuietly(dir);
            }
        }

        @Test
        public void testCacheAndElibReturnIdenticalRetentionTimes() throws Exception
        {
            Path dir = Files.createTempDirectory("elibreader");
            try
            {
                File elib = dir.resolve("test.elib").toFile();
                File cache = new File(elib.getAbsolutePath() + ".cache");
                createSourceElibWithPeaks(elib);
                ElibCacheWriter.build(elib, cache);

                ElibSpectrumReader reader = new ElibSpectrumReader();
                try (Connection elibConn = LibSpectrumReader.getLibConnection(elib.getAbsolutePath());
                     Connection cacheConn = LibSpectrumReader.getLibConnection(cache.getAbsolutePath()))
                {
                    List<PeptideIdRtInfo> fromElib = reader.readRetentionTimes(elibConn, "PEPTIDEK", elib.getAbsolutePath());
                    List<PeptideIdRtInfo> fromCache = reader.readRetentionTimes(cacheConn, "PEPTIDEK", elib.getAbsolutePath());
                    assertFalse("expected some retention times", fromElib.isEmpty());
                    assertEquals(rtKeys(fromElib), rtKeys(fromCache));
                }
            }
            finally
            {
                deleteQuietly(dir);
            }
        }

        private static ElibSpectrum spectrum(String modSeq, int charge, String sourceFile, double score)
        {
            ElibSpectrum s = new ElibSpectrum();
            s.setPeptideModSeq(modSeq);
            s.setPrecursorCharge(charge);
            s.setSourceFile(sourceFile);
            s.setScore(score);
            return s;
        }

        private static List<String> sourceFiles(List<ElibSpectrum> spectra)
        {
            return spectra.stream().map(ElibSpectrum::getSourceFile).collect(Collectors.toList());
        }

        private static List<String> redundantKeys(ElibSpectrum spectrum)
        {
            return spectrum.getRedundantSpectrumList().stream()
                    .map(r -> r.getSourceFile() + "|" + r.getRedundantRefSpectrumId() + "|" + r.isBestSpectrum() + "|" + r.getRetentionTime())
                    .collect(Collectors.toList());
        }

        private static List<String> peakKeys(ElibSpectrum spectrum)
        {
            return spectrum.getPeaks().stream()
                    .map(p -> p.getMz() + "|" + p.getIntensity())
                    .collect(Collectors.toList());
        }

        private static List<String> rtKeys(List<PeptideIdRtInfo> rts)
        {
            return rts.stream()
                    .map(r -> r.getSampleFileName() + "|" + r.getCharge() + "|" + r.getRt() + "|" + r.isBestSpectrum())
                    .collect(Collectors.toList());
        }

        // Builds a small .elib with peak blobs. The three rows share PeptideModSeq "PEPTIDEK" / charge 2;
        // b.mzML has the best (lowest) score, and a.mzML and c.mzML tie on score so the SourceFile
        // tie-breaker decides their order.
        private static void createSourceElibWithPeaks(File elib) throws Exception
        {
            try (Connection conn = openWritable(elib.getAbsolutePath());
                 Statement st = conn.createStatement())
            {
                st.execute("CREATE TABLE entries (PeptideSeq TEXT, PeptideModSeq TEXT, PrecursorCharge INTEGER, "
                        + "PrecursorMz REAL, SourceFile TEXT, RTInSeconds REAL, Score REAL, "
                        + "MassEncodedLength INTEGER, MassArray BLOB, IntensityEncodedLength INTEGER, IntensityArray BLOB)");

                insertRow(conn, "a.mzML", 1810.0, 0.02, new double[]{100.0, 200.0}, new float[]{10f, 20f});
                insertRow(conn, "b.mzML", 1800.0, 0.01, new double[]{150.0, 250.0}, new float[]{5f, 15f});
                insertRow(conn, "c.mzML", 1790.0, 0.02, new double[]{120.0, 220.0}, new float[]{7f, 17f});
            }
        }

        private static void insertRow(Connection conn, String sourceFile, double rtSeconds, double score, double[] mz, float[] intensity) throws SQLException
        {
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO entries "
                    + "(PeptideSeq, PeptideModSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score, "
                    + "MassEncodedLength, MassArray, IntensityEncodedLength, IntensityArray) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
            {
                ins.setString(1, "PEPTIDEK");
                ins.setString(2, "PEPTIDEK");
                ins.setInt(3, 2);
                ins.setDouble(4, 500.25);
                ins.setString(5, sourceFile);
                ins.setDouble(6, rtSeconds);
                ins.setDouble(7, score);
                ins.setInt(8, mz.length * Double.BYTES);
                ins.setBytes(9, deflate(toBigEndianBytes(mz)));
                ins.setInt(10, intensity.length * Float.BYTES);
                ins.setBytes(11, deflate(toBigEndianBytes(intensity)));
                ins.executeUpdate();
            }
        }

        // Match the on-disk format the reader expects: big-endian arrays, zlib-compressed.
        private static byte[] toBigEndianBytes(double[] values)
        {
            ByteBuffer bb = ByteBuffer.allocate(values.length * Double.BYTES).order(ByteOrder.BIG_ENDIAN);
            for (double v : values)
                bb.putDouble(v);
            return bb.array();
        }

        private static byte[] toBigEndianBytes(float[] values)
        {
            ByteBuffer bb = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
            for (float v : values)
                bb.putFloat(v);
            return bb.array();
        }

        private static byte[] deflate(byte[] raw)
        {
            Deflater deflater = new Deflater();
            deflater.setInput(raw);
            deflater.finish();
            byte[] buf = new byte[Math.max(64, raw.length * 2)];
            int n = deflater.deflate(buf);
            deflater.end();
            return Arrays.copyOf(buf, n);
        }

        private static Connection openWritable(String path) throws Exception
        {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:/" + path);
        }

        private static void deleteQuietly(Path dir)
        {
            try (var paths = Files.walk(dir))
            {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
            catch (IOException ignored) {}
        }
    }
}
