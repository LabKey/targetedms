/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.parser;

import org.labkey.api.util.UnexpectedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
* User: jeckels
* Date: Apr 16, 2012
*/
public class Chromatogram extends SkylineEntity
{
    // Size is one four-byte float, eight four-byte ints, one four-byte spacer, and one eight-byte long.
    // Divide by 8 to convert from bits to bytes
    public static final int SIZE = (Float.SIZE + 8 * Integer.SIZE + Integer.SIZE + Long.SIZE) / 8;

    private int _runId;

    private float _precursor;
    private int _fileIndex;
    private int _numTransitions;
    private int _startTransitionIndex;
    private int _numPeaks;
    private int _startPeakIndex;
    private int _maxPeakIndex;
    private int _numPoints;
    private int _compressedSize;
    // One "spacer" int for alignment purposes before the next variable
    private long _locationPoints;

    private float[] _times;
    private float[][] _intensities;
    private SkylineBinaryParser.CachedFile[] _cachedFiles;
    private float[] _transitions;
    /** The compressed bytes */
    private byte[] _chromatogram;

    public Chromatogram()
    {

    }

    /** Parses the header information from the underlying file */
    public Chromatogram(ByteBuffer buffer, SkylineBinaryParser.CachedFile[] cachedFiles, float[] transitions)
    {
        _precursor = buffer.getFloat();
        _fileIndex = buffer.getInt();
        _numTransitions = buffer.getInt();
        _startTransitionIndex = buffer.getInt();
        _numPeaks = buffer.getInt();
        _startPeakIndex = buffer.getInt();
        _maxPeakIndex = buffer.getInt();
        _numPoints = buffer.getInt();
        _compressedSize = buffer.getInt();
        buffer.getInt(); // Burn four bytes for due to layout/alignment in data file
        _locationPoints = buffer.getLong();

        _cachedFiles = cachedFiles;
        _transitions = transitions;
    }

    public float getPrecursorMz()
    {
        return _precursor;
    }

    private void ensureUncompressed() throws DataFormatException
    {
        // Uncompress them
        byte[] pointsUncompressed = uncompress(_chromatogram);

        // Unpack them into float arrays for the times and intensities
        _times = new float[_numPoints];
        _intensities = new float[_numTransitions][];
        for (int i = 0; i < _numTransitions; i++)
            _intensities[i] = new float[_numPoints];

        for (int i = 0, offset = 0; i < _numPoints; i++, offset += Float.SIZE / 8)
            _times[i] = toFloat(pointsUncompressed, offset);
        int sizeArray = Float.SIZE / 8 * _numPoints;
        for (int i = 0, offsetTran = sizeArray; i < _numTransitions; i++, offsetTran += sizeArray)
        {
            for (int j = 0, offset = 0; j < _numPoints; j++, offset += Float.SIZE / 8)
                _intensities[i][j] = toFloat(pointsUncompressed, offset + offsetTran);
        }
    }

    /** Use zlib to inflate the compressed bytes to their original content */
    private byte[] uncompress(byte[] compressedBytes) throws DataFormatException
    {
        int uncompressedSize = (Integer.SIZE / 8) * _numPoints * (_numTransitions + 1);
        if(uncompressedSize == _compressedSize)
            return compressedBytes;

        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);
        byte[] result = new byte[uncompressedSize];
        int index = 0;
        int bytesRead;
        while ((bytesRead = inflater.inflate(result, index, uncompressedSize - index)) != 0)
        {
            index += bytesRead;
        }
        return result;
    }

    private float toFloat(byte[] bytes, int offset)
    {
        int asInt = (bytes[offset] & 0xFF)
                    | ((bytes[offset + 1] & 0xFF) << 8)
                    | ((bytes[offset + 2] & 0xFF) << 16)
                    | ((bytes[offset + 3] & 0xFF) << 24);
        return Float.intBitsToFloat(asInt);
    }

    public float[] getTimes()
    {
        try
        {
            ensureUncompressed();
        }
        catch (DataFormatException e)
        {
            throw new IllegalStateException("Should have been caught at initial import time", e);
        }
        return _times;
    }

    public float[] getIntensities(int index)
    {
        try
        {
            ensureUncompressed();
        }
        catch (DataFormatException e)
        {
            throw new IllegalStateException("Should have been caught at initial import time", e);
        }
        return _intensities[index];
    }

    public int getTransitionsCount()
    {
        try
        {
            ensureUncompressed();
        }
        catch (DataFormatException e)
        {
            throw new IllegalStateException("Should have been caught at initial import time", e);
        }
        return _intensities.length;
    }

    public int matchTransitions(List<Transition> transitions, double tolerance, boolean multiMatch)
    {
        int match = 0;
        for (Transition transition : transitions)
        {
            int start = _startTransitionIndex;
            int end = start + _numTransitions;
            for (int i = start; i < end; i++)
            {
                // Do we need to look through all of the transitions from the .skyd file?
                if (compareTolerant((float)transition.getMz(), _transitions[i], tolerance) == 0)
                {
                    match++;
                    if (!multiMatch)
                        break;  // only one match per transition
                }
            }
        }
        return match;
    }

    public static int compareTolerant(double f1, double f2, double tolerance)
    {
        if (Math.abs(f1 - f2) < tolerance)
            return 0;
        return (f1 > f2 ? 1 : -1);
    }

    public String getFilePath()
    {
        return _cachedFiles[_fileIndex].getFilePath();
    }

    public int getFileIndex()
    {
        return _fileIndex;
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public byte[] getChromatogram()
    {
        return _chromatogram;
    }

    /** Read the chromatogram out of the file, but don't hold on to it. This reduces memory usage during import */
    public byte[] readChromatogram(SkylineBinaryParser parser)
    {
        if (_chromatogram != null)
        {
            // If we have a cached one already for some reason, just use it
            return _chromatogram;
        }

        try
        {
            // Get the compressed bytes
            MappedByteBuffer buffer = parser.getChannel().map(FileChannel.MapMode.READ_ONLY, _locationPoints, _compressedSize);
            byte[] result = new byte[_compressedSize];
            buffer.get(result);

            // Make sure it uncompresses successfully so that we don't import bad content into the database
            uncompress(result);
            return result;
        }
        catch (IOException e)
        {
            throw new UnexpectedException(e);
        }
        catch (DataFormatException e)
        {
            throw new UnexpectedException(e);
        }
    }

    public void setChromatogram(byte[] chromatogram)
    {
        _chromatogram = chromatogram;
    }

    public int getNumPoints()
    {
        return _numPoints;
    }

    public void setNumPoints(int numPoints)
    {
        _numPoints = numPoints;
    }

    public int getNumTransitions()
    {
        return _numTransitions;
    }

    public void setNumTransitions(int numTransitions)
    {
        _numTransitions = numTransitions;
    }

    public float[] getTransitions()
    {
        float[] result = new float[_numTransitions];
        System.arraycopy(_transitions, _startTransitionIndex, result, 0, _numTransitions);
        return result;
    }
}
