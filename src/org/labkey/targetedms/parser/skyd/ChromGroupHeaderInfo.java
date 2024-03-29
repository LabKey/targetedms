/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.targetedms.parser.skyd;

import org.apache.poi.util.LittleEndianInput;
import org.jetbrains.annotations.Nullable;
import org.labkey.targetedms.parser.ChromatogramBinaryFormat;
import org.labkey.targetedms.parser.SignedMz;

import java.util.EnumSet;

/**
 * Structure which describes a group of chromatograms.
 */
public class ChromGroupHeaderInfo
{
    // using this in the constructor for floats to avoid using non-primitive Float objects to reduce memory footprint
    private static final float NULL_PLACEHOLDER = Float.NEGATIVE_INFINITY;

    // For reducing memory usage, following instance variables are commented out because of the
    // absence of accessors and reading from inputStream is left to correctly advance the size of the fields
    private int textIdIndex;
    private int startTransitionIndex;
//    private int startPeakIndex;
//    private int startScoreIndex;
    private int numPoints;
    private int compressedSize;
    private EnumSet<FlagValues> flagValues;
    private short fileIndex;
    private short textIdLen;
    private short numTransitions;
//    private byte numPeaks;
//    private byte maxPeakIndex;
//    private byte isProcessedScans;
//    private byte align1;
//    private short statusId;
//    private short statusRank;
    private double precursor;
    private long locationPoints;
    private int uncompressedSize;
    private float startTime;
    private float endTime;
//    private float collisionalCrossSection;

