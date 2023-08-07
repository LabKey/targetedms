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

/**
 * Structure which describes a candidate peak in a chromatogram for a transition.
 */
public class ChromPeak
{
    private float _retentionTime;
    private float _startTime;
    private float _endTime;
    private float _area;
    private float _backgroundArea;
    private float _height;
    private float _fwhm;
    private int _flagBits;
    private short _pointsAcross;
    private float _stdDev;
    private float _skewness;
    private float _kurtosis;
    private float _shapeCorrelation;

    private static final int FLAGVALUE_HAS_PEAK_SHAPE = 0x80;

    public ChromPeak(LittleEndianInput dataInputStream)
    {
        _retentionTime = Float.intBitsToFloat(dataInputStream.readInt());
        _startTime = Float.intBitsToFloat(dataInputStream.readInt());
        _endTime = Float.intBitsToFloat(dataInputStream.readInt());
        _area = Float.intBitsToFloat(dataInputStream.readInt());
        _backgroundArea = Float.intBitsToFloat(dataInputStream.readInt());
        _height = Float.intBitsToFloat(dataInputStream.readInt());
        _fwhm = Float.intBitsToFloat(dataInputStream.readInt());
        _flagBits = dataInputStream.readInt();
        _pointsAcross = dataInputStream.readShort();
        _stdDev = Float.intBitsToFloat(dataInputStream.readInt());
        _skewness = Float.intBitsToFloat(dataInputStream.readInt());
        _kurtosis = Float.intBitsToFloat(dataInputStream.readInt());
        _shapeCorrelation = Float.intBitsToFloat(dataInputStream.readInt());
    }

    public static int getStructSize(CacheFormatVersion cacheFormatVersion) {
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Twelve) < 0) {
            return 32;
        }
        if (cacheFormatVersion.compareTo(CacheFormatVersion.Sixteen) < 0) {
            return 36;
        }
        return 52;
    }

    public float getRetentionTime()
    {
        return _retentionTime;
    }

    public float getStartTime()
    {
        return _startTime;
    }

    public float getEndTime()
    {
        return _endTime;
    }

    public float getArea()
    {
        return _area;
    }

    public float getBackgroundArea()
    {
        return _backgroundArea;
    }

    public float getHeight()
    {
        return _height;
    }

    public float getFwhm()
    {
        return _fwhm;
    }

    public Float getStdDev() {
        return hasPeakShape() ? _stdDev : null;
    }
    public Float getSkewness() {
        return hasPeakShape() ? _skewness : null;
    }

    public Float getKurtosis() {
        return hasPeakShape() ? _kurtosis : null;
    }

    public Float getShapeCorrelation() {
        return hasPeakShape() ? _shapeCorrelation : null;
    }

    public boolean hasPeakShape() {
        return 0 != (_flagBits & FLAGVALUE_HAS_PEAK_SHAPE);
    }
}
