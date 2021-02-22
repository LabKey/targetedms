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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.targetedms.parser.speclib.LibSpectrum.RedundantSpectrum;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ElibSpectrumReader extends LibSpectrumReader
{
    @Override
    protected ElibSpectrum readSpectrum(Connection conn, String modifiedPeptide, int charge, Path libPath) throws DataFormatException, SQLException
    {
        return readElibSpectrum(conn, modifiedPeptide, charge, null, true);
    }

    @Nullable
    public ElibSpectrum getSpectrumForSourceFile(Container container, Path libPath, String modifiedPeptide, int charge, String sourceFile) throws SQLException, DataFormatException
    {
        String localLibPath = getLocalLibPath(container, libPath);

        if(localLibPath != null && new File(localLibPath).exists())
        {
            try (Connection conn = getLibConnection(localLibPath))
            {
                return readElibSpectrum(conn, modifiedPeptide, charge, sourceFile, false);
            }
        }
        return null;
    }

    private ElibSpectrum readElibSpectrum(Connection conn, String modifiedPeptide, int charge, String sourceFile, boolean getRedundant) throws SQLException, DataFormatException
    {
        StringBuilder sql = new StringBuilder("SELECT PeptideModSeq, PeptideSeq, PrecursorCharge, PrecursorMz, SourceFile, RTInSeconds, Score FROM entries");
            sql.append(" WHERE PeptideModSeq = '").append(modifiedPeptide).append("'");
            sql.append(" AND PrecursorCharge = ").append(charge);
            if(!StringUtils.isBlank(sourceFile))
            {
                sql.append(" AND SourceFile = '").append(sourceFile).append("'");
            }

        List<ElibSpectrum> spectra = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            while(rs.next())
            {
                ElibSpectrum spectrum = new ElibSpectrum();
                spectrum.setPeptideModSeq(modifiedPeptide);
                spectrum.setPrecursorCharge(rs.getInt("PrecursorCharge"));
                spectrum.setPrecursorMz(rs.getDouble("PrecursorMz"));
                double rt = rs.getDouble("RTInSeconds");
                spectrum.setRetentionTime(rt / 60.0);
                spectrum.setSourceFile(rs.getString("SourceFile"));
                spectrum.setScore(rs.getDouble("Score"));
                spectra.add(spectrum);
            }
        }

        if(spectra.size() > 0)
        {
            sortElibSpectra(spectra);
            ElibSpectrum bestSpectrum = spectra.get(0);
            readPeaks(conn, bestSpectrum);

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

    private void readPeaks(Connection conn, ElibSpectrum spectrum) throws SQLException, DataFormatException
    {
        StringBuilder sql = new StringBuilder("SELECT MassEncodedLength, MassArray, IntensityEncodedLength, IntensityArray FROM entries WHERE ");
        sql.append(" PrecursorCharge = ").append(spectrum.getPrecursorCharge());
        sql.append(" AND PeptideModSeq = '").append(spectrum.getPeptideModSeq()).append("'");
        sql.append(" AND SourceFile = '").append(spectrum.getSourceFile()).append("'");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString()))
        {
            if(rs.next())
            {
                byte[] mzArray = rs.getBytes("MassArray");
                byte[] intensityArray = rs.getBytes("IntensityArray");

                double[] peakMzs = extractMassArray(mzArray, rs.getInt("MassEncodedLength"));
                float[] peakIntensities = extractIntensityArray(intensityArray, rs.getInt("IntensityEncodedLength"));

                spectrum.setMzAndIntensity(peakMzs, peakIntensities);
            }
        }
    }

    private static double[] extractMassArray(byte[] compressedData, int uncompressedLength) throws DataFormatException
    {
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
                .thenComparing(ElibSpectrum::getScore)); // Assuming this is Qvalue; "Score" is not a nullable column;
                                                         // ascending sort will give us the best scoring spectrum
                                                         // for a modified sequence + charge at the top
    }

    @Override
    protected @NotNull List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> readRetentionTimes(Connection conn, String modifiedPeptide, String libPath) throws SQLException
    {
        StringBuilder sql = new StringBuilder("SELECT PeptideModSeq, PrecursorCharge, RTInSeconds, SourceFile, Score FROM entries");
        sql.append(" WHERE PeptideModSeq = '").append(modifiedPeptide).append("'");

        List<ElibSpectrum> spectra = new ArrayList<>();

        try(Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString()))
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

        sortElibSpectra(spectra);
        List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> retentionTimes = new ArrayList<>();
        int lastCharge = Integer.MAX_VALUE;
        for(var spectrum: spectra)
        {
            LibrarySpectrumMatchGetter.PeptideIdRtInfo rtInfo = new LibrarySpectrumMatchGetter.PeptideIdRtInfo(spectrum.getSourceFileName(), spectrum.getPeptideModSeq(),
                    spectrum.getPrecursorCharge(), spectrum.getRetentionTime(), spectrum.getPrecursorCharge() != lastCharge);
            retentionTimes.add(rtInfo);
            lastCharge = spectrum.getPrecursorCharge();
        }

        return Collections.unmodifiableList(retentionTimes);
    }
}