    public ChromGroupHeaderInfo(CacheFormatVersion cacheFormatVersion, LittleEndianInput dataInputStream)
    {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Seventeen) > 0)
        {
            precursor = dataInputStream.readDouble();
            locationPoints = dataInputStream.readLong();
            uncompressedSize = dataInputStream.readInt();
            startTransitionIndex = dataInputStream.readInt();
            textIdIndex = dataInputStream.readInt();
            /*startPeakIndex =*/ dataInputStream.readInt();
            /*startScoreIndex =*/ dataInputStream.readInt();
            numPoints = dataInputStream.readInt();
            compressedSize = dataInputStream.readInt();
            startTime = Float.intBitsToFloat(dataInputStream.readInt());
            endTime = Float.intBitsToFloat(dataInputStream.readInt());
            /*collisionalCrossSection =*/ Float.intBitsToFloat(dataInputStream.readInt());
            numTransitions = dataInputStream.readShort();
            flagValues = FlagValues.fromCurrentBits(dataInputStream.readShort());
            fileIndex = dataInputStream.readShort();
            /*ionMobilityUnits =*/ dataInputStream.readByte();
            /*numPeaks =*/ dataInputStream.readByte();
            /*maxPeakIndex =*/ dataInputStream.readByte();
            if (precursor < 0)
            {
                flagValues.add(FlagValues.polarity_negative);
                precursor = -precursor;
            }
            return;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Five) < 0)
        {
            precursor = Float.intBitsToFloat(dataInputStream.readInt());
            fileIndex = checkUShort(dataInputStream.readInt());
            numTransitions = checkUShort(dataInputStream.readInt());
            startTransitionIndex = dataInputStream.readInt();
            /*numPeaks =*/ checkByte(dataInputStream.readInt());
            /*startPeakIndex =*/ dataInputStream.readInt();
            /*int maxPeakIndexInt = */dataInputStream.readInt();
            /*maxPeakIndex = maxPeakIndexInt == -1 ? (byte) 0xff : checkByte(maxPeakIndexInt);*/
            numPoints = dataInputStream.readInt();
            compressedSize = dataInputStream.readInt();
            dataInputStream.readInt(); // ignore these four bytes
            locationPoints = dataInputStream.readLong();
            flagValues = EnumSet.noneOf(FlagValues.class);
        }
        else
        {
            // Versions 5 through 17
            textIdIndex = dataInputStream.readInt();
            startTransitionIndex = dataInputStream.readInt();
            /*startPeakIndex =*/
            dataInputStream.readInt();
            /*startScoreIndex =*/
            dataInputStream.readInt();
            numPoints = dataInputStream.readInt();
            compressedSize = dataInputStream.readInt();
            flagValues = FlagValues.fromLegacyBits(dataInputStream.readShort());
            fileIndex = dataInputStream.readShort();
            textIdLen = dataInputStream.readShort();
            numTransitions = dataInputStream.readShort();
            /*numPeaks =*/
            dataInputStream.readByte();
            /*maxPeakIndex =*/
            dataInputStream.readByte();
            /*isProcessedScans =*/
            dataInputStream.readByte();
            /*align1 =*/
            dataInputStream.readByte();
            /*statusId =*/
            dataInputStream.readShort();
            /*statusRank =*/
            dataInputStream.readShort();
            precursor = dataInputStream.readDouble();
            locationPoints = dataInputStream.readLong();
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Eleven) < 0)
        {
            uncompressedSize = -1;
            startTime = NULL_PLACEHOLDER;
            endTime = NULL_PLACEHOLDER;
        }
        else
        {
            uncompressedSize = dataInputStream.readInt();
            startTime = Float.intBitsToFloat(dataInputStream.readInt());
            endTime = Float.intBitsToFloat(dataInputStream.readInt());
            /*collisionalCrossSection =*/ Float.intBitsToFloat(dataInputStream.readInt());
        }
    }

    private static byte checkByte(int value) {
        if (value > 0xff || value < 0) {
            throw new IllegalArgumentException();
        }
        return (byte) value;
    }

    private static short checkUShort(int value) {
        if (value > 0xffff || value < 0) {
            throw new IllegalArgumentException();
        }
        return (short) value;
    }

    public int getFileIndex()
    {
        return Short.toUnsignedInt(fileIndex);
    }

    public EnumSet<FlagValues> getFlagValues() {
        return flagValues;
    }

    public static EnumSet<FlagValues> getFlagValues(long bits) {
        return EnumFlagValues.enumSetFromFlagValues(FlagValues.class, bits);
    }

    public short getFlagBits()
    {
        return (short) FlagValues.toLegacyBits(flagValues);
    }

    public enum FlagValues
    {
        has_mass_errors(0x01, 0x01),
        has_calculated_mzs (0, 0x02),
        extracted_base_peak (0x02, 0x04),
        has_ms1_scan_ids (0x04, 0x08),
        has_sim_scan_ids (0x08, 0x10),
        has_frag_scan_ids(0x10, 0x20),
        polarity_negative(0, 0x40),
        raw_chromatograms(0x20, 0x80),
        // Three bits for ion mobility info
        ion_mobility_type_1(0, 0x100),
        ion_mobility_type_2(0, 0x200),
        ion_mobility_type_3(0, 0x400),
        dda_acquisition_method(0x40, 0x800),
        extracted_qc_trace(0x80, 0x1000);

        private int currentFlagValue;
        private int legacyFlagValue;

        /**
         * @param currentFlagValue the value that the flag has in current (skyd format 18 or greater, Skyline 23_1) skyd files.
         * @param legacyFlagValue the value that the flag used to have in older skyd files.
         */
        FlagValues(int currentFlagValue, int legacyFlagValue)
        {
            this.currentFlagValue = currentFlagValue;
            this.legacyFlagValue = legacyFlagValue;
        }

        public static EnumSet<FlagValues> fromLegacyBits(int bits)
        {
            EnumSet<FlagValues> enumSet = EnumSet.noneOf(FlagValues.class);
            for (FlagValues flag : FlagValues.values())
            {
                if (0 != (flag.legacyFlagValue & bits))
                {
                    enumSet.add(flag);
                }
            }
            return enumSet;
        }

        public static EnumSet<FlagValues> fromCurrentBits(int bits)
        {
            EnumSet<FlagValues> enumSet = EnumSet.noneOf(FlagValues.class);
            for (FlagValues flag : FlagValues.values()) {
                if (0 != (flag.currentFlagValue & bits)) {
                    enumSet.add(flag);
                }
            }
            return enumSet;
        }

        public static int toLegacyBits(EnumSet<FlagValues> flags) {
            int result = 0;
            for (FlagValues flag : flags) {
                result |= flag.legacyFlagValue;
            }
            return result;
        }
    }

    public static int getStructSize(CacheFormatVersion cacheFormatVersion) {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Four) <= 0) {
            return 48;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Eleven) < 0) {
            return 56;
        }
        return 72;
    }

    public int getTextIdIndex()
    {
        return textIdIndex;
    }

    public int getStartTransitionIndex()
    {
        return startTransitionIndex;
    }

    // leaving commented out as these getters are not being accessed and for future usage
