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

package org.labkey.targetedms.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingCache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.Tuple3;
import org.labkey.api.util.UnexpectedException;
import org.labkey.targetedms.PanoramaBadDataException;
import org.labkey.targetedms.TargetedMSRun;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 4/16/12
 * Time: 3:39 PM
 */
public abstract class AbstractChromInfo extends ChromInfo<PrecursorChromInfoAnnotation>
{
    private Container _container;

    private int _numPoints;
    private Integer _uncompressedSize;
    /** Starting byte index in the .skyd file */
    private Long _chromatogramOffset;
    /** Number of compressed bytes stored in the .skyd file */
    private Integer _chromatogramLength;
    private int _chromatogramFormat;

    private static final Logger LOG = LogManager.getLogger(AbstractChromInfo.class);

    private static final BlockingCache<Tuple3<Path, Long, Integer>, CachedBytes> ON_DEMAND_CHROM_CACHE = new BlockingCache<>(CacheManager.getCache(100, CacheManager.HOUR, "SKYD chromatogram cache"), (key, argument) -> {
        Path path = key.first;
        long offset = key.second;
        int length = key.third;

        long startTime = System.currentTimeMillis();
        LOG.debug("Loading chromatogram from " + path + ", offset " + offset + ", length " + length);
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.SPARSE))
        {
            channel.position(offset);
            java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(length);
            channel.read(byteBuffer);
            byteBuffer.position(0);
            byte[] results = byteBuffer.array();
            LOG.debug("Finished loading from " + path + ", offset " + offset + ", length " + length + " in " + (System.currentTimeMillis() - startTime) + "ms");
            return new CachedBytes(results);
        }
        catch (NoSuchFileException e)
        {
            // Avoid a separate call to Files.exists() as it adds ~1 second overhead
            LOG.debug("Could not find SKYD file to get chromatogram at path " + path);
            return null;
        }
        catch (RuntimeException e)
        {
            if (e.getMessage() != null && e.getMessage().contains("The specified key does not exist"))
            {
                // Avoid a separate call to Files.exists() as it adds ~1 second overhead
                LOG.debug("Could not find SKYD file to get chromatogram at path " + path + ": " + e.getMessage());
                return null;
            }
            throw e;
        }
        catch (IOException e)
        {
            LOG.warn("Unable to fetch chromatogram from " + path, e);
            return null;
        }
    });

    public AbstractChromInfo()
    {
    }

    /**
     * Simple wrapper so we don't get warnings from CacheManager about caching a mutable array.
     * We trust callers not to mess with the bytes since they won't be, say, sorting or filtering them
     */
    public static class CachedBytes
    {
        public final byte[] _bytes;

        public CachedBytes(byte[] bytes)
        {
            _bytes = bytes;
        }
    }

    public AbstractChromInfo(Container c)
    {
        _container = c;
    }

    /** @return the DB-backed bytes, if available */
    @Nullable
    protected abstract byte[] getChromatogram();

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public int getNumPoints()
    {
        return _numPoints;
    }

    public void setNumPoints(int numPoints)
    {
        _numPoints = numPoints;
    }

    public abstract int getNumTransitions();

    public Integer getUncompressedSize()
    {
        return _uncompressedSize;
    }

    public void setUncompressedSize(Integer uncompressedSize)
    {
        _uncompressedSize = uncompressedSize;
    }


    public Integer getChromatogramFormat()
    {
        return _chromatogramFormat;
    }

    public void setChromatogramFormat(Integer chromatogramFormat)
    {
        _chromatogramFormat = chromatogramFormat == null ? ChromatogramBinaryFormat.Arrays.ordinal() : chromatogramFormat.intValue();
    }

    public Long getChromatogramOffset()
    {
        return _chromatogramOffset;
    }

    public void setChromatogramOffset(Long chromatogramOffset)
    {
        _chromatogramOffset = chromatogramOffset;
    }

    public Integer getChromatogramLength()
    {
        return _chromatogramLength;
    }

    public void setChromatogramLength(Integer chromatogramLength)
    {
        _chromatogramLength = chromatogramLength;
    }


    @Nullable
    public Chromatogram createChromatogram(TargetedMSRun run)
    {
        try
        {
            if (_chromatogramFormat < 0 || _chromatogramFormat >= ChromatogramBinaryFormat.values().length)
            {
                throw new IllegalArgumentException("Unknown format number " + _chromatogramFormat);
            }

            ChromatogramBinaryFormat binaryFormat = ChromatogramBinaryFormat.values()[getChromatogramFormat()];

            CompressedBytesAndStatus compressedBytesAndStatus = getCompressedBytesAndStatus(run);
            byte[] compressedBytes = compressedBytesAndStatus.getCompressedBytes();
            Chromatogram.SourceStatus status = compressedBytesAndStatus.getStatus();

            if (compressedBytes == null)
            {
                return null;
            }

            byte[] uncompressedBytes = SkylineBinaryParser.uncompressStoredBytes(compressedBytes, getUncompressedSize(), _numPoints, getNumTransitions());
            return binaryFormat.readChromatogram(uncompressedBytes, _numPoints, getNumTransitions(), status);
        }
        catch (DataFormatException e)
        {
            // Data was mangled. Don't report the exception but throw with specifics to let an admin track down the file if desired
            throw new PanoramaBadDataException("Unable to load chromatogram for run " + run.getId() + " in document " + run.getFileName() + " in " + run.getContainer().getPath() + " starting at offset " + getChromatogramOffset(), e);
        }
        catch (IOException e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    private CompressedBytesAndStatus getCompressedBytesAndStatus(TargetedMSRun run)
    {
        byte[] databaseBytes = getChromatogram();
        byte[] compressedBytes = databaseBytes;
        Chromatogram.SourceStatus status;

        if (run.getSkydDataId() != null && _chromatogramLength != null && _chromatogramOffset != null)
        {
            ExpData skydData = ExperimentService.get().getExpData(run.getSkydDataId());
            if (skydData != null)
            {
                Path skydPath = skydData.getFilePath();
                if (skydPath == null)
                {
                    status = Chromatogram.SourceStatus.skydMissing;
                    LOG.debug("No path available for " + this + ", bucket may be unavailable for URL " + skydData.getDataFileUrl());
                }
                else
                {
                    LOG.debug("Attempting to fetch chromatogram bytes (possibly cached) from " + skydPath + " for " + this);
                    CachedBytes diskBytes = ON_DEMAND_CHROM_CACHE.get(new Tuple3<>(skydPath, _chromatogramOffset, _chromatogramLength));
                    if (diskBytes == null)
                    {
                        status = Chromatogram.SourceStatus.skydMissing;
                    }
                    else if (databaseBytes != null && !Arrays.equals(databaseBytes, diskBytes._bytes))
                    {
                        LOG.error("Chromatogram bytes for " + this + " do not match between .skyd and DB. Using database copy. Lengths: " + diskBytes._bytes.length + " vs " + databaseBytes.length);
                        status = Chromatogram.SourceStatus.mismatch;
                    }
                    else
                    {
                        compressedBytes = diskBytes._bytes;
                        status = databaseBytes == null ? Chromatogram.SourceStatus.diskOnly : Chromatogram.SourceStatus.match;
                    }
                }
            }
            else
            {
                status = Chromatogram.SourceStatus.noSkydResolved;
            }
        }
        else
        {
            LOG.debug("No length, offset, and/or SKYD DataId for " + this);
            status = Chromatogram.SourceStatus.dbOnly;
        }

        return new CompressedBytesAndStatus(compressedBytes, status);
    }

    private static class CompressedBytesAndStatus
    {
        private final byte[] _compressedBytes;
        private final Chromatogram.SourceStatus _status;

        private CompressedBytesAndStatus(byte[] compressedBytes, Chromatogram.SourceStatus status)
        {
            _compressedBytes = compressedBytes;
            _status = status;
        }

        public byte[] getCompressedBytes()
        {
            return _compressedBytes;
        }

        public Chromatogram.SourceStatus getStatus()
        {
            return _status;
        }
    }

    @Nullable
    public byte[] getChromatogramBytes(TargetedMSRun run)
    {
        CompressedBytesAndStatus compressedBytesAndStatus = getCompressedBytesAndStatus(run);
        return compressedBytesAndStatus.getCompressedBytes();
    }
}
