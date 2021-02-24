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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingCache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * User: vsharma
 * Date: 5/6/12
 * Time: 11:36 AM
 */
public abstract class LibSpectrumReader
{
    static
    {
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find SQLite driver", e);
        }
    }

    private static final Logger LOG = LogManager.getLogger(LibSpectrumReader.class);

    @Nullable
    public LibSpectrum getLibSpectrum(Container container, Path libPath,
                                      String modifiedPeptide, int charge) throws SQLException, DataFormatException
    {
        String localLibPath = getLocalLibPath(container, libPath);
        if (null == localLibPath)
            return null;

        if(!(new File(localLibPath).exists()))
            return null;

        try (Connection conn = getLibConnection(localLibPath))
        {
            return readSpectrum(conn, modifiedPeptide, charge, libPath);
        }
    }

    @NotNull
    public List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> getRetentionTimes(Container container, Path libPath, String modifiedPeptide)
    {
        String libFilePath = getLocalLibPath(container, libPath);
        if (null == libFilePath)
            return Collections.emptyList();

        // We know it's local file and string may need encoding to convert to Path
        if(!(new File(libFilePath).exists()))
        {
            LOG.debug("File not found: " + libFilePath + ", referenced from container " + container.getPath());
            return Collections.emptyList();
        }

        try (Connection conn = getLibConnection(libFilePath))
        {
            return readRetentionTimes(conn, modifiedPeptide, libFilePath);
        }
        catch(SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    protected abstract LibSpectrum readSpectrum(Connection conn, String modifiedPeptide, int charge, Path libPath) throws DataFormatException, SQLException;

    @NotNull
    protected abstract List<LibrarySpectrumMatchGetter.PeptideIdRtInfo> readRetentionTimes(Connection conn, String modifiedPeptide, String libPath) throws SQLException;


    private static final int LIBCACHE_LIMIT = 1000;
    private static final long LIBCACHE_LIFETIME = CacheManager.DAY;

    private static String getLibCacheKey(@NotNull Container container, String pathStr)
    {
        return container.getId() + "::" + pathStr;
    }

    private static final CacheLoader<String, String> _libCacheLoader = (key, arg) ->
    {
        if (null != arg)
        {
            Pair<Container, Path> pair = (Pair<Container, Path>) arg;
            Container container = pair.first;
            Path remotePath = pair.second;
            if (null != container && null != remotePath)
            {
                File file = LocalDirectory.copyToContainerDirectory(container, remotePath, LOG);
                if (null != file)
                    return file.getAbsolutePath();
            }
        }
        return null;
    };

    private static final BlockingCache<String, String> _libCache =
            new BlockingCache<>(CacheManager.getStringKeyCache(LIBCACHE_LIMIT, LIBCACHE_LIFETIME, "" +
                    "SpeclibCache"), _libCacheLoader)
            {
                @Override
                public void remove(@NotNull String key)
                {
                    deleteTempFile(key);
                    super.remove(key);
                }

                @Override
                public void clear()
                {
                    getKeys().forEach(this::deleteTempFile);
                    super.clear();
                }

                private void deleteTempFile(String key)
                {
                    try
                    {
                        String filePathStr = get(key);
                        if (null != filePathStr)
                            Files.deleteIfExists(new File(filePathStr).toPath());
                    }
                    catch (IOException e)
                    {
                        LOG.error("Temp spectrum library file not removed", e);
                    }
                }
            };

    @Nullable
    static String getLocalLibPath(Container container, Path libPath)
    {
        // If lib is in cloud, copy it locally to read
        if (FileUtil.hasCloudScheme(libPath))
        {
            String localFilePathStr = _libCache.get(getLibCacheKey(container, FileUtil.getAbsolutePath(libPath)), new Pair<>(container, libPath));
            if (null != localFilePathStr)
            {
                return localFilePathStr;
            }
            else
            {
                LOG.debug("Unable to copy " + libPath + " to local file, referenced from " + container.getPath());
                return null;
            }
        }
        return FileUtil.getAbsolutePath(libPath);
    }

    public static void clearLibCache(Container container)
    {
        _libCache.clear();     // TODO: we could clear only keys from this container
    }

    static Connection getLibConnection(String libFilePath) throws SQLException
    {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        return DriverManager.getConnection("jdbc:sqlite:/" + libFilePath, config.toProperties());
    }
}
