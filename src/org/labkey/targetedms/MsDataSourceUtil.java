package org.labkey.targetedms;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.MsDataSourceService;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.PsiInstruments;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.ReplicateManager;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MsDataSourceUtil implements MsDataSourceService
{
    private static final MsDataSourceUtil instance = new MsDataSourceUtil();

    private MsDataSourceUtil() {}

    public static MsDataSourceUtil getInstance()
    {
        return instance;
    }

    public Pair<ExpData, Long> getDownloadInfo(SampleFile sampleFile, Container container)
    {
        ExpData expData = getDataFor(sampleFile, container, false);
        Long size = null;
        if(expData != null)
        {
            Path filePath = expData.getFilePath();
            if(filePath != null && Files.exists(filePath))
            {
                try
                {
                    // TODO:
                    size = Files.isDirectory(filePath) ? FileUtils.sizeOfDirectory(filePath.toFile()) : Files.size(filePath);
                }
                catch (IOException ignored){}
            }

        }
        return new Pair<>(expData, size);
    }

    public ExpData getDataFor(SampleFile sampleFile, Container container, boolean validateZip)
    {
        // We will look for raw data files only in @files/RawFiles
        Path rawFilesDir = getRawFilesDir(container);
        if (rawFilesDir == null || !Files.exists(rawFilesDir))
        {
            return null;
        }

        ExperimentService expSvc = ExperimentService.get();
        if(expSvc == null)
        {
            return null;
        }

        List<MsDataSource> sourceTypes = getPotentialMsDataSources(sampleFile);
        return getDataFor(sampleFile, sourceTypes, container, rawFilesDir, expSvc, validateZip);
    }

    private ExpData getDataFor(ISampleFile sampleFile, List<MsDataSource> sourceTypes, Container container, Path rawFilesDir, ExperimentService expSvc, boolean validateZip)
    {
        String fileName = FilenameUtils.getName(sampleFile.getFilePath());
        for(MsDataSource source: sourceTypes)
        {
            String[] filenames = source.isFileSource() ? new String[]{fileName}
                    : new String[]{fileName, fileName + EXT_ZIP}; // For a directory based source look for a zip file as well
            ExpData data = getDataFor(filenames, source, container, rawFilesDir, expSvc, validateZip);
            if(data != null)
            {
                return data;
            }
        }
        return null;
    }

    @NotNull
    private List<MsDataSource> getPotentialMsDataSources(ISampleFile sampleFile)
    {
        List<MsDataSource> sourceTypes = getSourceForName(sampleFile.getFileName());
        if(sourceTypes.size() > 1)
        {
            // We can get more than one source type by filename extension lookup. e.g. .raw for Thermo and Waters
            // Try to resolve by looking up the instrument on which the data was acquired.
            Instrument instrument = ReplicateManager.getInstrument(sampleFile.getInstrumentId());
            if(instrument != null)
            {
                MsDataSource source = getSourceForInstrument(instrument);
                if(source != null)
                {
                    sourceTypes = Collections.singletonList(source);
                }
            }
        }
        return sourceTypes;
    }

    private ExpData getDataFor(String[] fileNames, MsDataSource dataSource, Container container, Path rawFilesDir, ExperimentService expSvc, boolean validateZip)
    {
        List<? extends ExpData> expDatas = getExpData(fileNames, container, rawFilesDir, expSvc);

        if(dataSource.isFileSource())
        {
           return expDatas.size() > 0 ? expDatas.get(0) : null;
        }

        for (ExpData expData: expDatas)
        {
            if(expData.getName().toLowerCase().endsWith(EXT_ZIP))
            {
                return validateZip ? (validateZip(expData, dataSource, container) ? expData : null) : expData;
            }
            else
            {
                SimpleFilter expDataFilter = getExpDataFilter(container, expData.getDataFileUrl(), ((MsDataDirSource)dataSource).getDirContentsFilterClause());
                if(expDataExists(expSvc, expDataFilter))
                {
                    return expData;
                }
            }
        }
        return null;
    }

    private boolean validateZip(ExpData expData, MsDataSource dataSource, Container container)
    {
        FileContentService fcs = FileContentService.get();
        if(fcs != null && !fcs.isCloudRoot(container))
        {
            Path path = expData.getFilePath();
            return path != null && !dataSource.isFileSource() && dataSource.isSource(path);
        }
        return false;
    }

    @NotNull
    private List<? extends ExpData> getExpData(String[] fileNames, Container container, Path pathPrefix, ExperimentService expSvc)
    {
        SimpleFilter.OrClause orClause = new SimpleFilter.OrClause();
        for(String fileName: fileNames)
        {
            orClause.addClause(new CompareType.EqualsCompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, fileName));
        }

        SimpleFilter filter = getExpDataFilter(container, pathPrefix, orClause);

        return getExpData(expSvc, filter);
    }

    @NotNull
    private List<? extends ExpData> getExpData(ExperimentService expSvc, SimpleFilter filter)
    {
        TableInfo expDataTInfo = expSvc.getTinfoData();
        ArrayList<Integer> expDataIds = new TableSelector(expDataTInfo,
                expDataTInfo.getColumns("RowId"), filter, null).getArrayList(Integer.class);
        return expSvc.getExpDatas(expDataIds);
    }

    private boolean expDataExists(ExperimentService expSvc, SimpleFilter filter)
    {
        TableInfo expDataTInfo = expSvc.getTinfoData();
        return new TableSelector(expDataTInfo, expDataTInfo.getColumns("RowId"), filter, null).exists();
    }

    private SimpleFilter getExpDataFilter(Container container, Path pathPrefix, SimpleFilter.FilterClause filterClause)
    {
        String pathPrefixString = FileUtil.pathToString(pathPrefix); // Encoded URI string
        return getExpDataFilter(container, pathPrefixString, filterClause);
    }

    private SimpleFilter getExpDataFilter(Container container, String pathPrefix, SimpleFilter.FilterClause filterClause)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        filter.addClause(filterClause);
        if (!pathPrefix.endsWith("/"))
        {
            pathPrefix = pathPrefix + "/";
        }
        // StartsWithClause is not public.  And CompareClause with CompareType.STARTS_WITH is not the same thing as StartWithClause
        // Add as a condition instead.
        filter.addCondition(FieldKey.fromParts("datafileurl"), pathPrefix, CompareType.STARTS_WITH);
        return filter;
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

    public List<? extends ISampleFile> hasData(List<? extends ISampleFile> sampleFiles, Container container)
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
            List<MsDataSource> sourceTypes = getPotentialMsDataSources(sampleFile);

            ExpData data = getDataFor(sampleFile, sourceTypes, container, rawFilesDir, expSvc, true);
            if(data != null)
            {
                hasData.add(sampleFile);
            }
            else
            {
                String fileName = FilenameUtils.getName(sampleFile.getFilePath());

                if(dataExists(fileName, sourceTypes, rawFilesDir))
                {
                    hasData.add(sampleFile);
                }
            }
        }
        return hasData;
    }

    private boolean dataExists(String fileName, List<MsDataSource> sourceTypes, Path rawFilesDir)
    {
        if(dataExists(fileName, rawFilesDir, sourceTypes))
        {
            return true;
        }

        // Look in subdirectories
        try (Stream<Path> list = Files.walk(rawFilesDir).filter(p -> Files.isDirectory(p)))
        {
            for (Path subDir : list.collect(Collectors.toList()))
            {
                if (dataExists(fileName, subDir, sourceTypes))
                {
                    return true;
                }
            }
        }
        catch (IOException ignored) {} // TODO: Log exception?

        return false;
    }

    private boolean dataExists(String fileName, Path dir, List<MsDataSource> sourceTypes)
    {
        Path filePath = dir.resolve(fileName);
        for(MsDataSource sourceType: sourceTypes)
        {
            return sourceType.isSource(filePath) || (!sourceType.isFileSource() && sourceType.isSource(dir.resolve(fileName + EXT_ZIP)));
        }

        return false;
    }

    private List<MsDataSource> getSourceForName(String name)
    {
        // Can return more than one data source type. For example, Bruker and Waters both have .d extension;
        // Thermo and Waters both have .raw extension
        return EXTENSION_SOURCE_MAP.get(extensionLowerCase(name));
    }

    private static String extensionLowerCase(String name)
    {
        String ext = extension(name);
        if(EXT_ZIP.equals(ext))
        {
            name = name.substring(0, name.length() - EXT_ZIP.length());
            String ext2 = extension(name);
            return ext2 + ext; // Example: .d.zip, .raw.zip
        }
        return ext;
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
        // 2. The instrument model associated with the sample file may be something general like "Waters instrument model".
        //    This is generally seen in Skyline documents for data from non-Thermo instruments
        PsiInstruments.PsiInstrument psiInstrument = PsiInstruments.getInstrument(instrument.getModel());
        String vendorOrModel = psiInstrument != null ? psiInstrument.getVendor() : instrument.getModel();
        return Arrays.stream(sourceTypes).filter(s -> s.isInstrumentSource(vendorOrModel)).findFirst().orElse(null);
    }

    private static final MsDataSource CONVERTED_DATA_SOURCE = new MsDataFileSource(Arrays.asList(".mzxml", ".mzml", ".mz5", ".mzdata"));
    private static final MsDataSource THERMO = new MsDataFileSource("thermo", ".raw");
    private static final MsDataSource SCIEX = new MsDataFileSource(Arrays.asList("sciex", "applied biosystems"), Arrays.asList(".wiff", ".wiff2", ".wiff.scan", ".wiff2.scan"))
    {
        @Override
        public boolean isInstrumentSource(String instrument)
        {
            return super.isInstrumentSource(instrument) || (instrument != null && instrument.contains("applied biosystems"));
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

    private static final MsDataDirSource AGILENT = new MsDataDirSource("agilent", ".d")
    {
        @Override
        public Predicate<Path> getDirContentCondition()
        {
            return f -> Files.isDirectory(f) && FileUtil.getFileName(f).equals("AcqData");
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, "AcqData");
        }
    };

    private static final MsDataDirSource BRUKER = new MsDataDirSource("bruker", ".d")
    {
        @Override
        public Predicate<Path> getDirContentCondition()
        {
            return f -> !Files.isDirectory(f) && (FileUtil.getFileName(f).equals("analysis.baf") || FileUtil.getFileName(f).equals("analysis.tdf"));
        }

        @Override
        public SimpleFilter.FilterClause getDirContentsFilterClause()
        {
            return new SimpleFilter.OrClause(
                    new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, "analysis.baf"),
                    new CompareType.CompareClause(FieldKey.fromParts("Name"), CompareType.EQUAL, "analysis.tdf")
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
        private List<String> _instrumentVendors;

        private MsDataSource(@NotNull List<String> instrumentVendors, @NotNull List<String> extensions)
        {
            _instrumentVendors = instrumentVendors;
            _extensions = extensions;
        }

        public boolean isSourceName(String name)
        {
            if(name != null)
            {
                String nameLc = name.toLowerCase();
                return _extensions.stream().anyMatch(nameLc::endsWith);
            }
            return false;
        }

        /* instrument can be instrument model (from Instrument) or vendor from the PSI-MS vocabulary */
        public boolean isInstrumentSource(String instrument)
        {
            if(instrument != null)
            {
                String instrumentLc = instrument.toLowerCase();
                return _instrumentVendors.stream().anyMatch(instrumentLc::contains);
            }
            return false;
        }

        public String getInstrumentVendor()
        {
            return _instrumentVendors.isEmpty() ? null : _instrumentVendors.get(0); // Only SCIEX has two names for instrument vendor
        }

        abstract boolean isFileSource();
        abstract boolean isSource(Path path);
    }

    private static class MsDataFileSource extends MsDataSource
    {
        private MsDataFileSource(String instrument, String extension)
        {
            super(Collections.singletonList(instrument), Collections.singletonList(extension));
        }

        public MsDataFileSource(List<String> extensions)
        {
            super(Collections.emptyList(), extensions);
        }

        private MsDataFileSource(List<String> instrumentVendors, List<String> extensions)
        {
            super(instrumentVendors, extensions);
        }

        @Override
        boolean isFileSource()
        {
            return true;
        }
        @Override
        boolean isSource(Path path)
        {
            return path != null && isSourceName(FileUtil.getFileName(path)) && Files.exists(path) && !Files.isDirectory(path);
        }
    }

    private static abstract class MsDataDirSource extends MsDataSource
    {
        public abstract Predicate<Path> getDirContentCondition();
        public abstract SimpleFilter.FilterClause getDirContentsFilterClause();

        private MsDataDirSource(String instrumentVendor, String extension)
        {
            super(Collections.singletonList(instrumentVendor), Arrays.asList(extension, extension + EXT_ZIP));
        }

        @Override
        public boolean isFileSource()
        {
            return false;
        }

        @Override
        public boolean isSource(@NotNull Path path)
        {
            if(Files.exists(path))
            {
                String nameLc = FileUtil.getFileName(path).toLowerCase();
                if(Files.isDirectory(path))
                {
                    try
                    {
                        return isSourceName(nameLc) && matchDirContents(path);
                    }
                    catch (IOException ignored) {} // TODO: log exception??
                }
                else if(nameLc.endsWith(EXT_ZIP))
                {
                    try
                    {
                        return isSourceName(FilenameUtils.getBaseName(nameLc)) && zipMatches(path);
                    }
                    catch (IOException ignored){} // TODO: log exception??
                }
            }
            return false;
        }

        private boolean matchDirContents(@NotNull Path path) throws IOException
        {
            return Files.list(path).anyMatch(getDirContentCondition());
        }

        private boolean zipMatches(@NotNull Path path) throws IOException
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
                    String subdir = FileUtil.getBaseName(FileUtil.getFileName(path));  // Name minus the .zip
                    // Look for match in the subdirectory
                    Path subDir = Files.list(root).filter(p -> Files.isDirectory(p) && subdir.equals(FileUtil.getFileName(p))).findFirst().orElse(null);
                    if (subDir != null)
                    {
                        return matchDirContents(subDir);
                    }
                }
            }
            return false;
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testExtensionLowerCase()
        {
            String et = FileUtil.getExtension("QC_10.9.17");
            assertTrue(".17".equals(extensionLowerCase("QC_10.9.17")));
            assertTrue(".raw".equals(extensionLowerCase("QC_10.9.17.raw")));
            assertTrue(".raw".equals(extensionLowerCase("QC_10.9.17.RAW")));
            assertTrue(".raw.zip".equals(extensionLowerCase("QC_10.9.17.RAW.zip")));
            assertTrue(".d.zip".equals(extensionLowerCase("QC_10.9.17.d.zip")));
            assertTrue(".17.zip".equals(extensionLowerCase("QC_10.9.17.zip")));
        }

        @Test
        public void testGetInstrumentVendor()
        {
            MsDataSourceUtil dataSourceUtl = MsDataSourceUtil.getInstance();
            Instrument instrument = new Instrument();
            assertNull(dataSourceUtl.getSourceForInstrument(instrument));

            // These are some of the values in the "model" column of the targetedms.instrument table on PanoramaWeb
            // Specific model names are only available for Thermo and SCIEX instruments.

            instrument.setModel("Thermo Electron instrument model");
            assertEquals(THERMO, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("Orbitrap Exploris 480");
            assertEquals(THERMO, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("TSQ Altis");
            assertEquals(THERMO, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("TSQ Quantum Ultra AM");
            assertEquals(THERMO, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("ITQ 1100");
            assertEquals(THERMO, dataSourceUtl.getSourceForInstrument(instrument));


            instrument.setModel("AB SCIEX instrument model");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("SCIEX instrument model");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("Applied Biosystems instrument model");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("4000 QTRAP");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("QTRAP 5500");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("QSTAR Elite");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("TripleTOF 5600");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("TripleTOF 6600");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("Triple Quad 4500");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));
            instrument.setModel("Triple Quad 6500");
            assertEquals(SCIEX, dataSourceUtl.getSourceForInstrument(instrument));

            instrument.setModel("Bruker Daltonics maXis series");
            assertEquals(BRUKER, dataSourceUtl.getSourceForInstrument(instrument));

            instrument.setModel("Waters instrument model");
            assertEquals(WATERS, dataSourceUtl.getSourceForInstrument(instrument));

            instrument.setModel("Agilent instrument model");
            assertEquals(AGILENT, dataSourceUtl.getSourceForInstrument(instrument));

            instrument.setModel("Shimadzu instrument model");
            assertEquals(SHIMADZU, dataSourceUtl.getSourceForInstrument(instrument));
        }
    }
}
