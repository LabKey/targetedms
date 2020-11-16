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
import org.labkey.api.targetedms.MsDataSourceService;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.Pair;
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
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.hasItem;

public class MsDataSourceUtil implements MsDataSourceService
{
    private static final MsDataSourceUtil instance = new MsDataSourceUtil();
    private static final Logger LOG = Logger.getLogger(MsDataSourceUtil.class);

    private MsDataSourceUtil() {}

    public static MsDataSourceUtil getInstance()
    {
        return instance;
    }

    /**
     * @return Pair of ExpData for the given sample file and the file size on the filesystem
     */
    public Pair<ExpData, Long> getDownloadInfo(SampleFile sampleFile, Container container)
    {
        ExpData expData = getDataForSampleFile(sampleFile, container);
        Long size = null;
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
                    }
                    else
                    {
                        size = FileUtil.hasCloudScheme(dataPath) ? null : FileUtils.sizeOfDirectory(dataPath.toFile());
                    }
                }
                catch (IOException e)
                {
                    LOG.debug("Error getting size of " + dataPath, e);
                }
            }

        }
        return new Pair<>(expData, size);
    }

    private ExpData getDataForSampleFile(SampleFile sampleFile, Container container)
    {
        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return null;
        }
        // We will look for raw data files only in @files/RawFiles
        Path rawFilesDir = getRawFilesDir(container);
        return getDataForSampleFile(sampleFile, container, rawFilesDir, expSvc, false);
    }

    private ExpData getDataForSampleFile(SampleFile sampleFile, Container container, Path rawFilesDir, ExperimentService expSvc, boolean validateZip)
    {
        List<? extends ExpData> expDatas = getExpData(sampleFile.getFileName(), container, rawFilesDir, expSvc);

        if(expDatas.size() == 0)
        {
            return null;
        }

        if(!validateZip)
        {
            for(ExpData data: expDatas)
            {
                // Prefer to return a zip source if we found one
                if(isZip(data.getName()))
                {
                    return  data;
                }
            }
            // Get the source type file from the file extension and (if required) the instrument associated with the sample file
            MsDataSource sourceType = getMsDataSource(sampleFile);
            for(ExpData data: expDatas)
            {
                if(sourceType.isValidSource(data, expSvc))
                {
                    return data;
                }
            }

        }
        else
        {
            // Get the source type file from the file extension and (if required) the instrument associated with the sample file
            MsDataSource sourceType = getMsDataSource(sampleFile);
            if(sourceType != null)
            {
                for(ExpData data: expDatas)
                {
                    boolean isZip = isZip(data.getName());
                    boolean isValid = isZip ? sourceType.isValidSource(data.getFilePath()) : sourceType.isValidSource(data, expSvc);
                    if(isValid)
                    {
                        return data;
                    }
                }
            }
        }

        return null;
    }

    @NotNull
    private List<? extends ExpData> getExpData(String fileName, Container container, Path pathPrefix, ExperimentService expSvc)
    {
        // Look for the file and file.zip (e.g. sample_1.raw and sample_1.raw.zip)
        String[] fileNames = new String[] {fileName, fileName + EXT_ZIP};

        SimpleFilter.OrClause orClause = new SimpleFilter.OrClause();
        for(String name: fileNames)
        {
            // TODO: Is this a case insensitive comparision??
            orClause.addClause(new CompareType.EqualsCompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, name));
        }

        String pathPrefixString = FileUtil.pathToString(pathPrefix); // Encoded URI string
        TableInfo expDataTInfo = expSvc.getTinfoData();
        ArrayList<Integer> expDataIds = new TableSelector(expDataTInfo,
                expDataTInfo.getColumns("RowId"), getExpDataFilter(container, pathPrefixString, orClause), null).getArrayList(Integer.class);
        return expSvc.getExpDatas(expDataIds);
    }

    private static SimpleFilter getExpDataFilter(Container container, String pathPrefix, SimpleFilter.FilterClause filterClause)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        if(filterClause != null)
        {
            filter.addClause(filterClause);
        }
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

    private MsDataSource getMsDataSource(ISampleFile sampleFile)
    {
        List<MsDataSource> sourceTypes = getSourceForName(sampleFile.getFileName());
        if(sourceTypes.size() > 1)
        {
            // We can get more than one source type by filename extension lookup. e.g. .raw for Thermo and Waters
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

        return sourceTypes.size() > 0 ? sourceTypes.get(0) : null;
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

    /**
     *
     * @param sampleFiles list of sample files for which we should check if data exists
     * @param container container where we should look for data
     * @return list of sample files for which data was found
     */
    @NotNull
    public List<? extends ISampleFile> hasData(@NotNull List<? extends ISampleFile> sampleFiles, @NotNull Container container)
    {
        List<ISampleFile> hasData = new ArrayList<>();

        Path rawFilesDir = getRawFilesDir(container);
        if (rawFilesDir == null || !Files.exists(rawFilesDir))
        {
            return hasData;
        }

        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return hasData;
        }

        for(ISampleFile sampleFile: sampleFiles)
        {
            MsDataSource dataSource = getMsDataSource(sampleFile);

            String fileName = sampleFile.getFileName();
            if(hasData(fileName, dataSource, container, rawFilesDir, expSvc))
            {
                hasData.add(sampleFile);
            }
        }
        return hasData;
    }

    private boolean hasData(String fileName, MsDataSource dataSource, Container container, Path rawFilesDir, ExperimentService expSvc)
    {
        List<? extends ExpData> expDatas = getExpData(fileName, container, rawFilesDir, expSvc);

        if(expDatas.size() > 0)
        {
            for (ExpData data : expDatas)
            {
                if (dataSource.isValidNameAndSource(data, expSvc))
                {
                   return true;
                }
            }
        }
        else if(!FileContentService.get().isCloudRoot(container))
        {
            // No matches found in exp.data.  Look on the filesystem
            if(dataExists(fileName, dataSource, rawFilesDir))
            {
                return true;
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
           return sourceType.isValidSource(path);
        }
        return false;
    }

    private List<MsDataSource> getSourceForName(String name)
    {
        // Can return more than one data source type. For example, Bruker and Waters both have .d extension;
        // Thermo and Waters both have .raw extension
        List<MsDataSource> sources = EXTENSION_SOURCE_MAP.get(extension(name));
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
        public Predicate<Path> getDirContentCondition()
        {
            return f -> !Files.isDirectory(f) && FileUtil.getFileName(f).matches("^_FUNC.*\\.DAT$");
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
        public Predicate<Path> getDirContentCondition()
        {
            return f -> Files.isDirectory(f) && FileUtil.getFileName(f).equals(AGILENT_ACQ_DATA);
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
        public Predicate<Path> getDirContentCondition()
        {
            return f -> !Files.isDirectory(f) && (FileUtil.getFileName(f).equals(BRUKER_ANALYSIS_BAF) || FileUtil.getFileName(f).equals(BRUKER_ANALYSIS_TDF));
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
                List<MsDataSource> sources = EXTENSION_SOURCE_MAP.get(ext);
                if(sources == null)
                {
                    sources = new ArrayList<>();
                    EXTENSION_SOURCE_MAP.put(ext, sources);
                }
                sources.add(s);
            }
        }
    }


    private abstract static class MsDataSource
    {
        private List<String> _extensions;
        private String _instrumentVendor;

        private MsDataSource() {}

        private MsDataSource(@NotNull String instrumentVendor, @NotNull List<String> extensions)
        {
            _instrumentVendor = instrumentVendor;
            _extensions = extensions;
        }

        public boolean isSourceName(String name)
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

                if(isZipSourceName(name))
                {
                    return true;
                }
            }
            return false;
        }

        boolean isZipSourceName(String fileName)
        {
            return MsDataSourceUtil.isZip(fileName) && isSourceName(FileUtil.getBaseName(fileName));
        }

        /* instrument can be the instrument model (from Instrument) or vendor from the PSI-MS instrument list */
        public boolean isInstrumentSource(String instrument)
        {
            return instrument != null && instrument.toLowerCase().contains(_instrumentVendor);
        }

        abstract boolean isValidNameAndSource(@NotNull Path path);
        abstract boolean isValidSource(@NotNull Path path);
        abstract boolean isValidNameAndSource(@NotNull ExpData data, @NotNull ExperimentService expSvc);
        abstract boolean isValidSource(@NotNull ExpData data, ExperimentService expSvc);

        // abstract boolean isZipSource(@NotNull Path path);
        // abstract boolean isZipSource(@NotNull ExpData data, @NotNull ExperimentService expSvc, boolean validateZip);

        public String toString()
        {
            return name();
        }
        public String name()
        {
            return _instrumentVendor;
        }
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
        boolean isValidNameAndSource(@NotNull Path path)
        {
            String name = FileUtil.getFileName(path);
            return isSourceName(name) && isValidSource(path);
        }

        @Override
        boolean isValidSource(@NotNull Path path)
        {
            if(Files.exists(path) && !Files.isDirectory(path))
            {
                return MsDataSourceUtil.isZip(FileUtil.getFileName(path)) ? zipMatches(path) : true;
            }
            return false;
        }

        @Override
        boolean isValidNameAndSource(ExpData data, ExperimentService expSvc)
        {
            return isSourceName(data.getName()) && isValidSource(data, expSvc);
        }

        @Override
        boolean isValidSource(@NotNull ExpData data, ExperimentService expSvc)
        {
            String pathPrefix = data.getDataFileUrl();
            TableInfo expDataTInfo = expSvc.getTinfoData();
            return !(new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"),
                    MsDataSourceUtil.getExpDataFilter(data.getContainer(), pathPrefix, null), null).exists());
        }

        private boolean zipMatches(@NotNull Path path)
        {
            try
            {
                for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                        .getRootDirectories())
                {
                    String basename = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                    Path file = root.resolve(basename);
                    if(Files.exists(file) && !Files.isDirectory(file))
                    {
                        return true;
                    }
                }
            }
            catch (IOException e)
            {
                LOG.debug("Error validating zip source " + path, e);
            }
            return false;
        }
    }

    private static abstract class MsDataDirSource extends MsDataSource
    {
        // Condition for validating directory or zip contents
        abstract Predicate<Path> getDirContentCondition();

        // Filter for validating a directory source by finding rows for directory contents in the exp.data table.
        // Used only if the source directory was uploaded without auto-zipping (e.g. when the directory was uploaded to
        // a network drive mapped to a LabKey folder)
        abstract SimpleFilter.FilterClause getDirContentsFilterClause();

        private MsDataDirSource(String instrumentVendor, String extension)
        {
            super(instrumentVendor, Collections.singletonList(extension));
        }

        @Override
        public boolean isValidNameAndSource(@NotNull Path path)
        {
            return isSourceName(FileUtil.getFileName(path)) && isValidSource(path);
        }

        @Override
        public boolean isValidSource(@NotNull Path path)
        {
            if(Files.exists(path))
            {
                String fileName = FileUtil.getFileName(path);
                if(MsDataSourceUtil.isZip(fileName))
                {
                    return !Files.isDirectory(path) && zipMatches(path);
                }
                else
                {
                    return Files.isDirectory(path) && matchDirContents(path);
                }
            }
            return false;
        }

        private boolean matchDirContents(@NotNull Path path)
        {
            try
            {
                return Files.list(path).anyMatch(getDirContentCondition());
            }
            catch (IOException e)
            {
                LOG.debug("Error validating directory source " + path, e);
            }
            return false;
        }

        private boolean zipMatches(@NotNull Path path)
        {
            try
            {
                for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                        .getRootDirectories())
                {
                    boolean fileInRoot = matchDirContents(root);
                    if (fileInRoot)
                    {
                        return true;
                    }
                    else
                    {
                        String subdirName = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                        // Look for match in the subdirectory
                        Path subDir = root.resolve(subdirName);
                        if(Files.exists(subDir) && matchDirContents(subDir))
                        {
                            return true;
                        }
//                        Path subDir = Files.list(root).filter(p -> Files.isDirectory(p) && subdir.equals(FileUtil.getFileName(p))).findFirst().orElse(null);
//                        if (subDir != null)
//                        {
//                            return matchDirContents(subDir);
//                        }
                    }
                }
            }
            catch (IOException e)
            {
                LOG.debug("Error validating zip source " + path, e);
            }
            return false;
        }

        @Override
        boolean isValidNameAndSource(ExpData expData, ExperimentService expSvc)
        {
            return isSourceName(expData.getName()) && isValidSource(expData, expSvc);
        }

        @Override
        boolean isValidSource(@NotNull ExpData expData, ExperimentService expSvc)
        {
            String fileName = expData.getName();
            if(MsDataSourceUtil.isZip(fileName))
            {
                String pathPrefix = expData.getDataFileUrl();
                TableInfo expDataTInfo = expSvc.getTinfoData();
                return !(new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"),
                        MsDataSourceUtil.getExpDataFilter(expData.getContainer(), pathPrefix, null), null).exists());
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
        boolean isValidNameAndSource(Path path)
        {
            // Check directory sources
            for(MsDataDirSource sourceType: _dirSources)
            {
                if(sourceType.isValidNameAndSource(path))
                {
                    return true;
                }
            }
            // Check file sources
            for(MsDataFileSource sourceType: _fileSources)
            {
                if(sourceType.isValidNameAndSource(path))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        boolean isValidSource(@NotNull Path path)
        {
            // Check directory sources
            for(MsDataDirSource sourceType: _dirSources)
            {
                if(sourceType.isValidSource(path))
                {
                    return true;
                }
            }
            // Check file sources
            for(MsDataFileSource sourceType: _fileSources)
            {
                if(sourceType.isValidSource(path))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        boolean isValidNameAndSource(ExpData data, ExperimentService expSvc)
        {
            // Check directory sources
            for(MsDataDirSource sourceType: _dirSources)
            {
                if(sourceType.isValidNameAndSource(data, expSvc))
                {
                    return true;
                }
            }
            // Check file sources
            for(MsDataFileSource sourceType: _fileSources)
            {
                if(sourceType.isValidNameAndSource(data, expSvc))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        boolean isValidSource(ExpData data, ExperimentService expSvc)
        {
            // Check directory sources
            for(MsDataDirSource sourceType: _dirSources)
            {
                if(sourceType.isValidSource(data, expSvc))
                {
                    return true;
                }
            }
            // Check file sources
            for(MsDataFileSource sourceType: _fileSources)
            {
                if(sourceType.isValidSource(data, expSvc))
                {
                    return true;
                }
            }
            return false;
        }
    }

    public static class TestCase extends Assert
    {
        private static final String FOLDER_NAME = "TargetedMSDataSourceTest";
        private static final String _fileName = "msdatasourcetest_9ab4da773526.sky.zip";
        private static User _user;
        private static Container _container;
        private static TargetedMSRun _run;
        private static Instrument _thermo;
        private static Instrument _sciex;
        private static Instrument _shimadzu;
        private static Instrument _waters;
        private static Instrument _agilent;
        private static Instrument _bruker;
        private static Instrument _unknown;

        private static MsDataSourceUtil _util;

        private static final String[] sciexFiles = new String [] {"20140807_nsSRM_01.wiff", "20140807_nsSRM_02.wiff"};
        private static final String[] watersData = new String[] {"20200929_CalMatrix_00_A.raw", "20200929_CalMatrix_01_A.raw", "20200929_CalMatrix_01_A_nestedzip.raw"};
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
                fail("Failed to clean up database after before running tests. Error was: " + e.getMessage());
            }

            _user = TestContext.get().getUser();
            _container = ContainerManager.ensureContainer(JunitUtil.getTestContainer(), FOLDER_NAME);

            // Create an entry in targetedms.runs table
            _run = createRun();

            // Create instruments
            _thermo = createInstrument("TSQ Altis");
            _sciex = createInstrument("Triple Quad 6500");
            _shimadzu = createInstrument("Shimadzu instrument model");
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
            run.setFileName(_fileName);
            Table.insert(_user, TargetedMSManager.getTableInfoRuns(), run);
            return run;
        }

        private static Instrument createInstrument(String model)
        {
            Instrument instrument = new Instrument();
            instrument.setRunId(_run.getId());
            instrument.setModel(model);
            Table.insert(_user, TargetedMSManager.getTableInfoInstrument(), instrument);
            return instrument;
        }

        @Test
        public void testDataExists() throws IOException
        {
            Path rawDataDir = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule(TargetedMSModule.class), "TargetedMS/RawDataTest").toPath();
            ExperimentService expSvc = ExperimentService.get();

            testDataExists(sciexFiles, SCIEX, rawDataDir);
            testDataExists(watersData, WATERS, rawDataDir);
            testDataExists(brukerData, BRUKER, rawDataDir);
            testDataExists(agilentData, AGILENT, rawDataDir);

            testDataNotExists(agilentData, BRUKER, rawDataDir);
            testDataNotExists(agilentData, WATERS, rawDataDir);
            testDataNotExists(watersData, THERMO, rawDataDir);
            // This will fail because MsDataSourceUtil.dataExists() does not validate that the filename matches
            // the given datasource.  It only validates zip contents and contents of directory bases sources
            // testDataNotExists(sciexFiles, THERMO, rawDataDir);
        }

        private void testDataExists(String[] files, MsDataSource sourceType, Path dataDir)
        {
            for(String file: files)
            {
                // test private boolean hasData(String fileName, MsDataSource dataSource, Container container, Path rawFilesDir, ExperimentService expSvc)
                assertTrue("Expected data for " + file, _util.dataExists(file, sourceType, dataDir));
            }
        }

        private void testDataNotExists(String[] files, MsDataSource sourceType, Path dataDir)
        {
            for(String file: files)
            {
                // test private boolean hasData(String fileName, MsDataSource dataSource, Container container, Path rawFilesDir, ExperimentService expSvc)
                assertFalse("Unexpected data for " + file, _util.dataExists(file, sourceType, dataDir));
            }
        }

        @Test
        public void testGetDataForSampleFile() throws IOException
        {
            Path rawDataDir = JunitUtil.getSampleData(ModuleLoader.getInstance().getModule(TargetedMSModule.class), "TargetedMS/RawDataTest").toPath();
            ExperimentService expSvc = ExperimentService.get();

            // Create some rows in exp.data
            for(String fileName: sciexFiles)
            {
                addData(fileName, rawDataDir, SCIEX.name());
            }
            for(String fileName: watersData)
            {
                ExpData saved = addData(fileName, rawDataDir, WATERS.name());
                addData("_FUNC001.DAT", saved.getFilePath(), "");
            }
            for(String fileName: agilentData)
            {
                ExpData saved = addData(fileName, rawDataDir, AGILENT.name());
                addData(AGILENT_ACQ_DATA, saved.getFilePath(), "");
            }
            for(String fileName: brukerData)
            {
                ExpData saved = addData(fileName, rawDataDir, BRUKER.name());
                addData(BRUKER_ANALYSIS_BAF, saved.getFilePath(), "");
            }


            testGetDataForSampleFiles(sciexFiles, _sciex, rawDataDir, expSvc);
            testGetDataForSampleFiles(watersData, _waters, rawDataDir, expSvc);
            testGetDataForSampleFiles(brukerData, _bruker, rawDataDir, expSvc);
            testGetDataForSampleFiles(agilentData, _agilent, rawDataDir, expSvc);

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

        private void testGetDataForSampleFiles(String[] files, Instrument instrument, Path dataDir, ExperimentService expSvc)
        {
            for(String file: files)
            {
                SampleFile sf = new SampleFile();
                sf.setFilePath("C:\\rawfiles\\" + file);
                sf.setInstrumentId(instrument.getId());
                assertNotNull("Expected row in exp.data for " + file, _util.getDataForSampleFile(sf, _container, dataDir, expSvc, false));
            }
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
            assertNull("Expected null source type", sourceType);
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

        @Test
        public void testExtension()
        {
            assertTrue(".raw".equals(extension("QC_10.9.17.raw")));
            assertTrue(".raw".equals(extension("QC_10.9.17.RAW")));
            assertTrue(".zip".equals(extension("QC_10.9.17.RAW.zip")));
            assertTrue(".zip".equals(extension("QC_10.9.17.d.ZIP")));
        }

        private static void cleanDatabase() throws ExperimentException
        {
            TableInfo runsTable = TargetedMSManager.getTableInfoRuns();
            Integer runId = new TableSelector(TargetedMSManager.getTableInfoRuns(), Collections.singletonList(runsTable.getColumn("id")),
                    new SimpleFilter().addCondition(runsTable.getColumn("filename"), _fileName, CompareType.EQUAL), null).getObject(Integer.class);
            if(runId != null)
            {
                Table.delete(TargetedMSManager.getTableInfoInstrument(),
                        new SimpleFilter(new CompareType.CompareClause(FieldKey.fromParts("runId"), CompareType.EQUAL, runId)));
                Table.delete(TargetedMSManager.getTableInfoRuns(), runId);
            }

            // TableInfo expDataTable = ExperimentService.get().getTinfoData();
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