/*

    public int getStartPeakIndex()
    {
        return startPeakIndex;
    }

    public int getStartScoreIndex()
    {
        return startScoreIndex;
    }
*/

    public int getNumPoints()
    {
        return numPoints;
    }

    public int getCompressedSize()
    {
        return compressedSize;
    }

    public short getTextIdLen()
    {
        return textIdLen;
    }

    public short getNumTransitions()
    {
        return numTransitions;
    }
/*

    public byte getNumPeaks()
    {
        return numPeaks;
    }
*/

    public long getLocationPoints()
    {
        return locationPoints;
    }

    public int getUncompressedSize()
    {
        if (uncompressedSize != -1) {
            return uncompressedSize;
        }
        int sizeArray = (Integer.SIZE / 8)*numPoints;
        int sizeArrayErrors = (Short.SIZE / 8)*numPoints;
        int sizeTotal = sizeArray*(numTransitions + 1);
        EnumSet<FlagValues> flagValues = getFlagValues();
        if (flagValues.contains(FlagValues.has_mass_errors))
            sizeTotal += sizeArrayErrors*numTransitions;
        if (flagValues.contains(FlagValues.has_ms1_scan_ids))
            sizeTotal += (Integer.SIZE / 8)*numPoints;
        if (flagValues.contains(FlagValues.has_frag_scan_ids))
            sizeTotal += (Integer.SIZE / 8)*numPoints;
        if (flagValues.contains(FlagValues.has_sim_scan_ids))
            sizeTotal += (Integer.SIZE / 8)*numPoints;
        return sizeTotal;

    }
/*

    public byte getMaxPeakIndex()
    {
        return maxPeakIndex;
    }
*/

    @Nullable
    public Float getStartTime()
    {
        return startTime == NULL_PLACEHOLDER ? null : startTime;
    }

    @Nullable
    public Float getEndTime()
    {
        return endTime == NULL_PLACEHOLDER ? null : endTime;
    }

    public SignedMz getPrecursor() {
        if (isNegativePolarity()) {
            return new SignedMz(Math.abs(precursor), true);
        }
        return new SignedMz(Math.abs(precursor), false);
    }

    public double getPrecursorMz() {
        return Math.abs(precursor) * (isNegativePolarity() ? -1 : 1);
    }

    public SignedMz toSignedMz(double mz) {
        return new SignedMz(Math.abs(mz), isNegativePolarity());
    }

    public boolean isNegativePolarity() {
        return flagValues.contains(FlagValues.polarity_negative);
    }

    public boolean excludesTime(double time) {
        if (NULL_PLACEHOLDER == startTime || NULL_PLACEHOLDER == endTime) {
            return false;
        }
        return startTime > time || endTime < time;
    }

    public ChromatogramBinaryFormat getChromatogramBinaryFormat() {
        return getFlagValues().contains(FlagValues.raw_chromatograms)
                ? ChromatogramBinaryFormat.ChromatogramGroupData : ChromatogramBinaryFormat.Arrays;
    }
}
