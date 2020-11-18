package org.labkey.targetedms;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.TestContext;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.PsiInstruments;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.InstrumentManager;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.hasItem;

public class MsDataSourceUtil
{
    private static final MsDataSourceUtil instance = new MsDataSourceUtil();
    private static final Logger LOG = Logger.getLogger(MsDataSourceUtil.class);

    private MsDataSourceUtil() {}

    public static MsDataSourceUtil getInstance()
    {
        return instance;
    }

    public RawDataInfo getDownloadInfo(@NotNull SampleFile sampleFile, @NotNull Container container)
    {
        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return null;
        }

        // We will look for raw data files only in @files/RawFiles
        Path rawFilesDir = getRawFilesDir(container);
        if(rawFilesDir == null)
        {
            return null;
        }

        ExpData expData = getDataForSampleFile(sampleFile, container, rawFilesDir, expSvc, false);

        Long size;
        if(expData != null)
        {
            Path dataPath = expData.getFilePath();
            if(dataPath != null && Files.exists(dataPath))
            {
                try
                {
                    if(!Files.isDirectory(dataPath))
                    {
                        size = Files.size(dataPath);
                        return new RawDataInfo(expData, size, true);
                    }
                    else
                    {
                        size = FileUtil.hasCloudScheme(dataPath) ? null : FileUtils.sizeOfDirectory(dataPath.toFile());
                        return new RawDataInfo(expData, size, false);
                    }
                }
                catch (IOException e)
                {
                    LOG.debug("Error getting size of " + dataPath, e);
                }
            }
        }
        return null;
    }

    private Path getRawFilesDir(Container c)
    {
        FileContentService fcs = FileContentService.get();
        if(fcs != null)
        {
            Path fileRoot = fcs.getFileRootPath(c, FileContentService.ContentType.files);
            if (fileRoot != null)
            {
                return fileRoot.resolve(TargetedMSService.RAW_FILES_DIR);
            }
        }
        return null;
    }

    private ExpData getDataForSampleFile(SampleFile sampleFile, Container container, Path rawFilesDir, ExperimentService expSvc, boolean validateZip)
    {
        List<? extends ExpData> expDatas = getExpData(sampleFile.getFileName(), container, rawFilesDir, expSvc);

        if(expDatas.size() == 0)
        {
            return null;
        }

        MsDataSource sourceType = null;
        List<ExpData> notZipDatas = new ArrayList<>();
        for(ExpData data: expDatas)
        {
            // Prefer to return a zip source if we found one
            if(isZip(data.getName()))
            {
                if(validateZip && sourceType == null)
                {
                    // We will need the source type to validate the zip. Get the source type from the file extension
                    // and (if required) the instrument associated with the sample file
                    sourceType = getMsDataSource(sampleFile);
                }
                if(!validateZip || (data.getFilePath() != null && sourceType.isValidPath(data.getFilePath())))
                {
                    return data;
                }
            }
            else
            {
                notZipDatas.add(data);
            }
        }

        if(sourceType == null)
        {
            sourceType = getMsDataSource(sampleFile);
        }
        for(ExpData data: notZipDatas)
        {
            if(sourceType.isValidData(data, expSvc))
            {
                return data;
            }
        }

        return null;
    }

    @NotNull
    private List<? extends ExpData> getExpData(String fileName, Container container, Path pathPrefix, ExperimentService expSvc)
    {
        String pathPrefixString = FileUtil.pathToString(pathPrefix); // Encoded URI string

        TableInfo expDataTInfo = expSvc.getTinfoData();
        SimpleFilter filter = getExpDataFilter(container, pathPrefixString);
        filter.addCondition(FieldKey.fromParts("name"), fileName, CompareType.STARTS_WITH);

        // Get the rowId and name of matching rows.
        Map<Integer, String> expDatas = new TableSelector(expDataTInfo,
                expDataTInfo.getColumns("RowId", "Name"), filter, null).getValueMap();

        List<Integer> expDataIds = new ArrayList<>();
        // Look for the file and file.zip (e.g. sample_1.raw and sample_1.raw.zip)
        expDatas.entrySet().stream()
                           .filter(e -> fileName.equals(e.getValue()) || (isZip(e.getValue()) && fileName.equals(FileUtil.getBaseName(e.getValue()))))
                           .forEach(e -> expDataIds.add(e.getKey()));

        return expSvc.getExpDatas(expDataIds);
    }

    private static SimpleFilter getExpDataFilter(Container container, String pathPrefix)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        if (!pathPrefix.endsWith("/"))
        {
            pathPrefix = pathPrefix + "/";
        }
        // StartsWithClause is not public.  And CompareClause with CompareType.STARTS_WITH is not the same thing as StartWithClause
        // Use addCondition() instead.
        filter.addCondition(FieldKey.fromParts("datafileurl"), pathPrefix, CompareType.STARTS_WITH);
        return filter;
    }

    private static boolean isZip(@NotNull String fileName)
    {
        return fileName.toLowerCase().endsWith(EXT_ZIP);
    }

    @NotNull
    private MsDataSource getMsDataSource(ISampleFile sampleFile)
    {
        List<MsDataSource> sourceTypes = getSourceForName(sampleFile.getFileName());
        if(sourceTypes.size() == 1)
        {
            return sourceTypes.get(0);
        }
        else if(sourceTypes.size() > 1)
        {
            // We can get more than one source type by filename extension lookup. e.g. .raw extension is used both by Thermo and Waters
            // Try to resolve by looking up the instrument on which the data was acquired.
            Instrument instrument = InstrumentManager.getInstrument(sampleFile.getInstrumentId());
            if(instrument != null)
            {
                MsDataSource source = getSourceForInstrument(instrument);
                if(source != null)
                {
                    return source;
                }
            }
            // We cannot resolve to a single source even after instrument lookup
            if(sourceTypes.size() > 1)
            {
                MsMultiDataSource multiSource = new MsMultiDataSource();
                for(MsDataSource source: sourceTypes)
                {
                    multiSource.addSource(source);
                }
                return multiSource;
            }
        }
        return new UnknownDataSource();
    }

    /**
     *
     * @param sampleFiles list of sample files for which we should check if data exists
     * @param container container where we should look for data
     * @return list of sample files for which data was found
     */
    @NotNull
    public List<? extends ISampleFile> hasData(@NotNull List<? extends ISampleFile> sampleFiles, @NotNull Container container)
    {
        List<ISampleFile> dataFound = new ArrayList<>();

        Path rawFilesDir = getRawFilesDir(container);
        if (rawFilesDir == null || !Files.exists(rawFilesDir))
        {
            return dataFound;
        }

        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return dataFound;
        }

        for(ISampleFile sampleFile: sampleFiles)
        {
            MsDataSource dataSource = getMsDataSource(sampleFile);

            String fileName = sampleFile.getFileName();
            if(hasData(fileName, dataSource, container, rawFilesDir, expSvc))
            {
                dataFound.add(sampleFile);
            }
        }
        return dataFound;
    }

    private boolean hasData(String fileName, MsDataSource dataSource, Container container, Path rawFilesDir, ExperimentService expSvc)
    {
        List<? extends ExpData> expDatas = getExpData(fileName, container, rawFilesDir, expSvc);

        if(expDatas.size() > 0)
        {
            for (ExpData data : expDatas)
            {
                if (dataSource.isValidNameAndData(data, expSvc))
                {
                   return true;
                }
            }
        }
        else
        {
            FileContentService fcs = FileContentService.get();
            if (fcs != null && !fcs.isCloudRoot(container))
            {
                // No matches found in exp.data.  Look on the filesystem
                return dataExists(fileName, dataSource, rawFilesDir);
            }
        }
        return false;
    }

    private boolean dataExists(String fileName, MsDataSource sourceType, Path rawFilesDir)
    {
        try
        {
            return Files.walk(rawFilesDir).anyMatch(p -> isSourceMatch(p, fileName, sourceType));
        }
        catch (IOException e)
        {
            LOG.debug("Error checking for data in sub-directories of " + rawFilesDir, e);
        }

        return false;
    }

    private boolean isSourceMatch(Path path, String fileName, MsDataSource sourceType)
    {
        String pathFileName = FileUtil.getFileName(path);
        if(fileName.equals(pathFileName) || (isZip(pathFileName) && fileName.equals(FileUtil.getBaseName(pathFileName))))
        {
           return sourceType.isValidPath(path);
        }
        return false;
    }

    private List<MsDataSource> getSourceForName(String name)
    {
        // Can return more than one data source type. For example, Bruker and Waters both have .d extension;
        // Thermo and Waters both have .raw extension
        var sources = EXTENSION_SOURCE_MAP.get(extension(name));
        return sources != null ? sources : Collections.emptyList();
    }

    private static String extension(String name)
    {
        if(name != null)
        {
            int idx = name.lastIndexOf('.');
            return idx != -1 ? name.substring(idx).toLowerCase() : EXT_EMPTY;
        }
        return EXT_EMPTY;
    }

    private MsDataSource getSourceForInstrument(Instrument instrument)
    {
        // Try to find an instrument from the PSI-MS instrument list that matches the instrument model.
        // We may not find a match because
        // 1. This is a new instrument model and our instrument list is not current
        // 2. The instrument model may be something general like "Waters instrument model".
        //    This is generally seen in Skyline documents for data from instruments other than Thermo and SCIEX.
        PsiInstruments.PsiInstrument psiInstrument = PsiInstruments.getInstrument(instrument.getModel());
        String vendorOrModel = psiInstrument != null ? psiInstrument.getVendor() : instrument.getModel();
        return Arrays.stream(sourceTypes).filter(s -> s.isInstrumentSource(vendorOrModel)).findFirst().orElse(null);
    }

    private static final MsDataSource CONVERTED_DATA_SOURCE = new MsDataFileSource(Arrays.asList(".mzxml", ".mzml", ".mz5", ".mzdata"));
    private static final MsDataSource THERMO = new MsDataFileSource("thermo", ".raw");
    private static final MsDataSource SCIEX = new MsDataFileSource("sciex", Arrays.asList(".wiff", ".wiff2", ".wiff.scan", ".wiff2.scan"))
    {
        @Override
        public boolean isInstrumentSource(String instrument)
        {
            return super.isInstrumentSource(instrument) || (instrument != null && instrument.toLowerCase().contains("applied biosystems"));
        }
    };
    private static final MsDataSource SHIMADZU = new MsDataFileSource("shimadzu", ".lcd");
    private static final MsDataDirSource WATERS = new MsDataDirSource("waters", ".raw")
    {
        @Override
        public boolean isExpectedDirContent(Path p)
        {
            return !Files.isDirectory(p) && FileUtil.getFileName(p).matches("^_FUNC.*\\.DAT$");
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new CompareType.ContainsClause(FieldKey.fromParts("Name"), "")
            {
                @Override
                public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
                {
                    ColumnInfo colInfo = columnMap != null ? columnMap.get(getFieldKey()) : null;
                    String alias = colInfo != null ? colInfo.getAlias() : getFieldKey().getName();
                    return new SQLFragment(dialect.getColumnSelectName(alias))
                            .append(" ").append(dialect.getCaseInsensitiveLikeOperator()).append(" ? ")
                            .append(sqlEscape())
                            .add(escapeLikePattern("_") + "FUNC%.DAT");
                }
            };
        }
    };

    private static final String AGILENT_ACQ_DATA = "AcqData";
    private static final MsDataDirSource AGILENT = new MsDataDirSource("agilent", ".d")
    {
        @Override
        public boolean isExpectedDirContent(Path p)
        {
            return Files.isDirectory(p) && FileUtil.getFileName(p).equals(AGILENT_ACQ_DATA);
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, AGILENT_ACQ_DATA);
        }
    };

    private static final String BRUKER_ANALYSIS_BAF = "analysis.baf";
    private static final String BRUKER_ANALYSIS_TDF = "analysis.tdf";
    private static final MsDataDirSource BRUKER = new MsDataDirSource("bruker", ".d")
    {
        @Override
        public boolean isExpectedDirContent(Path p)
        {
            return !Files.isDirectory(p) && (FileUtil.getFileName(p).equals(BRUKER_ANALYSIS_BAF) || FileUtil.getFileName(p).equals(BRUKER_ANALYSIS_TDF));
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new SimpleFilter.OrClause(
                    new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, BRUKER_ANALYSIS_BAF),
                    new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, BRUKER_ANALYSIS_TDF)
            );
        }
    };

    private static MsDataSource[] sourceTypes = new MsDataSource[]{THERMO, SCIEX, SHIMADZU, WATERS, AGILENT, BRUKER, CONVERTED_DATA_SOURCE};
    private static final String EXT_ZIP = ".zip";
    private static final String EXT_EMPTY = "";


    private static Map<String, List<MsDataSource>> EXTENSION_SOURCE_MAP = new HashMap<>();
    static
    {
        for (MsDataSource s : sourceTypes)
        {
            for(String ext: s._extensions)
            {
                List<MsDataSource> sources = EXTENSION_SOURCE_MAP.computeIfAbsent(ext, k -> new ArrayList<>());
                sources.add(s);
            }
        }
    }


    private abstract static class MsDataSource
    {
        private final List<String> _extensions;
        private final String _instrumentVendor;

        private MsDataSource(@NotNull String instrumentVendor, @NotNull List<String> extensions)
        {
            _instrumentVendor = instrumentVendor;
            _extensions = extensions;
        }

        public boolean isValidName(String name)
        {
            if(name != null)
            {
                String nameLc = name.toLowerCase();
                for(String ext: _extensions)
                {
                    if(nameLc.endsWith(ext))
                    {
                        return true;
                    }
                }

                return isValidZipName(name);
            }
            return false;
        }

        boolean isValidZipName(String fileName)
        {
            return MsDataSourceUtil.isZip(fileName) && isValidName(FileUtil.getBaseName(fileName));
        }

        /* instrument can be the instrument model (saved in targetedms.instrument) or vendor name from the PSI-MS instrument list */
        public boolean isInstrumentSource(String instrument)
        {
            return instrument != null && instrument.toLowerCase().contains(_instrumentVendor);
        }

        public String toString()
        {
            return name();
        }

        public String name()
        {
            return _instrumentVendor;
        }

        public boolean isValidNameAndPath(@NotNull Path path)
        {
            return isValidName(FileUtil.getFileName(path)) && isValidPath(path);
        }

        public boolean isValidNameAndData(@NotNull ExpData expData, @NotNull ExperimentService expSvc)
        {
            return isValidName(expData.getName()) && isValidData(expData, expSvc);
        }

        abstract boolean isValidPath(@NotNull Path path);
        abstract boolean isValidData(@NotNull ExpData data, ExperimentService expSvc);
        abstract boolean isFileSource();
    }

    private static class MsDataFileSource extends MsDataSource
    {
        private MsDataFileSource(String instrument, List<String> extensions)
        {
            super(instrument, extensions);
        }

        private MsDataFileSource(String instrument, String extension)
        {
            super(instrument, Collections.singletonList(extension));
        }

        public MsDataFileSource(List<String> extensions)
        {
            super("Unknown", extensions);
        }

        @Override
        public boolean isFileSource()
        {
            return true;
        }

        @Override
        boolean isValidPath(@NotNull Path path)
        {
            if(Files.exists(path) && !Files.isDirectory(path))
            {
                return !MsDataSourceUtil.isZip(FileUtil.getFileName(path)) || validZip(path);
            }
            return false;
        }

        private boolean validZip(@NotNull Path path)
        {
            try
            {
                for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                        .getRootDirectories())
                {
                    String basename = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                    Path file = root.resolve(basename);
                    // Make sure that a file with the name exists in the zip archive
                    if(Files.exists(file) && !Files.isDirectory(file))
                    {
                        return true;
                    }
                }
            }
            catch (IOException e)
            {
                LOG.debug("Error validating zip source for " + name() + ". Path: " + path, e);
            }
            return false;
        }

        @Override
        boolean isValidData(@NotNull ExpData expData, ExperimentService expSvc)
        {
            String pathPrefix = expData.getDataFileUrl();
            TableInfo expDataTInfo = expSvc.getTinfoData();
            // We are not going to do a !Files.isDirectory() check.
            // Instead, we will look for any rows in exp.data where the dataFileUrl starts with the pathPrefix
            // Example: if dataFileUrl for given ExpData is file://folder/exp_data_name.raw
            //          we shouldn't find any rows where the dataFileUrl is like file://folder/exp_data_name.raw/file.raw
            return !(new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"),
                    MsDataSourceUtil.getExpDataFilter(expData.getContainer(), pathPrefix), null).exists());
        }
    }

    private static abstract class MsDataDirSource extends MsDataSource
    {
        // Condition for validating directory-based raw data
        abstract boolean isExpectedDirContent(Path p);

        // Filter for validating a directory source by finding rows for expected directory contents in the exp.data table.
        // Used only if the source directory was uploaded without auto-zipping (e.g. when the directory was uploaded to
        // a network drive mapped to a LabKey folder)
        abstract SimpleFilter.FilterClause getDirContentsFilterClause();

        private MsDataDirSource(String instrumentVendor, String extension)
        {
            super(instrumentVendor, Collections.singletonList(extension));
        }

        @Override
        public boolean isFileSource()
        {
            return false;
        }

        @Override
        public boolean isValidPath(@NotNull Path path)
        {
            if(Files.exists(path))
            {
                if(MsDataSourceUtil.isZip(FileUtil.getFileName(path)))
                {
                    return !Files.isDirectory(path) && validZip(path);
                }
                else
                {
                    return Files.isDirectory(path) && hasExpectedDirContents(path);
                }
            }
            return false;
        }

        private boolean hasExpectedDirContents(@NotNull Path path)
        {
            try
            {
                return Files.list(path).anyMatch(this::isExpectedDirContent);
            }
            catch (IOException e)
            {
                LOG.debug("Error validating directory source for " + name() + ". Path: " + path, e);
            }
            return false;
        }

        private boolean validZip(@NotNull Path path)
        {
            try
            {
                for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                        .getRootDirectories())
                {
                    boolean dataInRoot = hasExpectedDirContents(root);
                    if (dataInRoot)
                    {
                        return true;
                    }
                    else
                    {
                        String subdirName = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                        // Look for match in the subdirectory. The zip may look like this (Waters example):
                        // datasouce.raw.zip
                        // -- datasource.raw
                        //    -- _FUNC001.DAT
                        Path subDir = root.resolve(subdirName);
                        if(Files.exists(subDir) && hasExpectedDirContents(subDir))
                        {
                            return true;
                        }
                    }
                }
            }
            catch (IOException e)
            {
                LOG.debug("Error validating zip source for " + name() + ". Path: " + path, e);
            }
            return false;
        }

        @Override
        boolean isValidData(@NotNull ExpData expData, ExperimentService expSvc)
        {
            String fileName = expData.getName();
            if(MsDataSourceUtil.isZip(fileName))
            {
                String pathPrefix = expData.getDataFileUrl();
                TableInfo expDataTInfo = expSvc.getTinfoData();
                // We are not going to do a !Files.isDirectory() check.
                // Instead, we will look for any rows in exp.data where the dataFileUrl starts with the pathPrefix
                // Example: if dataFileUrl for given ExpData is file://folder/exp_data_name.raw.zip
                //          we shouldn't find any rows where the dataFileUrl is like file://folder/exp_data_name.raw.zip/file.raw
                return !(new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"),
                        MsDataSourceUtil.getExpDataFilter(expData.getContainer(), pathPrefix), null).exists());
            }
            else
            {
                // This is a directory source. Check for rows in exp.data for the expected directory contents.
                TableInfo expDataTInfo = expSvc.getTinfoData();
                return new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"), getExpDataFilter(expData, getDirContentsFilterClause()), null).exists();
            }
        }

        private SimpleFilter getExpDataFilter(ExpData expData, SimpleFilter.FilterClause filterClause)
        {
            SimpleFilter filter = SimpleFilter.createContainerFilter(expData.getContainer());

            String pathPrefix = expData.getDataFileUrl();
            filter.addClause(filterClause);
            if (!pathPrefix.endsWith("/"))
            {
                pathPrefix = pathPrefix + "/";
            }

            // StartsWithClause is not public.  And CompareClause with CompareType.STARTS_WITH is not the same thing as StartWithClause
            // Use addCondition() instead.
            filter.addCondition(FieldKey.fromParts("datafileurl"), pathPrefix, CompareType.STARTS_WITH);

            return filter;
        }
    }

    private static class MsMultiDataSource extends MsDataSource
    {
        private List<MsDataDirSource> _dirSources;
        private List<MsDataFileSource> _fileSources;

        private MsMultiDataSource()
        {
            super("unknown", Collections.emptyList());
            _fileSources = new ArrayList<>();
            _dirSources = new ArrayList<>();
        }

        private void addSource(MsDataSource source)
        {
            if(source instanceof MsDataFileSource)
            {
                _fileSources.add((MsDataFileSource) source);
            }
            else if(source instanceof  MsDataDirSource)
            {
                _dirSources.add((MsDataDirSource) source);
            }
        }

        @Override
        public boolean isFileSource()
        {
            return _dirSources.isEmpty();
        }

        @Override
        public boolean isValidNameAndPath(@NotNull Path path)
        {
            return _dirSources.stream().anyMatch(s -> s.isValidNameAndPath(path)) ||
                    _fileSources.stream().anyMatch(s -> s.isValidNameAndPath(path));
        }

        @Override
        boolean isValidPath(@NotNull Path path)
        {
            return _dirSources.stream().anyMatch(s -> s.isValidPath(path)) ||
                    _fileSources.stream().anyMatch(s -> s.isValidPath(path));
        }

        @Override
        public boolean isValidNameAndData(@NotNull ExpData data, @NotNull ExperimentService expSvc)
        {
            return _dirSources.stream().anyMatch(s -> s.isValidNameAndData(data, expSvc)) ||
                    _fileSources.stream().anyMatch(s -> s.isValidNameAndData(data, expSvc));
        }

        @Override
        boolean isValidData(@NotNull ExpData data, ExperimentService expSvc)
        {
            return _dirSources.stream().anyMatch(s -> s.isValidData(data, expSvc)) ||
                    _fileSources.stream().anyMatch(s -> s.isValidData(data, expSvc));
        }
    }

    private static class UnknownDataSource extends MsDataSource
    {
        private UnknownDataSource()
        {
            super("unknown", Collections.emptyList());
        }

        @Override
        boolean isValidPath(@NotNull Path path)
        {
            return false;
        }

        @Override
        boolean isValidData(@NotNull ExpData data, ExperimentService expSvc)
        {
            return false;
        }

        @Override
        boolean isFileSource()
        {
            return false;
        }
    }

    public static class RawDataInfo
    {
        private final ExpData _expData;
        private final Long _size;
        private final boolean _isFile;

        public RawDataInfo(ExpData expData, Long size, boolean isFile)
        {
            _expData = expData;
            _size = size;
            _isFile = isFile;
        }

        public ExpData getExpData()
        {
            return _expData;
        }

        public Long getSize()
        {
            return _size;
        }

        public boolean isFile()
        {
            return _isFile;
        }
    }

    public static class TestCase extends Assert
    {
        private static final String FOLDER_NAME = "TargetedMSDataSourceTest";
        private static final String SKY_FILE_NAME = "msdatasourcetest_9ab4da773526.sky.zip";
        private static final String TEST_DATA_FOLDER = "TargetedMS/Raw Data Test";
        private static User _user;
        private static Container _container;
        private static TargetedMSRun _run;
        private static Instrument _thermo;
        private static Instrument _sciex;
        private static Instrument _waters;
        private static Instrument _agilent;
        private static Instrument _bruker;
        private static Instrument _unknown;

        private static MsDataSourceUtil _util;

        private static final String[] thermoFiles = new String[] {"5Aug2017-FU2-PDK4-PRM2-2_1-01.raw", "5Aug2017-FU2-PDK4-PRM2-4_1-01.raw"};
        private static final String[] sciexFiles = new String [] {"20140807_nsSRM_01.wiff", "20140807_nsSRM_02.wiff"};
        private static final String[] watersData = new String[] {"20200929_CalMatrix_00_A.raw", "20200929_CalMatrix_00_A_flatzip.raw", "20200929_CalMatrix_00_A_nestedzip.raw"};
        private static final String[] brukerData = new String[] {"DK0034-G10_1-D,2_01_1036.d", "DK0034-G10_1-D,2_01_1036_flatzip.d", "DK0034-G10_1-D,2_01_1036_nestedzip.d"};
        private static final String[] agilentData = new String[] {"pTE219_0 hr_R2.d", "pTE219_0 hr_R2_flatzip.d", "pTE219_0 hr_R2_nestedzip.d"};

        @BeforeClass
        public static void setup()
        {
            try
            {
                cleanDatabase();
            }
            catch (ExperimentException e)
            {
                fail("Failed to clean up database before running tests. Error was: " + e.getMessage());
            }

            _user = TestContext.get().getUser();
            _container = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME);

            // Create an entry in the targetedms.runs table
            _run = createRun();

            // Create instruments
            _thermo = createInstrument("TSQ Altis");
            _sciex = createInstrument("Triple Quad 6500");
            _waters = createInstrument("Waters instrument model");
            _bruker = createInstrument("Bruker instrument model");
            _agilent = createInstrument("Agilent instrument model");
            _unknown = createInstrument("UWPR instrument model");

            _util = new MsDataSourceUtil();
        }

        private static TargetedMSRun createRun()
        {
            TargetedMSRun run = new TargetedMSRun();
            run.setContainer(_container);
            run.setFileName(SKY_FILE_NAME);
            Table.insert(_user, TargetedMSManager.getTableInfoRuns(), run);
            assertNotEquals("Id for saved run should not be 0", 0, run.getId());
            return run;
        }

        private static Instrument createInstrument(String model)
        {
            Instrument instrument = new Instrument();
            instrument.setRunId(_run.getId());
            instrument.setModel(model);
            Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
            assertNotEquals("Id for saved instrument should not be 0", 0, instrument.getId());
            return instrument;
        }

        @Test
        public void testDataExists() throws IOException
        {
            Path rawDataDir = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule(TargetedMSModule.class), TEST_DATA_FOLDER).toPath();

            testDataExists(thermoFiles, THERMO, rawDataDir);
            testDataExists(sciexFiles, SCIEX, rawDataDir);
            testDataExists(watersData, WATERS, rawDataDir);
            testDataExists(brukerData, BRUKER, rawDataDir);
            testDataExists(agilentData, AGILENT, rawDataDir);

            testDataNotExists(agilentData, BRUKER, rawDataDir); // Agilent data should not validate for Bruker
            testDataNotExists(agilentData, WATERS, rawDataDir); // Agilent data should not validate for Waters
            testDataNotExists(watersData, THERMO, rawDataDir);  // Waters data should not validate for Thermo

            // The test below fail because MsDataSourceUtil.dataExists() does not validate that the filename matches
            // the given datasource.  It only validates contents of directory based sources, and THERMO is a file-based source.
            // testDataNotExists(sciexFiles, THERMO, rawDataDir);
        }

        private void testDataExists(String[] files, MsDataSource sourceType, Path dataDir)
        {
            String message = "Expected " + (sourceType.isFileSource() ? "file or zip for " : "directory or zip for ") + sourceType.name() + ". File: ";
            for(String file: files)
            {
                assertTrue(message + file, _util.dataExists(file, sourceType, dataDir));
            }
        }

        private void testDataNotExists(String[] files, MsDataSource sourceType, Path dataDir)
        {
            String message = "Unxpected " + (sourceType.isFileSource() ? "file or zip for " : "directory or zip for ") + sourceType.name() + ". File: ";
            for(String file: files)
            {
                assertFalse(message + file, _util.dataExists(file, sourceType, dataDir));
            }
        }

        @Test
        public void testGetDataForSampleFile() throws IOException
        {
            Path rawDataDir = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule(TargetedMSModule.class), TEST_DATA_FOLDER).toPath();
            ExperimentService expSvc = ExperimentService.get();

            testGetDataForFileBasedSampleFiles(thermoFiles, _thermo, THERMO.name(), rawDataDir, expSvc);
            testGetDataForFileBasedSampleFiles(sciexFiles, _sciex, SCIEX.name(), rawDataDir, expSvc);
            testGetDataForWatersSampleFiles(rawDataDir, expSvc);
            testGetDataForAgilentSampleFiles(rawDataDir, expSvc);
            testGetDataForBrukerSampleFiles(rawDataDir, expSvc);
        }

        private void testGetDataForFileBasedSampleFiles(String[] files, Instrument instrument, String instrumentDataDirName,
                                                        Path rawDataDir, ExperimentService expSvc)
        {
            for(String fileName: files)
            {
                // Rows have not been created in exp.data. Should not find any matching rows.
                testNoDataForSampleFile(fileName, instrument, rawDataDir, expSvc);
            }
            // Create rows in exp.data
            for(String fileName: files)
            {
                addData(fileName, rawDataDir, instrumentDataDirName);
            }
            for(String fileName: files)
            {
                // We should find matching rows in exp.data
                testGetDataForSampleFile(fileName, instrument, rawDataDir, expSvc);
            }
        }

        private void testGetDataForWatersSampleFiles(Path rawDataDir, ExperimentService expSvc)
        {
            testGetDataForDirBasedSampleFiles(
                    "20200929_CalMatrix_00_A.raw", // For testing VALID unzipped directory
                    "20200929_CalMatrix_00_A_invalid.raw", // For testing INVALID unzipped directory
                    "20200929_CalMatrix_00_A_flatzip.raw", // For testing VALID ZIP
                    "20200929_CalMatrix_00_A_invalidzip.raw", // For testing INVALID ZIP
                    "_FUNC001.DAT", // Required content in directory
                    _waters, WATERS.name(),
                    rawDataDir, expSvc);
        }

        private void testGetDataForAgilentSampleFiles(Path rawDataDir, ExperimentService expSvc)
        {
            testGetDataForDirBasedSampleFiles(
                    "pTE219_0 hr_R2.d", // For testing VALID unzipped directory
                    "pTE219_0 hr_R2_invalid.d", // For testing INVALID unzipped directory
                    "pTE219_0 hr_R2_flatzip.d", // For testing VALID ZIP
                    "pTE219_0 hr_R2_invalidzip.d", // For testing INVALID ZIP
                    AGILENT_ACQ_DATA, // Required content in directory
                    _agilent, AGILENT.name(),
                    rawDataDir, expSvc);
        }

        private void testGetDataForBrukerSampleFiles(Path rawDataDir, ExperimentService expSvc)
        {
            testGetDataForDirBasedSampleFiles(
                    "DK0034-G10_1-D,2_01_1036.d", // For testing VALID unzipped directory
                    "DK0034-G10_1-D,2_01_1036_invalid.d", // For testing INVALID unzipped directory
                    "DK0034-G10_1-D,2_01_1036_flatzip.d", // For testing VALID ZIP
                    "DK0034-G10_1-D,2_01_1036_invalidzip.d", // For testing INVALID ZIP
                    BRUKER_ANALYSIS_BAF, // Required content in directory
                    _bruker, BRUKER.name(),
                    rawDataDir, expSvc);
        }

        private void testGetDataForDirBasedSampleFiles(String validDirName, String invalidDirName,
                                                       String validZipName, String invalidZipName,
                                                       String dirContentNameForValidDir, Instrument instrument, String instrumentDataDirName,
                                                       Path rawDataDir, ExperimentService expSvc)
        {
            String[] files = new String[] {validDirName, invalidDirName, validZipName, invalidZipName};
            for(String fileName: files)
            {
                // Rows have not yet been created in exp.data. Should not find any matching rows.
                testNoDataForSampleFile(fileName, instrument, rawDataDir, expSvc);
            }

            // Test valid data directory
            String name = validDirName;
            ExpData saved = addData(name, rawDataDir, instrumentDataDirName);
            testNoDataForSampleFile(name, instrument, rawDataDir, expSvc); // No rows created yet for the required directory content. Should not find any matches
            addData(dirContentNameForValidDir, Objects.requireNonNull(saved.getFilePath()), "");
            testGetDataForSampleFile(name, instrument, rawDataDir, expSvc); // Should find a match

            // Test invalid data directory
            name = invalidDirName;
            saved = addData(name, rawDataDir, instrumentDataDirName);
            testNoDataForSampleFile(name, instrument, rawDataDir, expSvc); // No rows created yet for the required directory content. Should not find any matches
            addData("invalid_" + dirContentNameForValidDir, Objects.requireNonNull(saved.getFilePath()), "");
            testNoDataForSampleFile(name, instrument, rawDataDir, expSvc); // Invalid directory. Should not find a match

            // Test valid zip
            name = validZipName;
            addData(name + ".ZIP", rawDataDir, instrumentDataDirName);
            testGetDataForSampleFile(name, instrument, rawDataDir, expSvc); // Should find a match
            testGetDataForSampleFileValidateZip(name, instrument, rawDataDir, expSvc); // Should find a match and zip validation should pass

            // Test invalid zip
            name = invalidZipName;
            addData(name + ".zip", rawDataDir, instrumentDataDirName);
            testGetDataForSampleFile(name, instrument, rawDataDir, expSvc); // Should find a match since we are not validating the zip file
            testNoDataForSampleFileValidateZip(name, instrument, rawDataDir, expSvc); // Should NOT find a match since zip validation will fail
        }

        private void testGetDataForSampleFile(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, true, false);
        }

        private void testNoDataForSampleFile(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, false, false);
        }

        private void testGetDataForSampleFileValidateZip(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, true, true);
        }

        private void testNoDataForSampleFileValidateZip(String file, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            testGetDataForSampleFile(file, instrument, dataDir, expSvc, false, true);
        }

        private void testGetDataForSampleFile(String file, Instrument instrument, Path dataDir, ExperimentService expSvc, boolean hasExpData, boolean validateZip)
        {
            SampleFile sf = new SampleFile();
            sf.setFilePath("C:\\rawfiles\\" + file);
            sf.setInstrumentId(instrument.getId());
            if(hasExpData)
            {
                String message = "Expected row in exp.data for " + file + (validateZip ? " with zip validation." : "");
                assertNotNull(message, _util.getDataForSampleFile(sf, _container, dataDir, expSvc, validateZip));
            }
            else
            {
                String message = "Unxpected row in exp.data for " + file + (validateZip ? " with zip validation." : "");
                assertNull(message, _util.getDataForSampleFile(sf, _container, dataDir, expSvc, validateZip));
            }
        }

        private ExpData addData(String fileName, Path rawDataDir, String subfolder)
        {
            Lsid lsid = new Lsid(ExperimentService.get().generateGuidLSID(_container, new DataType("UploadedFile")));
            ExpData data = ExperimentService.get().createData(_container, fileName, lsid.toString());

            data.setContainer(_container);
            data.setDataFileURI(rawDataDir.resolve(subfolder).resolve(fileName).toUri());
            data.save(_user);
            return data;
        }

        @Test
        public void testGetMsDataSource()
        {
            SampleFile sampleFile = new SampleFile();
            MsDataSourceUtil dataSourceUtil = new MsDataSourceUtil();

            // The following file extensions are unambiguous. Will not need an instrument model to resolve to a single data source
            sampleFile.setFilePath("C:\\RawData\\file.mzML");
            assertEquals(CONVERTED_DATA_SOURCE, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setFilePath("C:\\RawData\\file.mzXML");
            assertEquals(CONVERTED_DATA_SOURCE, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setFilePath("C:\\RawData\\Site54_190909_Study9S_PHASE-1.wiff|Site54_STUDY9S_PHASE1_6ProtMix_QC_03|2");
            assertEquals(SCIEX, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setFilePath("C:\\RawData\\file.lcd");
            assertEquals(SHIMADZU, dataSourceUtil.getMsDataSource(sampleFile));

            // Ambiguous extensions. Need an instrument model to resolve data source
            // .raw
            sampleFile.setFilePath("C:\\RawData\\file.raw?centroid_ms1=true&centroid_ms2=true");
            sampleFile.setInstrumentId(_thermo.getId());
            assertEquals(THERMO, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setInstrumentId(_waters.getId());
            assertEquals(WATERS, dataSourceUtil.getMsDataSource(sampleFile));
            // .d
            sampleFile.setFilePath("C:\\RawData\\file.d");
            sampleFile.setInstrumentId(_agilent.getId());
            assertEquals(AGILENT, dataSourceUtil.getMsDataSource(sampleFile));
            sampleFile.setInstrumentId(_bruker.getId());
            assertEquals(BRUKER, dataSourceUtil.getMsDataSource(sampleFile));


            // With an unknown instrument model we will not be able to resolve the .raw and .d data sources to a single data source type
            sampleFile.setInstrumentId(_unknown.getId());
            MsDataSource sourceType;
            // .raw
            sampleFile.setFilePath("C:\\RawData\\file.raw?centroid_ms1=true&centroid_ms2=true");
            sourceType = dataSourceUtil.getMsDataSource(sampleFile);
            assertTrue(sourceType instanceof MsMultiDataSource);
            MsMultiDataSource multiSourceType = (MsMultiDataSource) sourceType;
            assertEquals("Expected one directory source", 1, multiSourceType._dirSources.size());
            assertEquals("Expected Waters source", WATERS, multiSourceType._dirSources.get(0));
            assertEquals("Expected one file source", 1, multiSourceType._fileSources.size());
            assertEquals("Expected Thermo source", THERMO, multiSourceType._fileSources.get(0));

            // .d
            sampleFile.setFilePath("C:\\RawData\\file.d");
            sourceType = dataSourceUtil.getMsDataSource(sampleFile);
            assertTrue(sourceType instanceof MsMultiDataSource);
            multiSourceType = (MsMultiDataSource) sourceType;
            assertEquals("Expected 2 directory sources", 2, multiSourceType._dirSources.size());
            assertThat(multiSourceType._dirSources, hasItem(AGILENT));
            assertThat(multiSourceType._dirSources, hasItem(BRUKER));
            assertEquals("Expected 0 file source", 0, multiSourceType._fileSources.size());


            sampleFile.setFilePath("C:\\RawData\\unknowntype");
            sourceType = dataSourceUtil.getMsDataSource(sampleFile);
            assertTrue("Expected UnknownDataSource", sourceType instanceof UnknownDataSource);
        }

        @Test
        public void testGetSourceForInstrument()
        {
            MsDataSourceUtil dataSourceUtil = MsDataSourceUtil.getInstance();
            Instrument instrument = new Instrument();
            assertNull(dataSourceUtil.getSourceForInstrument(instrument));

            // These are some of the values in the "model" column of the targetedms.instrument table on PanoramaWeb
            // Specific model names are only available for Thermo and SCIEX instruments.

            instrument.setModel("Thermo Electron instrument model");
            assertEquals(THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Orbitrap Exploris 480");
            assertEquals(THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TSQ Altis");
            assertEquals(THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TSQ Quantum Ultra AM");
            assertEquals(THERMO, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("ITQ 1100");
            assertEquals(THERMO, dataSourceUtil.getSourceForInstrument(instrument));


            instrument.setModel("AB SCIEX instrument model");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("SCIEX instrument model");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Applied Biosystems instrument model");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("4000 QTRAP");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("QTRAP 5500");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("QSTAR Elite");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TripleTOF 5600");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("TripleTOF 6600");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Triple Quad 4500");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));
            instrument.setModel("Triple Quad 6500");
            assertEquals(SCIEX, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Bruker Daltonics maXis series");
            assertEquals(BRUKER, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Waters instrument model");
            assertEquals(WATERS, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Agilent instrument model");
            assertEquals(AGILENT, dataSourceUtil.getSourceForInstrument(instrument));

            instrument.setModel("Shimadzu instrument model");
            assertEquals(SHIMADZU, dataSourceUtil.getSourceForInstrument(instrument));
        }

        private static void cleanDatabase() throws ExperimentException
        {
            TableInfo runsTable = TargetedMSManager.getTableInfoRuns();
            Integer runId = new TableSelector(TargetedMSManager.getTableInfoRuns(), Collections.singletonList(runsTable.getColumn("id")),
                    new SimpleFilter().addCondition(runsTable.getColumn("filename"), SKY_FILE_NAME, CompareType.EQUAL), null).getObject(Integer.class);
            if(runId != null)
            {
                Table.delete(TargetedMSManager.getTableInfoInstrument(),
                        new SimpleFilter(new CompareType.CompareClause(FieldKey.fromParts("runId"), CompareType.EQUAL, runId)));
                Table.delete(TargetedMSManager.getTableInfoRuns(), runId);
            }

            ExperimentService.get().deleteAllExpObjInContainer(_container, _user);
        }

        @AfterClass
        public static void cleanup()
        {
            try
            {
                cleanDatabase();
            }
            catch (ExperimentException e)
            {
                fail("Failed to clean up database after running tests. Error was: " + e.getMessage());
            }
        }
    }
}
