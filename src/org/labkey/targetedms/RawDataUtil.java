package org.labkey.targetedms;

import org.apache.commons.io.FileUtils;
import org.apache.tika.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.RawDataService;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.parser.Instrument;
import org.labkey.targetedms.parser.PsiInstruments;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.query.ReplicateManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RawDataUtil implements RawDataService
{
    @Override
    public List<ISampleFile> hasData(List<ISampleFile> sampleFiles, Container container) throws IOException
    {
        List<ISampleFile> found = new ArrayList<>();
        Path rawFilesDir = getRawFilesDirPath(container);
        if(!Files.exists(rawFilesDir))
        {
            return Collections.emptyList();
        }
        for (ISampleFile s : sampleFiles)
        {
            if (hasData(s, container, rawFilesDir))
            {
                found.add(s);
            }
        }
        return found;
    }


    private boolean hasData(ISampleFile sampleFile, Container container, Path rawFilesDir) throws IOException
    {
        linkRawData(sampleFile, container, rawFilesDir);
        return sampleFile.getRawDataId() != null;
    }

    @Override
    public String getWebdavUrl(ISampleFile sampleFile, Container container) throws IOException
    {
        ExpData expData = null;
        if(sampleFile.getRawDataId() != null)
        {
            // Already linked
            expData = ExperimentService.get().getExpData(sampleFile.getRawDataId());
        }

        ExperimentService svc = ExperimentService.get();
        if(svc != null)
        {
            Path rawFilesDir = getRawFilesDirPath(container);
            if (Files.exists(rawFilesDir))
            {
                expData = getExpData(sampleFile, container, rawFilesDir, false);
            }
        }
        return expData != null ? expData.getWebDavURL(ExpData.PathType.full) : null;
    }

    public Pair<ExpData, Long> getDownloadInfo(ISampleFile sampleFile, Container container) throws IOException
    {
        ExpData expData = null;
        if(sampleFile.getRawDataId() != null)
        {
            // Already linked
            expData = ExperimentService.get().getExpData(sampleFile.getRawDataId());
        }

        ExperimentService svc = ExperimentService.get();
        if(svc != null)
        {
            Path rawFilesDir = getRawFilesDirPath(container);
            if (Files.exists(rawFilesDir))
            {
                expData = getExpData(sampleFile, container, rawFilesDir, false);
            }
        }
        Long size = null;
        if(expData != null)
        {
            Path filePath = expData.getFilePath();
            if(Files.exists(filePath))
            {
                File f = filePath.toFile();
                size = Files.isDirectory(filePath) ? FileUtils.sizeOfDirectory(f) : FileUtils.sizeOf(f);
            }

        }
        return new Pair<>(expData, size);
    }

    @Override
    public ExpData linkRawData(ISampleFile sampleFile, Container container) throws IOException
    {
        Path rawFilesDir = getRawFilesDirPath(container);
        if(Files.exists(rawFilesDir))
        {
            linkRawData(sampleFile, container, rawFilesDir);
        }
        return null;
    }



    @Nullable
    public static ExpData linkRawData(ISampleFile sampleFile, Container container, Path rawFilesDir) throws IOException
    {
        if(sampleFile.getRawDataId() != null)
        {
            // Already linked
            return ExperimentService.get().getExpData(sampleFile.getRawDataId());
        }

        String filePath = sampleFile.getFilePath();
        String fileName = FilenameUtils.getName(filePath);

        RawData dataSource = RawData.sourceFor(sampleFile.getFilePath());
        if(dataSource == null)
        {
            Instrument instrument = ReplicateManager.getInstrument(sampleFile.getInstrumentId());
            PsiInstruments.PsiInstrument psiInstrument = PsiInstruments.getInstrument(instrument.getModel());
            dataSource = RawData.getForInstrument(psiInstrument == null ? instrument.getModel() : psiInstrument.getVendor());
        }
        // First check if there is a row in ExpData
        ExperimentService svc = ExperimentService.get();
        if(svc == null)
        {
            return null;
        }
        ExpData expData = getExpData(fileName, container, rawFilesDir, svc, dataSource, false);
        if(expData != null)
        {
            if(sampleFile instanceof SampleFile)
            {
                ((SampleFile)sampleFile).setRawDataId(expData.getRowId());
                Path p = expData.getFilePath();
                if(!Files.isDirectory(p))
                {
                    ((SampleFile)sampleFile).setRawDataSize(Files.size(expData.getFilePath()));
                }
                else
                {
                    FileUtils.sizeOfDirectory(p.toFile()); // TODO: cloud??
                }
                ReplicateManager.updateSampleFile((SampleFile) sampleFile);
            }
            return expData;
        }
        else if(!FileContentService.get().isCloudRoot(container))
        {
            Path pathOnFileSystem = lookupInFileRoot(fileName, container, rawFilesDir);
            if(pathOnFileSystem != null)
            {
                Path fileRoot = FileContentService.get().getFileRootPath(container);
                PipeRoot root = PipelineService.get().findPipelineRoot(container, FileContentService.FILES_LINK);
                Path relativePath = pathOnFileSystem.relativize(fileRoot);
                String relPath = root.relativePath(pathOnFileSystem);
                // TODO: Save in ExpData if no row exists???
                // return AppProps.getInstance().getBaseServerUrl() + root.getWebdavURL() + relPath;
            }
        }

        return null;
    }

    @Nullable
    public static ExpData getExpData(ISampleFile sampleFile, Container container, Path rawFilesDir, boolean validateZip) throws IOException
    {
        if(sampleFile.getRawDataId() != null)
        {
            // Already linked
            return ExperimentService.get().getExpData(sampleFile.getRawDataId());
        }

        String filePath = sampleFile.getFilePath();
        String fileName = FilenameUtils.getName(filePath);

        RawData dataSource = RawData.sourceFor(sampleFile.getFilePath());
        if(dataSource == null)
        {
            Instrument instrument = ReplicateManager.getInstrument(sampleFile.getInstrumentId());
            PsiInstruments.PsiInstrument psiInstrument = PsiInstruments.getInstrument(instrument.getModel());
            dataSource = RawData.getForInstrument(psiInstrument == null ? instrument.getModel() : psiInstrument.getVendor());
        }

        ExperimentService svc = ExperimentService.get();
        if(svc == null)
        {
            return null;
        }
        return getExpData(fileName, container, rawFilesDir, svc, dataSource, validateZip);
//        if(expData != null)
//        {
//            if(sampleFile instanceof SampleFile)
//            {
//                ((SampleFile)sampleFile).setRawDataId(expData.getRowId());
//                Path p = expData.getFilePath();
//                if(!Files.isDirectory(p))
//                {
//                    ((SampleFile)sampleFile).setRawDataSize(Files.size(expData.getFilePath()));
//                }
//                else
//                {
//                    FileUtils.sizeOfDirectory(p.toFile()); // TODO: cloud??
//                }
//                ReplicateManager.updateSampleFile((SampleFile) sampleFile);
//            }
//            return expData;
//        }
//        else if(!FileContentService.get().isCloudRoot(container))
//        {
//            Path pathOnFileSystem = lookupInFileRoot(fileName, container, rawFilesDir);
//            if(pathOnFileSystem != null)
//            {
//                Path fileRoot = FileContentService.get().getFileRootPath(container);
//                PipeRoot root = PipelineService.get().findPipelineRoot(container, FileContentService.FILES_LINK);
//                Path relativePath = pathOnFileSystem.relativize(fileRoot);
//                String relPath = root.relativePath(pathOnFileSystem);
//                // TODO: Save in ExpData if no row exists???
//                // return AppProps.getInstance().getBaseServerUrl() + root.getWebdavURL() + relPath;
//            }
//        }

//        return null;
    }

    @Nullable
    private static ExpData getExpData(String fileName, Container container, Path rawFilesDir, ExperimentService expSvc, RawData dataType, boolean validateZip) throws IOException
    {
        String nameNoExt = FileUtil.getBaseName(fileName);
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        // Look for files that start with the base filename of the sample file.
        filter.addCondition(FieldKey.fromParts("Name"), nameNoExt, CompareType.STARTS_WITH);
        // Look for data under @files/RawFiles.
        // Use FileUtil.pathToString(); this will remove the access ID is this is a S3 path
        String prefix = FileUtil.pathToString(rawFilesDir); // Encoded URI string
        if (!prefix.endsWith("/"))
        {
            prefix = prefix + "/";
        }
        filter.addCondition(FieldKey.fromParts("datafileurl"), prefix, CompareType.STARTS_WITH);

        // Set<String> columns = Stream.of("RowId", "DataFileUrl", "Name").collect(Collectors.toSet());
        TableInfo expDataTInfo = expSvc.getTinfoData();
        Collection<Map<String, Object>> expDatas = new TableSelector(expDataTInfo,
                expDataTInfo.getColumns("RowId", "DataFileUrl", "Name"), filter, null)
                .getMapCollection();

        if(dataType != null && !dataType.isFileSource())
        {
            for (Map<String, Object> expDataInfo: expDatas)
            {
                String expDataName = (String) expDataInfo.get("Name");

                if(fileName.equals(expDataName))
                {
                    // TODO: Check in exp.data
                    String encodedUrl = (String)expDataInfo.get("DataFileUrl");
                    Path dataDirPath = FileUtil.stringToPath(container, encodedUrl);
                    if(dataType.isSource(dataDirPath))
                    {
                        return  expSvc.getExpData((Integer)expDataInfo.get("RowId"));
                    }
                }
                else if((fileName + ".zip").equalsIgnoreCase(expDataName))
                {
                    String encodedUrl = (String)expDataInfo.get("DataFileUrl");
                    Path dataDirPath = FileUtil.stringToPath(container, encodedUrl);
                    ExpData d = expSvc.getExpData((Integer)expDataInfo.get("RowId"));
                    return validateZip ? (dataType.isSource(dataDirPath) ? d : null) : d;
                }
            }
        }
        else
        {
            // File source
            for (Map<String, Object> expDataInfo: expDatas)
            {
                String expDataName = (String) expDataInfo.get("Name");
                if(fileName.equals(expDataName))
                {
                    return expSvc.getExpData((Integer)expDataInfo.get("RowId"));
                }
            }
            // return expDataList.stream().filter(e -> fileName.equals(e.getName()) || (fileName + ".zip").equalsIgnoreCase(e.getName())).findAny().orElse(null);
        }

        return null;
    }

    private static boolean verifyRequiredFiles(ExpData expData, RawData dataType) throws IOException
    {
        Path dataDirPath = Path.of(expData.getDataFileURI());
        if(dataDirPath != null && Files.exists(dataDirPath))
        {
            return dataType.isSource(dataDirPath);
        }
        return false;
    }

    private static boolean verifyRequiredFilesInZip(ExpData expData, RawData dataType)
    {
        return false;
    }

    @Nullable
    private static ExpData getExpData(String dataFileUrl, ExperimentService expSvc)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("Name"), dataFileUrl, CompareType.EQUAL);
        List<ExpData> expDataList = new TableSelector(expSvc.getTinfoData(), filter, null).getArrayList(ExpData.class);
        return expDataList.isEmpty() ? null : expDataList.get(0);
    }

    private static boolean hasExpData(String sampleFileName, Container container, Path rawFilesDir, ExperimentService svc, boolean allowBasenameOnly)
    {
        if(svc == null)
        {
            return false;
        }

        String nameNoExt = FileUtil.getBaseName(sampleFileName);

        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        // Look for files that start with the base filename of the sample file.
        filter.addCondition(FieldKey.fromParts("Name"), nameNoExt, CompareType.STARTS_WITH);
        // Look for data under @files/RawFiles.
        // Use FileUtil.pathToString(); this will remove the access ID is this is a S3 path
        String prefix = FileUtil.pathToString(rawFilesDir);
        if (!prefix.endsWith("/"))
        {
            prefix = prefix + "/";
        }
        filter.addCondition(FieldKey.fromParts("datafileurl"), prefix, CompareType.STARTS_WITH);

        List<String> files = new TableSelector(svc.getTinfoData(), Collections.singleton("Name"), filter, null).getArrayList(String.class);

        for (String expDataFile: files)
        {
            if(accept(sampleFileName, expDataFile, allowBasenameOnly))
            {
                return true;
            }
        }
        return false;
    }

    private static Path getRawFilesDirPath(Container c)
    {
        return resolveToFileRoot(c, TargetedMSService.RAW_FILES_DIR);
    }

    private static Path resolveToFileRoot(Container c, String relativePath)
    {
        FileContentService service = FileContentService.get();
        if(service != null)
        {
            Path fileRoot = service.getFileRootPath(c, FileContentService.ContentType.files);
            if (fileRoot != null)
            {
                return fileRoot.resolve(relativePath);
            }
        }
        return null;
    }

    private static Path lookupInFileRoot(String fileName, Container container, Path rawFilesDir)
    {
       try
        {
            if (rawDataExists(rawFilesDir, fileName))
            {
                return null;
            }
        }
        catch (IOException e)
        {
            // LOG.error(experimentContainer.getPath() + ": Error looking for raw data associated with Skyline documents in " + rawFilesDirPath.toString(), e);
            return null;
        }

        // Look in subdirectories
        try (Stream<Path> list = Files.walk(rawFilesDir).filter(p -> Files.isDirectory(p)))
        {
            for (Path subDir : list.collect(Collectors.toList()))
            {
                if (rawDataExists(subDir, fileName)) return null;
            }
        }
        catch (IOException e)
        {
            // LOG.error(experimentContainer + ": Error looking for raw data associated with Skyline documents in sub-directories of" + rawFilesDirPath.toString(), e);
            return null;
        }
        return null;
    }

    private static boolean rawDataExists(Path rawFilesDirPath, String fileName) throws IOException
    {
        Path rawFilePath = rawFilesDirPath.resolve(fileName);
        if(Files.exists(rawFilePath) || Files.isDirectory(rawFilePath))
        {
            return true;
        }

        // Look for zip files
        String nameNoExt = FileUtil.getBaseName(fileName);
        try (Stream<Path> list = Files.list(rawFilesDirPath).filter(p -> FileUtil.getFileName(p).startsWith(nameNoExt)))
        {
            for (Path path : list.collect(Collectors.toList()))
            {
                String name = FileUtil.getFileName(path);
                if(accept(fileName, name))
                {
                    // TODO: For directory based raw data check if required file / subdirectory also exists
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean accept(String sampleFileName, String uploadedFileName)
    {
        return accept(sampleFileName, uploadedFileName, false);
    }

    private static boolean accept(String sampleFileName, String uploadedFileName, boolean allowBasenameOnly)
    {
        // Accept QC_10.9.17.raw OR for QC_10.9.17.raw.zip OR QC_10.9.17.zip
        // 170428_DBS_cal_7a.d OR 170428_DBS_cal_7a.d.zip OR 170428_DBS_cal_7a.zip
        String nameNoExt = FileUtil.getBaseName(sampleFileName);
        return sampleFileName.equalsIgnoreCase(uploadedFileName)
                || (sampleFileName + ".zip").equalsIgnoreCase(uploadedFileName)
                // || (nameNoExt + ".zip").equalsIgnoreCase(uploadedFileName)
                || (allowBasenameOnly && nameNoExt.equalsIgnoreCase(FileUtil.getBaseName(uploadedFileName)));
    }


    public RawData getRawDataType(Instrument instrument)
    {
        PsiInstruments.PsiInstrument psiInstrument = PsiInstruments.getInstrument(instrument.getModel());
        RawData rawDataType = null;
        if(psiInstrument != null)
        {
            rawDataType = RawData.getForInstrument(psiInstrument.getVendor());
        }
        else
        {
            rawDataType = RawData.getForInstrument(instrument.getModel());
        }
        return rawDataType;
    }

    private static RawData[] sourceTypes = {new ThermoData(), new SciexData(), new ShimadzuData(), new WatersData(), new AgilentData(), new BrukerData()};
    private static abstract class RawData
    {
        public static final String EXT_THERMO_RAW = ".raw";
        public static final String EXT_WIFF = ".wiff";
        public static final String EXT_WIFF2 = ".wiff2";
        public static final String EXT_SHIMADZU_RAW = ".lcd";
        public static final String EXT_MZXML =  ".mzxml";
        public static final String EXT_MZDATA = ".mzdata";
        public static final String EXT_MZML = ".mzml";
        public static final String EXT_MZ5 = ".mz5";
        public static final String EXT_XML = ".xml";
        public static final String EXT_UIMF = ".uimf";
        public static final String EXT_WATERS_RAW = ".raw";
        public static final String EXT_AGILENT_BRUKER_RAW = ".d";

        abstract boolean isSource(@NotNull Path path) throws IOException;
        abstract boolean vendorOrModelMatches(@NotNull String name);

        boolean isFileSource()
        {
            return true;
        }

        /* name can be instrument model (from Instrument) or vendor from the PSI-MS vocabulary */
        static RawData getForInstrument(String name)
        {
            return Arrays.stream(sourceTypes).filter(rd -> rd.vendorOrModelMatches(name)).findFirst().orElse(null);
        }

        static RawData sourceFor(String fileName)
        {
            return null;
        }
    }

    private static abstract class RawDataDirectory extends RawData
    {
        abstract String getExtension();
        abstract Predicate<Path> condition();

        public boolean isSource(@NotNull Path path) throws IOException
        {
            String fileName = path.getFileName().toString();
            String fileNameLc = fileName.toLowerCase();
            if(Files.isDirectory(path))
            {
                return fileNameLc.endsWith(getExtension()) ? matches(path) : false;
            }
            else if(fileNameLc.endsWith(getExtension() + ".zip"))
            {
                for (Path root : FileSystems.newFileSystem(path, Collections.emptyMap())
                        .getRootDirectories())
                {
                    boolean fileInRoot = matches(root);
                    if(fileInRoot)
                    {
                        return true;
                    }
                    else
                    {
                        String subdir = FileUtil.getBaseName(fileName);  // Name minus the .zip
                        // Look for match in the subdirectory
                        Path subDir = Files.list(root).filter(p -> Files.isDirectory(p) && subdir.equals(FileUtil.getFileName(p))).findFirst().orElse(null);
                        if(subDir != null)
                        {
                            return matches(subDir);
                        }
                    }
                }
            }
            return false;
        }

        private boolean matches(@NotNull Path path) throws IOException
        {
            return Files.list(path).anyMatch(condition());
        }

        public boolean isFileSource()
        {
            return false;
        }
    }

    private static class ThermoData extends RawData
    {
        public boolean isSource(@NotNull Path path)
        {
            return !Files.isDirectory(path) && "raw".equals(FileUtil.getExtension(FileUtil.getFileName(path)).toLowerCase());
        }

        @Override
        boolean vendorOrModelMatches(@NotNull String name)
        {
            return name.toLowerCase().contains("thermo");
        }
    }
    private static class SciexData extends RawData
    {
        public boolean isSource(@NotNull Path path)
        {
            if(!Files.isDirectory(path))
            {
                String extension = FileUtil.getExtension(FileUtil.getFileName(path)).toLowerCase();
                return "wiff".equals(extension) || "wiff.scan".equals(extension);
            }
            return false;
        }
        @Override
        boolean vendorOrModelMatches(@NotNull String name)
        {
            String lcName = name.toLowerCase();
            return lcName.contains("sciex") || lcName.contains("applied biosystems");
        }
    }
    private static class ShimadzuData extends RawData
    {
        public boolean isSource(@NotNull Path path)
        {
            return !Files.isDirectory(path) && "lcd".equals(FileUtil.getExtension(FileUtil.getFileName(path)).toLowerCase());
        }
        @Override
        boolean vendorOrModelMatches(@NotNull String name)
        {
            return name.toLowerCase().contains("shimadzu");
        }
    }

    private static class WatersData extends RawDataDirectory
    {
        @Override
        String getExtension()
        {
            return ".raw";
        }

        @NotNull
        Predicate<Path> condition()
        {
            return f -> !Files.isDirectory(f) && FileUtil.getFileName(f).matches("^_FUNC.*\\.DAT$");
        }

        @Override
        boolean vendorOrModelMatches(@NotNull String name)
        {
            return name.toLowerCase().contains("waters");
        }
    }

    private static class AgilentData extends RawDataDirectory
    {
        @Override
        String getExtension()
        {
            return ".d";
        }

        @NotNull
        Predicate<Path> condition()
        {
            return f -> Files.isDirectory(f) && FileUtil.getFileName(f).equals("AcqData");
        }

        @Override
        boolean vendorOrModelMatches(@NotNull String name)
        {
            return name.toLowerCase().contains("agilent");
        }
    }

    private static class BrukerData extends RawDataDirectory
    {
        @Override
        String getExtension()
        {
            return ".d";
        }

        @Override
        Predicate<Path> condition()
        {
            return f -> !Files.isDirectory(f) && (FileUtil.getFileName(f).equals("analysis.baf") || FileUtil.getFileName(f).equals("analysis.tdf"));
        }

        @Override
        boolean vendorOrModelMatches(@NotNull String name)
        {
            return name.toLowerCase().contains("bruker");
        }
    }

    public static void main(String[] args) throws IOException
    {
         String path = "C:\\Users\\vsharma\\Downloads\\UofLiegeSTORI_RawFiles files";
//         String path = "C:\\Users\\vsharma\\Downloads\\UofLiege_intercache_RawFiles_files\\BaF-Ct";
//        String path = "C:\\Users\\vsharma\\Downloads\\UofLiege_intercache_RawFiles_files\\C26-Ct";
//        String path = "C:\\Users\\vsharma\\Downloads\\JBEI_indigoiodine_RawFiles files";

        Path dir = Paths.get(path);
        WatersData watersData = new WatersData();
        AgilentData agilentData = new AgilentData();
        try (Stream<Path> list = Files.list(dir))
        {
            for (Path p : list.collect(Collectors.toList()))
            {
                System.out.print(p.getFileName());
                if(!watersData.isSource(p))
                {
                    System.out.print(" NOT VALID");
                }
                System.out.println();
            }
        }
    }
}
