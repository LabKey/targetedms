/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import com.google.common.collect.Iterables;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Pair;
import org.labkey.api.util.Tuple3;
import org.labkey.api.util.UnexpectedException;
import org.labkey.targetedms.IrtPeptide;
import org.labkey.targetedms.SkylineDocImporter.IProgressStatus;
import org.labkey.targetedms.chromlib.ConnectionSource;
import org.labkey.targetedms.parser.GeneralTransition.IonType;
import org.labkey.targetedms.parser.list.ListColumn;
import org.labkey.targetedms.parser.list.ListData;
import org.labkey.targetedms.parser.proto.SkylineDocument;
import org.labkey.targetedms.parser.skyd.ChromGroupHeaderInfo;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses the .sky XML file format, building up an in-memory representation of its contents.
 * User: vsharma
 * Date: 4/2/12
 */
public class SkylineDocumentParser implements AutoCloseable
{
    public static final float DEFAULT_TOLERANCE = 0.055f;

    private static final String SETTINGS_SUMMARY = "settings_summary";
    private static final String TRANSITION_LIBRARIES = "transition_libraries";
    private static final String TRANSITION_SETTINGS = "transition_settings";
    private static final String TRANSITION_PREDICTION = "transition_prediction";
    private static final String PREDICT_COLLISION_ENERGY = "predict_collision_energy";
    private static final String OPTIMIZED_LIBRARY = "optimized_library";
    private static final String REGRESSION_CE = "regression_ce";
    private static final String PREDICT_DECLUSTERING_POTENTIAL = "predict_declustering_potential";
    private static final String TRANSITION_INSTRUMENT = "transition_instrument";
    private static final String TRANSITION_FULL_SCAN = "transition_full_scan";
    private static final String ISOTOPE_ENRICHMENTS = "isotope_enrichments";
    private static final String ATOM_PERCENT_ENRICHMENT = "atom_percent_enrichment";
    private static final String PEPTIDE_SETTINGS = "peptide_settings";
    private static final String DATA_SETTINGS = "data_settings";
    private static final String MEASURED_RESULTS = "measured_results";
    private static final String REPLICATE = "replicate";
    private static final String SAMPLE_FILE = "sample_file";
    private static final String INSTRUMENT_INFO_LIST = "instrument_info_list";
    private static final String INSTRUMENT_INFO = "instrument_info";
    private static final String PEPTIDE_LIST = "peptide_list";
    private static final String PROTEIN = "protein";
    private static final String PROTEIN_GROUP = "protein_group";
    private static final String PEPTIDE = "peptide";
    private static final String MOLECULE = "molecule";
    private static final String NOTE = "note";
    private static final String ATTRIBUTE_GROUP_ID = "attribute_group_id";
    public static final String PRECURSOR = "precursor";
    private static final String TRANSITION = "transition";
    private static final String PRECURSOR_MZ = "precursor_mz";
    private static final String ANNOTATION = "annotation";
    private static final String LIST_DATA = "list_data";
    private static final String LIST_DEF = "list_def";
    private static final String COLUMN = "column";
    private static final String PRODUCT_MZ = "product_mz";
    private static final String COLLISION_ENERGY = "collision_energy";
    private static final String DECLUSTERING_POTENTIAL = "declustering_potential";
    private static final String LOSSES = "losses";
    private static final String NEUTRAL_LOSS = "neutral_loss";
    public static final String TRANSITION_PEAK = "transition_peak";
    private static final String TRANSITION_LIB_INFO = "transition_lib_info";
    private static final String PRECURSOR_PEAK = "precursor_peak";
    private static final String PEPTIDE_RESULT = "peptide_result";
    private static final String EXPLICIT_MODIFICATION = "explicit_modification";
    private static final String EXPLICIT_STATIC_MODIFICATIONS = "explicit_static_modifications";
    private static final String EXPLICIT_HEAVY_MODIFICATIONS = "explicit_heavy_modifications";
    private static final String IMPLICIT_MODIFICATION = "implicit_modification";
    private static final String IMPLICIT_HEAVY_MODIFICATIONS = "implicit_heavy_modifications";
    private static final String IMPLICIT_STATIC_MODIFICATIONS = "implicit_static_modifications";
    private static final String CROSSLINKS = "crosslinks";
    private static final String CROSSLINK = "crosslink";
    private static final String SITE = "site";
    private static final String VARIABLE_MODIFICATION = "variable_modification";
    private static final String SEQUENCE = "sequence";
    private static final String BIBLIOSPEC_SPECTRUM_INFO = "bibliospec_spectrum_info";
    private static final String HUNTER_SPECTRUM_INFO = "hunter_spectrum_info";
    private static final String NIST_SPECTRUM_INFO = "nist_spectrum_info";
    private static final String SPECTRAST_SPECTRUM_INFO = "spectrast_spectrum_info";
    private static final String CHROMATOGRAM_LIBRARY_SPECTRUM_INFO = "chromatogram_library_spectrum_header_info";
    private static final String LIBRARY_NAME = "library_name";
    private static final String COUNT_MEASURED = "count_measured";
    private static final String SCORE = "score";
    private static final String SCORE_TYPE = "score_type";
    private static final String EXPECT = "expect";
    private static final String PROCESSED_INTENSITY =  "processed_intensity";
    private static final String TOTAL_INTENSITY = "total_intensity";
    private static final String TFRATIO = "tfratio";
    private static final String PEAK_AREA = "peak_area";
    private static final String ISOLATION_SCHEME = "isolation_scheme";
    private static final String ISOLATION_WINDOW = "isolation_window";
    private static final String ION_FORMULA = "ion_formula";
    private static final String NEUTRAL_FORMULA = "neutral_formula";
    private static final String CUSTOM_ION_NAME = "custom_ion_name";
    private static final String MASS_MONOISOTOPIC = "mass_monoisotopic"; //  Obsolete - would be most properly called mass_h_monoisotopic
    private static final String MASS_AVERAGE = "mass_average"; // Obsolete - would be most properly called mass_h_average
    private static final String NEUTRAL_MASS_MONOISOTOPIC = "neutral_mass_monoisotopic";
    private static final String NEUTRAL_MASS_AVERAGE = "neutral_mass_average";
    private static final String GROUP_COMPARISON = "group_comparison";
    private static final String CHARGE = "charge" ;
    public static final String TRANSITION_DATA = "transition_data";
    private static final String LINKED_FRAGMENT_ION = "linked_fragment_ion";
    private static final String SPECTRUM_FILTER = "spectrum_filter";

    private static final double MIN_SUPPORTED_VERSION = 1.2;
    public static final double MAX_SUPPORTED_VERSION = 23.1;

    private static final Pattern XML_ID_REGEX = Pattern.compile("\"/^[:_A-Za-z][-.:_A-Za-z0-9]*$/\"");
    private static final String XML_ID_FIRST_CHARS = ":_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String XML_ID_FOLLOW_CHARS = "-.:_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String XML_NON_ID_SEPARATOR_CHARS = ";[]{}()!|\\/\"'<>";
    private static final String XML_NON_ID_PUNCTUATION_CHARS = ",?";

    private static final String ID = "id";

    public static final Pattern oldModMassPattern = Pattern.compile("(\\[[+-]\\d+)]"); // e.g. KVN[-17]KTES[+80]K will match [-17] and [+80]

    private final static double OPTIMIZE_SHIFT_SIZE = 0.01;

    private int _peptideGroupCount;
    private int _peptideCount;
    private int _smallMoleculeCount;
    private int _precursorCount;
    private int _transitionCount;
    private int _replicateCount;
    private int _listCount;

    /** Tally the transition/replicate combinations so that we can avoid storing huge DIA-type runs in the DB */
    private int _transitionChromInfoCount;

    /** Null if we haven't found a SKYD to parse */
    @Nullable
    private SkylineBinaryParser _binaryParser;

    private TransitionSettings _transitionSettings;
    private PeptideSettings _peptideSettings;
    private DataSettings _dataSettings;
    private List<SkylineReplicate> _replicateList;
    private Map<String, String> _sampleFileIdToFilePathMap;
    // Map of replicate names and sample file Ids ('id' attribute of <sample_file> element).
    // An entry is added to this map only if a replicate contains a single sample file.
    // This is used to lookup the sample file for chrom info elements (<peptide_result>, <precursor_peak> and <transition_peak>)
    // that do not have the "file" attribute.  The "file" attribute is missing only if the replicate has a single
    // sample file.
    private Map<String, String> _replicateSampleFileIdMap;
    private final List<IrtPeptide> _iRTScaleSettings = new ArrayList<>();

    private final List<OptimizationDBRow> _optimizationDBRows = new LinkedList<>();

    private double _matchTolerance = DEFAULT_TOLERANCE;

    private final XMLStreamReader _reader;
    private final ProgressInputStream _inputStream;
    private final File _file;
    private final Container _container;
    private final Logger _log;

    private String _formatVersion;
    private String _softwareVersion;
    private GUID _documentGUID;

    private final long _fileSize;
    private final IProgressStatus _progressStatus;

    private final Map<String, AtomicInteger> _missingChromatograms = new HashMap<>();

    public SkylineDocumentParser(File file, Logger log, Container container, IProgressStatus progressStatus) throws XMLStreamException, IOException
    {
        _file = file;
        _fileSize = file.length();
        _progressStatus = progressStatus;
        _container = container;
        _inputStream = new ProgressInputStream(new FileInputStream(_file));
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        _reader = inputFactory.createXMLStreamReader(_inputStream);
        _log = log;
        readDocumentVersion(_reader);
    }

    @Override
    public void close()
    {
        if (_reader != null) try
        {
            _reader.close();
        }
        catch (XMLStreamException e)
        {
            _log.error(e);
        }
        if(_inputStream != null) try
        {
            _inputStream.close();
        }
        catch(IOException e)
        {
            _log.error(e);
        }
        if (_binaryParser != null)
        {
            _binaryParser.close();
        }
    }

    /** @return the data object for the .skyd file, if available */
    @Nullable
    public ExpData readSettings(@NotNull Container container, @NotNull User user) throws XMLStreamException, IOException
    {
        _replicateList = new ArrayList<>();
        _sampleFileIdToFilePathMap = new HashMap<>();
        _replicateSampleFileIdMap = new HashMap<>();

        readDocumentSettings(_reader);
        updateProgress();

        parseiRTFile();
        parseOptDbFiles();
        return parseChromatograms(container, user);
    }

    @SuppressWarnings("SqlResolve")
    private void parseOptDbFiles()
    {
        for (String optDbPath : _transitionSettings.getPredictionSettings().getOptimizedLibraries().values())
        {
            // FileNameUtils.getName() will handle a file path in either Unix or Windows format.
            String optDbFileName = FilenameUtils.getName(optDbPath);
            File optDbFile = new File(_file.getParent(), optDbFileName);
            if (! optDbFile.exists() ) {
                _log.warn("Input OPTDB database does not exist " + optDbFileName);
            }
            else
            {
                try
                {
                    try (ConnectionSource cs = new ConnectionSource(optDbFile.getAbsolutePath());
                         Connection conn = cs.getConnection())
                    {
                        int schemaVersion;
                        try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM VersionInfo"))
                        {
                            if (rs.next())
                            {
                                schemaVersion = rs.getInt("SchemaVersion");
                                if (schemaVersion > 3)
                                {
                                    _log.warn("Unsupported OPTDB version: " + schemaVersion + " in OPTDB file " + optDbFile + ", attempting to continue");
                                }
                            }
                            else
                            {
                                throw new IllegalStateException("Could not find version info from " + optDbFile);
                            }
                        }
                        try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM OptimizationLibrary"))
                        {
                            while (rs.next())
                            {
                                OptimizationDBRow info = new OptimizationDBRow();
                                info.setPeptideModSeq(rs.getString("PeptideModSeq"));
                                info.setCharge(rs.getString("Charge"));
                                info.setFragmentIon(rs.getString("FragmentIon"));
                                info.setProductCharge(rs.getString("ProductCharge"));
                                info.setValue(rs.getDouble("Value"));
                                info.setType(switch (rs.getInt("Type"))
                                {
                                    case 1 -> "ce";
                                    case 2 -> "dp";
                                    case 3 -> "rcv";
                                    case 4 -> "mcv";
                                    case 5 -> "hcv";
                                    default -> throw new IllegalArgumentException("Unrecognized optimization type " + rs.getInt("Type") + " in " + optDbFileName);
                                });

                                _optimizationDBRows.add(info);
                            }
                        }
                    }
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }

        }
    }

    private void parseiRTFile()
    {
        PeptideSettings.RetentionTimePredictionSettings rtPredictionSettings = _peptideSettings.getPeptidePredictionSettings().getRtPredictionSettings();
        if(rtPredictionSettings == null)
            return;

        String irtDatabasePath = rtPredictionSettings.getIrtDatabasePath();
        if (null != irtDatabasePath)
        {
            // FileNameUtils.getName() will handle a file path in either Unix or Windows format.
            String iRTFileName = FilenameUtils.getName(irtDatabasePath);
            File iRTFile = new File(_file.getParent(), iRTFileName);
            if (! iRTFile.exists() ) {
                _log.warn("Input iRT database does not exist " + iRTFileName);
            }
            else
            {
                try
                {
                    @SuppressWarnings("SqlResolve")
                    String sql = "SELECT * FROM IrtLibrary";
                    try (ConnectionSource cs = new ConnectionSource(iRTFile.getAbsolutePath());
                         Connection conn = cs.getConnection();
                         ResultSet rs = conn.createStatement().executeQuery(sql))
                    {
                        while (rs.next())
                        {
                            IrtPeptide iRTPeptideRow = new IrtPeptide();
                            iRTPeptideRow.setModifiedSequence(rs.getString("PeptideModSeq"));
                            iRTPeptideRow.setiRTStandard(rs.getBoolean("Standard"));
                            iRTPeptideRow.setiRTValue(rs.getDouble("Irt"));
                            iRTPeptideRow.setImportCount(1);
                            iRTPeptideRow.setTimeSource(rs.getInt("TimeSource"));
                            _iRTScaleSettings.add(iRTPeptideRow);
                        }
                    }
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
            }
        }
    }

    /** @return the data object for the .skyd file, if available */
    @Nullable
    private ExpData parseChromatograms(@NotNull Container container, @NotNull User user) throws IOException
    {
        // Just add a "d" based on the expected file extension
        File skydFile = new File(_file.getPath() + "d");
        if (NetworkDrive.exists(skydFile))
        {
            _binaryParser = new SkylineBinaryParser(skydFile, _log);
            _binaryParser.parse();
            ExpData result = ExperimentService.get().getExpDataByURL(skydFile, container);
            if (result == null)
            {
                result = ExperimentService.get().createData(container, SkylineBinaryParser.DATA_TYPE);
                result.setName(skydFile.getName());
                result.setDataFileURI(skydFile.toURI());
                result.save(user);
            }
            return result;
        }
        else
        {
            _log.warn("Unable to find file " + skydFile + ", unable to import chromatograms");
        }
        return null;
    }

    public List<IrtPeptide> getiRTScaleSettings()
    {
        return Collections.unmodifiableList(_iRTScaleSettings);
    }

    public List<SkylineReplicate> getReplicates()
    {
        return _replicateList;
    }

    public TransitionSettings getTransitionSettings()
    {
        return _transitionSettings;
    }

    public PeptideSettings getPeptideSettings()
    {
        return _peptideSettings;
    }

    public DataSettings getDataSettings()
    {
        return _dataSettings;
    }

    private void readDocumentVersion(XMLStreamReader reader) throws XMLStreamException
    {
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isStartElement(reader, evtType, "srm_settings")) {

                double version = XmlUtil.readRequiredDoubleAttribute(reader, "format_version", "srm_settings");
                if(version < MIN_SUPPORTED_VERSION)
                {
                    throw new IllegalStateException("The version of this Skyline document is " +
                                                    version +
                                                    ". Version less than " + MIN_SUPPORTED_VERSION +
                                                    " is not supported.");
                }
                else if(version > MAX_SUPPORTED_VERSION)
                {
                    // We will log a warning but continue with the import.
                    _log.warn("The version of this Skyline document is " + version +
                              ". This is newer than the highest supported version " + MAX_SUPPORTED_VERSION);
                }

                _formatVersion = String.valueOf(version);
                _softwareVersion = reader.getAttributeValue(null, "software_version");
                return;
            }
        }

        throw new IllegalStateException("Not a valid Skyline document. <srm_settings> element was not found.");
    }

    public String getFormatVersion()
    {
        return _formatVersion;
    }

    public String getSoftwareVersion()
    {
        return _softwareVersion;
    }

    public GUID getDocumentGUID()
    {
        return _documentGUID;
    }

    private void readDocumentSettings(XMLStreamReader reader) throws XMLStreamException
    {
        _dataSettings = new DataSettings();
         while(reader.hasNext())
         {
             int evtType = reader.next();
             if(XmlUtil.isEndElement(reader, evtType, SETTINGS_SUMMARY))
             {
                 break;
             }

             if(evtType == XMLStreamReader.START_ELEMENT)
             {
                 if(PEPTIDE_SETTINGS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     PeptideSettingsParser pepSettingsParser = new PeptideSettingsParser();
                     _peptideSettings = pepSettingsParser.parse(reader, FileUtil.getBaseName(_file));
                 }
                 else if(TRANSITION_SETTINGS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     readTransitionSettings(reader);
                 }
                 else if(DATA_SETTINGS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     readDataSettings(reader);
                 }
                 else if(MEASURED_RESULTS.equalsIgnoreCase(reader.getLocalName()))
                 {
                     readMeasuredResults(reader);
                 }
             }
         }

         // Update the boolean type annotations for replicates. We do this after reading both the <data_settings> and
         // <measured_results> elements since older files have <data_settings> after <measured_results>
         updateReplicateAnnotations();
    }

    private void updateReplicateAnnotations()
    {
        if(_replicateList == null || _replicateList.isEmpty())
            return;
        for(SkylineReplicate replicate: _replicateList) {

            List<ReplicateAnnotation> annotations = removeUnusedAnnotations(replicate);

            for(ReplicateAnnotation annot: annotations)
            {
                if(_dataSettings.isBooleanAnnotation(annot.getName()))
                {
                    // If we are reading an older file, <measured_results> were read before <data_settings>
                    // so we did not have annotation definitions while reading the replicate annotations.
                    // The value of boolean annotations is the same as the name of the annotation in .sky files.
                    // We need to change it to "true".
                    annot.setValue(Boolean.TRUE.toString());
                }
            }

            // Boolean type annotations are not listed in the .sky file if their value was false.
            // We would still like to store them in the database.
            List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations, DataSettings.AnnotationTarget.replicate);
            List<ReplicateAnnotation> missingReplAnnotations = new ArrayList<>(missingBooleanAnnotations.size());
            for(String missingAnotName: missingBooleanAnnotations)
            {
                addMissingBooleanAnnotation(missingReplAnnotations, missingAnotName, new ReplicateAnnotation());
            }

            if(!missingReplAnnotations.isEmpty())
            {
                List<ReplicateAnnotation> combinedAnnotations = new ArrayList<>(annotations);
                combinedAnnotations.addAll(missingReplAnnotations);
                replicate.setAnnotations(combinedAnnotations);
            }
        }
    }

    private List<ReplicateAnnotation> removeUnusedAnnotations(SkylineReplicate replicate)
    {
        // 04/30/14 Skyline writes replicate annotation values for annotations
        // that have been deleted/unchecked from annotation settings.
        // Do not save unused / deleted annotations.
        List<ReplicateAnnotation> annotations = replicate.getAnnotations();
        List<ReplicateAnnotation> toReturn = new ArrayList<>();
        for(ReplicateAnnotation annotation: annotations)
        {
            if(_dataSettings.annotationExists(annotation.getName()))
            {
                toReturn.add(annotation);
            }
        }
        replicate.setAnnotations(toReturn);
        return toReturn;
    }

    private void readDataSettings(XMLStreamReader reader) throws XMLStreamException
    {
        String documentGUID = reader.getAttributeValue(null, "document_guid");
        if (documentGUID != null)
        {
            _documentGUID = new GUID(documentGUID);
        }

         while(reader.hasNext())
         {
             int evtType = reader.next();
             if(XmlUtil.isEndElement(reader, evtType, DATA_SETTINGS))
             {
                 break;
             }

             if(XmlUtil.isStartElement(reader, evtType, LIST_DATA))
             {
                 // For now just skip over list definitions
                 _dataSettings.addListData(readListData(reader));
             }
             if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
             {
                 String name = XmlUtil.readRequiredAttribute(reader, "name", ANNOTATION);
                 String targets = XmlUtil.readRequiredAttribute(reader, "targets", ANNOTATION);
                 String type = XmlUtil.readRequiredAttribute(reader, "type", ANNOTATION);
                 String lookup = XmlUtil.readAttribute(reader, "lookup", ANNOTATION);
                 _dataSettings.addAnnotations(name, targets, type, lookup);
             }
             else if (XmlUtil.isStartElement(reader, evtType, GROUP_COMPARISON))
             {
                 GroupComparisonSettings groupComparison = new GroupComparisonSettings();
                 groupComparison.setName(XmlUtil.readAttribute(reader, "name"));
                 groupComparison.setControlAnnotation(XmlUtil.readAttribute(reader, "control_annotation"));
                 groupComparison.setControlValue(XmlUtil.readAttribute(reader, "control_value"));
                 groupComparison.setCaseValue(XmlUtil.readAttribute(reader, "case_value"));
                 groupComparison.setIdentityAnnotation(XmlUtil.readAttribute(reader, "identity_annotation"));
                 groupComparison.setNormalizationMethod(XmlUtil.readAttribute(reader, "normalization_method"));
                 groupComparison.setPerProtein(XmlUtil.readBooleanAttribute(reader, "per_protein", false));
                 groupComparison.setAvgTechReplicates(XmlUtil.readBooleanAttribute(reader, "avg_tech_replicates"));
                 groupComparison.setSumTransitions(XmlUtil.readBooleanAttribute(reader, "sum_transitions"));
                 groupComparison.setIncludeInteractionTransitions(XmlUtil.readBooleanAttribute(reader, "include_interaction_transitions"));
                 groupComparison.setSummarizationMethod(XmlUtil.readAttribute(reader, "summarization_method"));
                 Double confidenceLevel = XmlUtil.readDoubleAttribute(reader, "confidence_level");
                 if (null != confidenceLevel)
                 {
                     groupComparison.setConfidenceLevel(confidenceLevel / 100.0);
                 }
                 _dataSettings.addGroupComparison(groupComparison);
             }
         }
    }

    private ListData readListData(XMLStreamReader reader) throws XMLStreamException
    {
        ListData listData = new ListData();
        while (reader.hasNext()) {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, LIST_DATA)) {
                break;
            }
            if (XmlUtil.isStartElement(reader, evtType, LIST_DEF))
            {
                readListDefinition(reader, listData);
            }
            if (XmlUtil.isStartElement(reader, evtType, COLUMN))
            {
                String columnText = reader.getElementText();
                ListColumn listColumn = listData.getColumnDef(listData.getColumnDatas().size());
                listData.addColumnData(makeColumnData(DataSettings.AnnotationType.fromString(listColumn.getAnnotationType()), columnText));
            }
        }
        return listData;
    }

    private List<Object> makeColumnData(DataSettings.AnnotationType annotationType, String persistedString)
    {
        try (TabLoader tabLoader = new TabLoader(persistedString, false))
        {
            tabLoader.parseAsCSV();
            String[][] lines = tabLoader.getFirstNLines(1);
            if (lines.length == 0)
            {
                return Collections.emptyList();
            }
            List<String> values = Arrays.asList(lines[0]);

            return switch (annotationType)
            {
                case true_false ->
                        values.stream().map(v -> "1".equals(v) ? Boolean.TRUE : Boolean.FALSE).collect(Collectors.toList());
                case number ->
                        values.stream().map(v -> StringUtils.isEmpty(v) ? null : Double.parseDouble(v)).collect(Collectors.toList());
                default -> values.stream().map(v -> StringUtils.isEmpty(v) ? null : v).collect(Collectors.toList());
            };
        }
        catch (IOException ioException)
        {
            throw UnexpectedException.wrap(ioException);
        }
    }

    private void readListDefinition(XMLStreamReader reader, ListData listData) throws XMLStreamException
    {
        _listCount++;
        listData.getListDefinition().setName(XmlUtil.readAttribute(reader, "name"));
        String idColumnName = XmlUtil.readAttribute(reader, "id_property");
        String displayColumnName = XmlUtil.readAttribute(reader, "display_property");
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, LIST_DEF)) {
                break;
            }
            if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                ListColumn listColumn = new ListColumn();
                listColumn.setAnnotationType(XmlUtil.readRequiredAttribute(reader, "type", ANNOTATION));
                listColumn.setColumnIndex(listData.getColumnCount());
                listColumn.setLookup(XmlUtil.readAttribute(reader, "lookup"));
                listColumn.setName(XmlUtil.readRequiredAttribute(reader, "name", ANNOTATION));
                listData.addColumnDefinition(listColumn);
            }
        }
        if (idColumnName != null)
        {
            listData.getListDefinition().setPkColumnIndex(Iterables.indexOf(
                    listData.getColumnDefinitions(), col->idColumnName.equals(col.getName())));
        }
        if (displayColumnName != null)
        {
            listData.getListDefinition().setDisplayColumnIndex(Iterables.indexOf(
                    listData.getColumnDefinitions(), col->displayColumnName.equals(col.getName())));
        }
    }

    private void readTransitionSettings(XMLStreamReader reader) throws XMLStreamException
    {
        _transitionSettings = new TransitionSettings();
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, TRANSITION_SETTINGS))
            {
                break;
            }
            if (XmlUtil.isStartElement(reader, evtType, TRANSITION_LIBRARIES))
            {
                _peptideSettings.getLibrarySettings().setIonMatchTolerance(XmlUtil.readDoubleAttribute(reader, "ion_match_tolerance"));
            }

            if (XmlUtil.isStartElement(reader, evtType, TRANSITION_PREDICTION))
            {
                _transitionSettings.setPredictionSettings(readTransitionPrediction(reader));
            }
            if (XmlUtil.isStartElement(reader, evtType, TRANSITION_FULL_SCAN))
            {
                _transitionSettings.setFullScanSettings(readFullScanSettings(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION_INSTRUMENT))
            {
                TransitionSettings.InstrumentSettings instrumentSettings = new TransitionSettings.InstrumentSettings();
                _transitionSettings.setInstrumentSettings(instrumentSettings);
                instrumentSettings.setDynamicMin(Boolean.parseBoolean(reader.getAttributeValue(null, "dynamic_min")));
                instrumentSettings.setMinMz(XmlUtil.readRequiredIntegerAttribute(reader, "min_mz", TRANSITION_INSTRUMENT));
                instrumentSettings.setMaxMz(XmlUtil.readRequiredIntegerAttribute(reader, "max_mz", TRANSITION_INSTRUMENT));
                _matchTolerance = XmlUtil.readRequiredDoubleAttribute(reader, "mz_match_tolerance", TRANSITION_INSTRUMENT);
                instrumentSettings.setMzMatchTolerance(_matchTolerance);
                instrumentSettings.setMinTime(XmlUtil.readIntegerAttribute(reader, "min_time"));
                instrumentSettings.setMaxTime(XmlUtil.readIntegerAttribute(reader, "max_time"));
                instrumentSettings.setMaxTransitions(XmlUtil.readIntegerAttribute(reader, "max_transitions"));
            }
        }
    }

    private TransitionSettings.FullScanSettings readFullScanSettings(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionSettings.FullScanSettings result = new TransitionSettings.FullScanSettings();
        result.setPrecursorIsotopes(reader.getAttributeValue(null, "precursor_isotopes"));
        result.setPrecursorIsotopeFilter(XmlUtil.readDoubleAttribute(reader, "precursor_isotope_filter"));
        result.setPrecursorRes(XmlUtil.readDoubleAttribute(reader, "precursor_res"));
        result.setPrecursorResMz(XmlUtil.readDoubleAttribute(reader, "precursor_res_mz")); // Guessed at attribute value name
        result.setPrecursorMassAnalyzer(reader.getAttributeValue(null, "precursor_mass_analyzer"));

        result.setPrecursorFilter(XmlUtil.readDoubleAttribute(reader, "precursor_filter")); // Guessed at attribute value name
        result.setPrecursorLeftFilter(XmlUtil.readDoubleAttribute(reader, "precursor_left_filter")); // Guessed at attribute value name
        result.setPrecursorRightFilter(XmlUtil.readDoubleAttribute(reader, "precursor_right_filter")); // Guessed at attribute value name
        result.setProductRes(XmlUtil.readDoubleAttribute(reader, "product_res"));
        result.setProductResMz(XmlUtil.readDoubleAttribute(reader, "product_res_mz"));
        result.setProductMassAnalyzer(reader.getAttributeValue(null, "product_mass_analyzer")); // Guessed at attribute value name

        result.setAcquisitionMethod(XmlUtil.readAttribute(reader, "acquisition_method"));
        result.setRetentionTimeFilterType(XmlUtil.readAttribute(reader, "retention_time_filter_type"));
        result.setRetentionTimeFilterLength(XmlUtil.readDoubleAttribute(reader, "retention_time_filter_length"));

        List<TransitionSettings.IsotopeEnrichment> enrichments = new ArrayList<>();
        result.setIsotopeEnrichmentList(enrichments);

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, TRANSITION_FULL_SCAN))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, ISOTOPE_ENRICHMENTS))
            {
                enrichments.addAll(readIsotopeEnrichments(reader));
            }

            else if (XmlUtil.isStartElement(reader, evtType, ISOLATION_SCHEME))
            {
                result.setIsolationScheme(readIsolationScheme(reader));
            }
        }

        return result;
    }

    private TransitionSettings.IsolationScheme readIsolationScheme(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionSettings.IsolationScheme iScheme = new TransitionSettings.IsolationScheme();
        iScheme.setName(XmlUtil.readRequiredAttribute(reader, "name", ISOLATION_SCHEME));
        iScheme.setPrecursorFilter(XmlUtil.readDoubleAttribute(reader, "precursor_filter"));
        iScheme.setPrecursorLeftFilter(XmlUtil.readDoubleAttribute(reader, "precursor_left_filter"));
        iScheme.setPrecursorRightFilter(XmlUtil.readDoubleAttribute(reader, "precursor_right_filter"));
        iScheme.setSpecialHandling(XmlUtil.readAttribute(reader, "special_handling"));
        iScheme.setWindowsPerScan(XmlUtil.readIntegerAttribute(reader, "windows_per_scan"));


        List<TransitionSettings.IsolationWindow> iWindows = new ArrayList<>();
        iScheme.setIsolationWindowList(iWindows);
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, ISOLATION_SCHEME))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, ISOLATION_WINDOW))
            {
                iWindows.add(readIsolationWindow(reader));
            }
        }
        return iScheme;
    }

    private TransitionSettings.IsolationWindow readIsolationWindow(XMLStreamReader reader)
    {
        TransitionSettings.IsolationWindow iWindow = new TransitionSettings.IsolationWindow();
        iWindow.setWindowStart(XmlUtil.readRequiredDoubleAttribute(reader, "start", ISOLATION_WINDOW));
        iWindow.setWindowEnd(XmlUtil.readRequiredDoubleAttribute(reader, "end", ISOLATION_WINDOW));
        iWindow.setTarget(XmlUtil.readDoubleAttribute(reader, "target"));
        iWindow.setMarginLeft(XmlUtil.readDoubleAttribute(reader, "margin_left"));
        iWindow.setMarginRight(XmlUtil.readDoubleAttribute(reader, "margin_right"));
        iWindow.setMargin(XmlUtil.readDoubleAttribute(reader, "margin"));
        return iWindow;
    }

    private List<TransitionSettings.IsotopeEnrichment> readIsotopeEnrichments(XMLStreamReader reader) throws XMLStreamException
    {
        String name = reader.getAttributeValue(null, "name");

        List<TransitionSettings.IsotopeEnrichment> result = new ArrayList<>();
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, ISOTOPE_ENRICHMENTS))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, ATOM_PERCENT_ENRICHMENT))
            {
                TransitionSettings.IsotopeEnrichment enrichment = readAtomPercentEnrichment(reader);
                enrichment.setName(name);
                result.add(enrichment);
            }
        }
        return result;
    }

    private TransitionSettings.IsotopeEnrichment readAtomPercentEnrichment(XMLStreamReader reader) throws XMLStreamException
    {
        StringBuilder text = new StringBuilder();
        TransitionSettings.IsotopeEnrichment result = new TransitionSettings.IsotopeEnrichment();
        result.setSymbol(reader.getAttributeValue(null, "symbol"));
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, ATOM_PERCENT_ENRICHMENT))
            {
                break;
            }

            if (XmlUtil.isText(evtType))
            {
                text.append(reader.getText());
            }
        }

        if (text.isEmpty())
        {
            throw new XMLStreamException("No text content for <" + ATOM_PERCENT_ENRICHMENT + "> element, should contain a percent value");
        }
        result.setPercentEnrichment(Double.parseDouble(text.toString()));
        return result;
    }

    private TransitionSettings.PredictionSettings readTransitionPrediction(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionSettings.PredictionSettings settings = new TransitionSettings.PredictionSettings();
        settings.setPrecursorMassType(reader.getAttributeValue(null, "precursor_mass_type"));
        settings.setProductMassType(reader.getAttributeValue(null, "fragment_mass_type"));
        settings.setOptimizeBy(reader.getAttributeValue(null, "optimize_by"));

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, TRANSITION_PREDICTION))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, PREDICT_COLLISION_ENERGY))
            {
                settings.setCePredictor(readPredictor(reader, PREDICT_COLLISION_ENERGY));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PREDICT_DECLUSTERING_POTENTIAL))
            {
                settings.setDpPredictor(readPredictor(reader, PREDICT_DECLUSTERING_POTENTIAL));
            }
            else if (XmlUtil.isStartElement(reader, evtType, OPTIMIZED_LIBRARY))
            {
                settings.addOptimizedLibrary(reader.getAttributeValue(null, "name"), reader.getAttributeValue(null, "database_path"));
            }
        }
        return settings;
    }

    private TransitionSettings.Predictor readPredictor(XMLStreamReader reader, String endElementName) throws XMLStreamException
    {
        TransitionSettings.Predictor predictor = new TransitionSettings.Predictor();
        predictor.setName(reader.getAttributeValue(null, "name"));
        String stepSize = reader.getAttributeValue(null, "step_size");
        if (stepSize != null)
        {
            predictor.setStepSize(Double.parseDouble(stepSize));
        }
        String stepCount = reader.getAttributeValue(null, "step_count");
        if (stepCount != null)
        {
            predictor.setStepCount(Integer.parseInt(stepCount));
        }
        List<TransitionSettings.PredictorSettings> allSettings = new ArrayList<>();
        predictor.setSettings(allSettings);

        String slope = reader.getAttributeValue(null, "slope");
        String intercept = reader.getAttributeValue(null, "intercept");

        if (slope != null && intercept != null)
        {
            TransitionSettings.PredictorSettings predictorSettings = new TransitionSettings.PredictorSettings();
            predictorSettings.setSlope(Double.parseDouble(slope));
            predictorSettings.setIntercept(Double.parseDouble(intercept));
            allSettings.add(predictorSettings);
        }

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, endElementName))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, REGRESSION_CE))
            {
                TransitionSettings.PredictorSettings settings = new TransitionSettings.PredictorSettings();
                settings.setCharge(Integer.parseInt(reader.getAttributeValue(null, "charge")));
                slope = reader.getAttributeValue(null, "slope");
                if (slope != null)
                {
                    settings.setSlope(Double.parseDouble(slope));
                }
                intercept = reader.getAttributeValue(null, "intercept");
                if (intercept != null)
                {
                    settings.setIntercept(Double.parseDouble(intercept));
                }
                allSettings.add(settings);
            }
        }

        return predictor;
    }

    private void readMeasuredResults(XMLStreamReader reader) throws XMLStreamException
    {
         _replicateList = new ArrayList<>();

         while(reader.hasNext()) {

             int evtType = _reader.next();
             if(XmlUtil.isEndElement(reader, evtType, MEASURED_RESULTS))
             {
                 break;
             }
             if(XmlUtil.isStartElement(reader, evtType, REPLICATE))
             {
                 _replicateList.add(readReplicate(reader));
             }
         }
    }

    private SkylineReplicate readReplicate(XMLStreamReader reader) throws XMLStreamException
    {
        SkylineReplicate replicate = new SkylineReplicate();
        replicate.setName(XmlUtil.readRequiredAttribute(reader, "name", REPLICATE));
        replicate.setSampleType(XmlUtil.readAttribute(reader, "sample_type"));
        replicate.setAnalyteConcentration(XmlUtil.readDoubleAttribute(reader, "analyte_concentration"));
        replicate.setSampleDilutionFactor(XmlUtil.readDoubleAttribute(reader, "sample_dilution_factor"));
        replicate.setHasMidasSpectra(XmlUtil.readBooleanAttribute(reader, "has_midas_spectra"));
        replicate.setBatchName(XmlUtil.readAttribute(reader, "batch_name"));

        List<SampleFile> sampleFileList = new ArrayList<>();
        replicate.setSampleFileList(sampleFileList);

        List<ReplicateAnnotation> annotations = new ArrayList<>();
        replicate.setAnnotations(annotations);

        while(reader.hasNext()) {

            int evtType = _reader.next();

            if (XmlUtil.isEndElement(reader, evtType, REPLICATE))
            {
                break;
            }
            if (XmlUtil.isStartElement(reader, evtType, SAMPLE_FILE))
            {
                SampleFile sampleFile = readSampleFile(reader);
                if (sampleFile.getSkylineId() == null)
                {
                    // This should only happen for older Skyline documents.
                    // CONSIDER: Throw an exception since we are enforcing a minimum version for documents that
                    //           that should always have the 'id' attribute for sample files.
                    sampleFile.setSkylineId(getSkylineXmlSampleFileId(replicate.getName()));
                }
                _sampleFileIdToFilePathMap.put(sampleFile.getSkylineId(), sampleFile.getFilePath());
                sampleFileList.add(sampleFile);
            }
            else if(XmlUtil.isStartElement(reader, evtType, PREDICT_COLLISION_ENERGY))
            {
                TransitionSettings.Predictor predictor = readPredictor(reader, PREDICT_COLLISION_ENERGY);
                replicate.setCePredictor(predictor);
            }
            else if(XmlUtil.isStartElement(reader, evtType, PREDICT_DECLUSTERING_POTENTIAL))
            {
                TransitionSettings.Predictor  predictor = readPredictor(reader, PREDICT_DECLUSTERING_POTENTIAL);
                replicate.setDpPredictor(predictor);
            }
            else if(XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new ReplicateAnnotation()));
            }

            if(sampleFileList.size() == 1)
            {
                // Only for replicates with single sample files, store the id given by Skyline to the sample file.
                _replicateSampleFileIdMap.put(replicate.getName(), sampleFileList.get(0).getSkylineId());
            }
        }

        _replicateCount++;
        return replicate;
    }

    private String getSkylineXmlSampleFileId(String replicateName)
    {
        if(StringUtils.isBlank(replicateName))
        {
            throw new IllegalStateException("Replicate name cannot be empty.");
        }

        if(XML_ID_REGEX.matcher(replicateName).matches())
        {
            return replicateName+"_f0";
        }

        StringBuilder validXmlIdSb = new StringBuilder();
        int i = 0;
        if(XML_ID_FIRST_CHARS.indexOf(replicateName.charAt(i)) != -1)
        {
            validXmlIdSb.append(replicateName.charAt(i));
        }
        else
        {
            validXmlIdSb.append("_");
            // If the first character is not allowable, advance past it.
            // Otherwise, keep it in the ID.
            if (XML_ID_FOLLOW_CHARS.indexOf(replicateName.charAt(i)) == -1)
            {
                i++;
            }
        }

        for (; i < replicateName.length(); i++)
        {
            char c = replicateName.charAt(i);
            if (XML_ID_FOLLOW_CHARS.indexOf(c) != -1)
                validXmlIdSb.append(c);
            else if (Character.isSpaceChar(c))
                validXmlIdSb.append('_');
            else if (XML_NON_ID_SEPARATOR_CHARS.indexOf(c) != -1)
                validXmlIdSb.append(':');
            else if (XML_NON_ID_PUNCTUATION_CHARS.indexOf(c) != -1)
                validXmlIdSb.append('.');
            else
                validXmlIdSb.append('-');
        }
        validXmlIdSb.append("_f0");
        return validXmlIdSb.toString();
    }

    private SampleFile readSampleFile(XMLStreamReader reader) throws XMLStreamException
    {
        SampleFile sampleFile = new SampleFile();
        sampleFile.setSkylineId(XmlUtil.readRequiredAttribute(reader, "id", SAMPLE_FILE));
        sampleFile.setFilePath(XmlUtil.readRequiredAttribute(reader, "file_path", SAMPLE_FILE));
        sampleFile.setSampleName(XmlUtil.readAttribute(reader, "sample_name", SAMPLE_FILE));

        sampleFile.setAcquiredTime(XmlUtil.readDateAttribute(reader, "acquired_time"));
        sampleFile.setModifiedTime(XmlUtil.readDateAttribute(reader, "modified_time"));
        sampleFile.setTicArea(XmlUtil.readDoubleAttribute(reader,"tic_area"));
        sampleFile.setSampleId(XmlUtil.readAttribute(reader,"sample_id"));
        sampleFile.setInstrumentSerialNumber(XmlUtil.readAttribute(reader,"instrument_serial_number"));
        sampleFile.setExplicitGlobalStandardArea(XmlUtil.readDoubleAttribute(reader, "explicit_global_standard_area"));
        sampleFile.setIonMobilityType(XmlUtil.readAttribute(reader, "ion_mobility_type"));

        List<Instrument> instrumentList = new ArrayList<>();
        sampleFile.setInstrumentInfoList(instrumentList);

        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, SAMPLE_FILE))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, INSTRUMENT_INFO_LIST))
            {
                instrumentList.addAll(readInstrumentInfoList(reader));
            }
        }

        return sampleFile;
    }

    private List<Instrument> readInstrumentInfoList(XMLStreamReader reader) throws XMLStreamException
    {
        List<Instrument> result = new ArrayList<>();
        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, INSTRUMENT_INFO_LIST))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, INSTRUMENT_INFO))
            {
                result.add(readInstrumentInfo(reader));
            }
        }
        return result;
    }

    private Instrument readInstrumentInfo(XMLStreamReader reader) throws XMLStreamException
    {
        Instrument instrument = new Instrument();
        while(reader.hasNext()) {

            int evtType = _reader.next();

            if(XmlUtil.isEndElement(reader, evtType, INSTRUMENT_INFO))
            {
                break;
            }
            if(XmlUtil.isStartElement(reader, evtType, "ionsource"))
            {
                instrument.setIonizationType(readTextElementValue(reader, "ionsource"));
            }
            if(XmlUtil.isStartElement(reader, evtType, "analyzer"))
            {
                instrument.setAnalyzer(readTextElementValue(reader, "analyzer"));
            }
            if(XmlUtil.isStartElement(reader, evtType, "detector"))
            {
                instrument.setDetector(readTextElementValue(reader, "detector"));
            }
            if(XmlUtil.isStartElement(reader, evtType, "model"))
            {
                instrument.setModel(readTextElementValue(reader, "model"));
            }
        }
        return instrument;
    }

    private String readTextElementValue(XMLStreamReader reader, String elementName) throws XMLStreamException
    {
        StringBuilder result = new StringBuilder();
        while (reader.hasNext())
        {
            int evtType = _reader.next();

            if (XmlUtil.isEndElement(reader, evtType, elementName))
            {
                break;
            }

            if (XmlUtil.isText(evtType))
            {
                result.append(reader.getText());
            }
        }
        return result.isEmpty() ? null : result.toString();
    }

    public boolean hasNextPeptideGroup() throws XMLStreamException
    {
       while (_reader.hasNext())
        {
            int evtType = _reader.next();
            if (evtType == XMLStreamReader.START_ELEMENT)
            {
                if (PEPTIDE_LIST.equalsIgnoreCase(_reader.getLocalName()) ||
                        PROTEIN.equalsIgnoreCase(_reader.getLocalName()) ||
                        PROTEIN_GROUP.equalsIgnoreCase(_reader.getLocalName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public PeptideGroup nextPeptideGroup() throws XMLStreamException
    {
        return readPeptideGroup(_reader);
    }

    private PeptideGroup readPeptideGroup(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideGroup pepGroup = new PeptideGroup();
        List<PeptideGroupAnnotation> annotations = new ArrayList<>();
        pepGroup.setAnnotations(annotations);

        if (PROTEIN.equalsIgnoreCase(_reader.getLocalName()) || PROTEIN_GROUP.equalsIgnoreCase(_reader.getLocalName()))
        {
            // <protein> elements have the 'name'  and 'description' attribute
            // <protein> elements can also have a 'label_name' attribute.  This is the name that the user
            // can type in the document node in Skyline, and most likely the name they want to see in Panorama.
            String name = reader.getAttributeValue(null, "name");
            String labelName = reader.getAttributeValue(null, "label_name");
            pepGroup.setLabel(StringUtils.isBlank(labelName) ? name : labelName);
            pepGroup.setName((!StringUtils.isBlank(labelName) && !StringUtils.isBlank(name)) ? name : null);

            String description = reader.getAttributeValue(null, "description");
            String labelDescription = reader.getAttributeValue(null, "label_description");
            pepGroup.setDescription(StringUtils.isBlank(labelDescription) ? description : labelDescription);
            pepGroup.setAltDescription((!StringUtils.isBlank(labelDescription) && !StringUtils.isBlank(description)) ? description : null);
        }
        else
        {
            // <peptide_list> elements have the 'label_name' attribute
            pepGroup.setLabel(reader.getAttributeValue(null, "label_name"));
            pepGroup.setDescription(reader.getAttributeValue(null, "label_description"));
        }

        Protein protein = null;
        if (PROTEIN.equalsIgnoreCase(reader.getLocalName()))
        {
            // Single protein entry - split between PeptideGroup and Protein for storage
            String accession = reader.getAttributeValue(null, "accession");
            String preferredName = reader.getAttributeValue(null, "preferred_name");
            String gene = reader.getAttributeValue(null, "gene");
            String species = reader.getAttributeValue(null, "species");

            if (accession != null || preferredName != null || gene != null || species != null)
            {
                protein = pepGroup.addSingleProtein();
                protein.setAccession(accession);
                protein.setPreferredName(preferredName);
                protein.setGene(gene);
                protein.setSpecies(species);
            }
        }

        boolean decoy = Boolean.parseBoolean(reader.getAttributeValue(null, "decoy"));
        pepGroup.setDecoy(decoy);

        String decoyMatchProportion = reader.getAttributeValue(null, "decoy_match_proportion");
        if (null != decoyMatchProportion)
            pepGroup.setDecoyMatchProportion(Double.parseDouble(decoyMatchProportion));

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isStartElement(reader, evtType, PROTEIN))
            {
                pepGroup.addProtein(readProtein(reader));
            }
            if (XmlUtil.isEndElement(reader, evtType, PEPTIDE_LIST) || XmlUtil.isEndElement(reader, evtType, PROTEIN))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, SEQUENCE))
            {
                if (protein == null)
                {
                    protein = pepGroup.addSingleProtein();
                }
                protein.setSequence(reader.getElementText().replaceAll("\\s+", ""));
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PeptideGroupAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PEPTIDE) ||
                     XmlUtil.isStartElement(reader, evtType, MOLECULE))
            {
                break; // We will read peptides / molecules one by one
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                pepGroup.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.protein);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PeptideGroupAnnotation());
        }

        _peptideGroupCount++;
        updateProgress();
        return pepGroup;
    }

    private Protein readProtein(XMLStreamReader reader) throws XMLStreamException
    {
        Protein result = new Protein();

        // <protein> elements have the 'name'  and 'description' attribute
        // <protein> elements can also have a 'label_name' attribute.  This is the name that the user
        // can type in the document node in Skyline, and most likely the name they want to see in Panorama.
        String name = reader.getAttributeValue(null, "name");
        String labelName = reader.getAttributeValue(null, "label_name");
        result.setLabel(StringUtils.isBlank(labelName) ? name : labelName);
        result.setName((!StringUtils.isBlank(labelName) && !StringUtils.isBlank(name)) ? name : null);

        String description = reader.getAttributeValue(null, "description");
        String labelDescription = reader.getAttributeValue(null, "label_description");
        result.setDescription(StringUtils.isBlank(labelDescription) ? description : labelDescription);
        result.setAltDescription((!StringUtils.isBlank(labelDescription) && !StringUtils.isBlank(description)) ? description : null);

        // Single protein entry - split between PeptideGroup and Protein for storage
        result.setAccession(reader.getAttributeValue(null, "accession"));
        result.setPreferredName(reader.getAttributeValue(null, "preferred_name"));
        result.setGene(reader.getAttributeValue(null, "gene"));
        result.setSpecies(reader.getAttributeValue(null, "species"));

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, PROTEIN))
            {
                return result;
            }
            if (XmlUtil.isStartElement(reader, evtType, SEQUENCE))
            {
                result.setSequence(reader.getElementText().replaceAll("\\s+", ""));
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                // TODO - wait to implement until we confirm Skyline is allowing protein-level annotations and we have
                // an example document
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                // TODO - wait to implement until we confirm Skyline is allowing protein-level notes and we have
                // an example document
            }
        }
        return result;
    }

    public void logMissingChromatogramCounts()
    {
        for (Map.Entry<String, AtomicInteger> entry : _missingChromatograms.entrySet())
        {
            _log.warn("Missed importing " + entry.getValue().intValue() + " chromatograms from sample file " + entry.getKey());
        }
    }

    public List<OptimizationDBRow> getOptimizationInfos()
    {
        return _optimizationDBRows;
    }

    public void matchIrt(GeneralMolecule generalMolecule)
    {
        for (IrtPeptide irt : _iRTScaleSettings)
        {
            if (irt.getGeneralMoleculeId() == null && generalMolecule.textIdMatches(irt.getModifiedSequence()))
            {
                irt.setGeneralMoleculeId(generalMolecule.getId());
            }
        }
    }

    public enum MoleculeType
    {
        PEPTIDE,
        MOLECULE
    }
    public MoleculeType hasNextPeptideOrMolecule() throws XMLStreamException
    {
        while(true)
        {
            int evtType = _reader.getEventType();
            if(XmlUtil.isStartElement(_reader, evtType, PEPTIDE))
            {
                return MoleculeType.PEPTIDE;
            }
            if(XmlUtil.isStartElement(_reader, evtType, MOLECULE))
            {
                return MoleculeType.MOLECULE;
            }
            if(XmlUtil.isEndElement(_reader, evtType, PEPTIDE_LIST) ||
                    XmlUtil.isEndElement(_reader, evtType, PROTEIN) ||
                    XmlUtil.isEndElement(_reader, evtType, PROTEIN_GROUP))
            {
                return null;
            }
            if(_reader.hasNext())
                _reader.next();
            else
                break;
        }
        return null;
    }

    public Peptide nextPeptide(boolean decoy) throws XMLStreamException, IOException
    {
        Peptide peptide = new Peptide();
        readGeneralMolecule(_reader, peptide);
        readPeptide(_reader, peptide, decoy);
        updateProgress();
        return peptide;
    }

    public Molecule nextMolecule(boolean decoy) throws XMLStreamException, IOException
    {
        Molecule molecule = new Molecule();
        readGeneralMolecule(_reader, molecule);
        readSmallMolecule(_reader, molecule, decoy);
        updateProgress();
        return molecule;
    }

    private void readGeneralMolecule(XMLStreamReader reader, GeneralMolecule generalMolecule)
    {
        String predictedRt = reader.getAttributeValue(null, "predicted_retention_time");
        if (null != predictedRt)
            generalMolecule.setPredictedRetentionTime(Double.parseDouble(predictedRt));

        String avgMeasuredRt = reader.getAttributeValue(null, "avg_measured_retention_time");
        if (null != avgMeasuredRt)
            generalMolecule.setAvgMeasuredRetentionTime(Double.parseDouble(avgMeasuredRt));

        String rtCalculatorScore = reader.getAttributeValue(null, "rt_calculator_score");
        if (null != rtCalculatorScore)
            generalMolecule.setRtCalculatorScore(Double.parseDouble(rtCalculatorScore));

        generalMolecule.setExplicitRetentionTime(XmlUtil.readDoubleAttribute(reader, "explicit_retention_time"));
        generalMolecule.setNormalizationMethod(reader.getAttributeValue(null, "normalization_method"));
        generalMolecule.setInternalStandardConcentration(XmlUtil.readDoubleAttribute(reader, "internal_standard_concentration"));
        generalMolecule.setConcentrationMultiplier(XmlUtil.readDoubleAttribute(reader, "concentration_multiplier"));
        generalMolecule.setStandardType(reader.getAttributeValue(null, "standard_type"));
        generalMolecule.setExplicitRetentionTimeWindow(XmlUtil.readDoubleAttribute(reader, "explicit_retention_time_window"));
    }

    private void readSmallMolecule(XMLStreamReader reader, Molecule molecule, boolean decoy) throws XMLStreamException, IOException
    {
        // read molecule-specific attributes
        String formula = reader.getAttributeValue(null, ION_FORMULA);
        if (formula == null)
        {
            formula = reader.getAttributeValue(null, NEUTRAL_FORMULA);
        }
        molecule.setIonFormula(formula);
        molecule.setCustomIonName(reader.getAttributeValue(null, CUSTOM_ION_NAME));
        molecule.setAttributeGroupId(reader.getAttributeValue(null, ATTRIBUTE_GROUP_ID));
        molecule.setMassMonoisotopic(readRequiredMass(reader, true, MOLECULE));
        molecule.setMassAverage(readRequiredMass(reader, false, MOLECULE));
        molecule.setMoleculeId(XmlUtil.readAttribute(reader, ID));

        List<MoleculePrecursor> moleculePrecursorList = new ArrayList<>();
        molecule.setMoleculePrecursorsList(moleculePrecursorList);

        List<GeneralMoleculeAnnotation> annotations = new ArrayList<>();
        molecule.setAnnotations(annotations);

        List<GeneralMoleculeChromInfo> generalMoleculeChromInfoList = new ArrayList<>();
        molecule.setGeneralMoleculeChromInfoList(generalMoleculeChromInfoList);

        //Note for future: Storing PRECURSOR, ANNOTATION, NOTE, and PEPTIDE_RESULT for now. Peptide/Proteomic specific structural and isotope mods (see lines 1199-1221 roughly)
        //do not seem relevant or maybe we are missing something. May need to revisit in the future.
        while (_reader.hasNext())
        {
            int evtType = _reader.next();

            if (XmlUtil.isEndElement(_reader, evtType, MOLECULE))
                break;

            if (XmlUtil.isStartElement(_reader, evtType, PRECURSOR))
                moleculePrecursorList.add(readMoleculePrecursor(_reader, molecule, decoy));

            else if (XmlUtil.isStartElement(_reader, evtType, ANNOTATION))
                annotations.add(readAnnotation(_reader, new GeneralMoleculeAnnotation()));

            else if (XmlUtil.isStartElement(_reader, evtType, NOTE))
                molecule.setNote(readNote(_reader));

            else if (XmlUtil.isStartElement(_reader, evtType, PEPTIDE_RESULT))
                generalMoleculeChromInfoList.add(readGeneralMoleculeChromInfo(_reader));
        }
        _smallMoleculeCount++;
    }

    private void readPeptide(XMLStreamReader reader, Peptide peptide, boolean decoyForSkipping) throws XMLStreamException, IOException
    {
        List<Precursor> precursorList = new ArrayList<>();
        peptide.setPrecursorList(precursorList);
        List<GeneralMoleculeAnnotation> annotations = new ArrayList<>();
        peptide.setAnnotations(annotations);

        List<GeneralMoleculeChromInfo> generalMoleculeChromInfoList = new ArrayList<>();
        peptide.setGeneralMoleculeChromInfoList(generalMoleculeChromInfoList);

        String start = reader.getAttributeValue(null, "start");
        if (null != start)
            peptide.setStartIndex(Integer.parseInt(start));

        String end = reader.getAttributeValue(null, "end");
        if (null != end)
            peptide.setEndIndex(Integer.parseInt(end));

        peptide.setSequence(reader.getAttributeValue(null, "sequence"));

        // Get the peptide structurally modified sequence (format v1.5)
        peptide.setPeptideModifiedSequence(reader.getAttributeValue(null, "modified_sequence"));

        String prevAa = reader.getAttributeValue(null, "prev_aa");
        if (null != prevAa)
            peptide.setPreviousAa(prevAa);

        String nextAa = reader.getAttributeValue(null, "next_aa");
        if (null != nextAa)
            peptide.setNextAa(nextAa);

        String decoy = reader.getAttributeValue(null, "decoy");
        if (null != decoy)
        {
            peptide.setDecoy(Boolean.parseBoolean(decoy));
            // We will skip TransitionChromInfo insertion if either the protein or the peptide is marked as a decoy
            decoyForSkipping |= peptide.isDecoyPeptide();
        }

        String calcNeutralPepMass = reader.getAttributeValue(null, "calc_neutral_pep_mass");
        if (null != calcNeutralPepMass)
            peptide.setCalcNeutralMass(Double.parseDouble(calcNeutralPepMass));

        String numMissedCleavages = reader.getAttributeValue(null, "num_missed_cleavages");
        if (null != numMissedCleavages)
            peptide.setNumMissedCleavages(Integer.parseInt(numMissedCleavages));

        String rank = reader.getAttributeValue(null, "rank");
        if (null != rank)
            peptide.setRank(Integer.parseInt(rank));

        peptide.setAttributeGroupId(reader.getAttributeValue(null, ATTRIBUTE_GROUP_ID));

        List<Peptide.StructuralModification> structuralMods = new ArrayList<>();
        List<Peptide.IsotopeModification> isotopeMods = new ArrayList<>();
        peptide.setStructuralMods(structuralMods);
        peptide.setIsotopeMods(isotopeMods);

        while (reader.hasNext())
        {
            int evtType = reader.next();
            if (XmlUtil.isEndElement(reader, evtType, PEPTIDE) || XmlUtil.isEndElement(reader, evtType, MOLECULE))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRECURSOR))
            {
                precursorList.add(readPrecursor(reader, peptide, decoyForSkipping));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                peptide.setNote(readNote(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PEPTIDE_RESULT))
            {
                generalMoleculeChromInfoList.add(readGeneralMoleculeChromInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, VARIABLE_MODIFICATION))
            {
                structuralMods.add(readStructuralModification(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new GeneralMoleculeAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, EXPLICIT_STATIC_MODIFICATIONS))
            {
                structuralMods.addAll(readStructuralModifications(reader, EXPLICIT_STATIC_MODIFICATIONS, EXPLICIT_MODIFICATION));
            }
            else if (XmlUtil.isStartElement(reader, evtType, IMPLICIT_STATIC_MODIFICATIONS))
            {
                structuralMods.addAll(readStructuralModifications(reader, IMPLICIT_STATIC_MODIFICATIONS, IMPLICIT_MODIFICATION));
            }
            else if (XmlUtil.isStartElement(reader, evtType, CROSSLINKS))
            {
                structuralMods.addAll(readCrosslinkers(reader, CROSSLINKS, CROSSLINK));
            }
            else if (XmlUtil.isStartElement(reader, evtType, EXPLICIT_HEAVY_MODIFICATIONS))
            {
                isotopeMods.addAll(readIsotopeModifications(reader, EXPLICIT_HEAVY_MODIFICATIONS, EXPLICIT_MODIFICATION));
            }
            else if (XmlUtil.isStartElement(reader, evtType, IMPLICIT_HEAVY_MODIFICATIONS))
            {
                isotopeMods.addAll(readIsotopeModifications(reader, IMPLICIT_HEAVY_MODIFICATIONS, IMPLICIT_MODIFICATION));
            }
        }
        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                DataSettings.AnnotationTarget.peptide);

        for (String missingAnotName : missingBooleanAnnotations)
            addMissingBooleanAnnotation(annotations, missingAnotName, new GeneralMoleculeAnnotation());

        _peptideCount++;
    }

    private String readNote(XMLStreamReader reader) throws XMLStreamException
    {
        StringBuilder result = new StringBuilder();
        while (reader.hasNext())
        {
            reader.next();
            int evtType = reader.getEventType();
            if (XmlUtil.isEndElement(reader, evtType, NOTE))
            {
                break;
            }
            if (XmlUtil.isText(evtType))
            {
                result.append(reader.getText());
            }
        }
        return result.isEmpty() ? null : result.toString();
    }

    private Peptide.IsotopeModification readIsotopeModification(XMLStreamReader reader, String isotopeLabel)
    {
        Peptide.IsotopeModification mod = new Peptide.IsotopeModification();
        mod.setModificationName(XmlUtil.readRequiredAttribute(reader, "modification_name", VARIABLE_MODIFICATION));
        Double massDiff = XmlUtil.readDoubleAttribute(reader, "mass_diff");
        if (massDiff != null)
        {
            mod.setMassDiff(massDiff);
        }
        mod.setIndexAa(XmlUtil.readRequiredIntegerAttribute(reader, "index_aa", VARIABLE_MODIFICATION));

        mod.setIsotopeLabel(isotopeLabel);
        return mod;
    }

    private Peptide.StructuralModification readStructuralModification(XMLStreamReader reader)
    {
        Peptide.StructuralModification mod = new Peptide.StructuralModification();
        mod.setModificationName(XmlUtil.readRequiredAttribute(reader, "modification_name", VARIABLE_MODIFICATION));
        mod.setMassDiff(XmlUtil.readDoubleAttribute(reader, "mass_diff"));
        mod.setIndexAa(XmlUtil.readRequiredIntegerAttribute(reader, "index_aa", VARIABLE_MODIFICATION));
        return mod;
    }

    private List<Peptide.StructuralModification> readCrosslink(XMLStreamReader reader) throws XMLStreamException
    {
        List<Peptide.StructuralModification> result = new ArrayList<>();

        String name = XmlUtil.readRequiredAttribute(reader, "modification_name", CROSSLINK);

        while (reader.hasNext())
        {
            reader.next();
            int evtType = reader.getEventType();
            if (XmlUtil.isEndElement(reader, evtType, CROSSLINK))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, SITE))
            {
                int peptideIndex = XmlUtil.readRequiredIntegerAttribute(reader, "peptide_index", SITE);
                if (peptideIndex == 0)
                {
                    Peptide.StructuralModification mod = new Peptide.StructuralModification();
                    mod.setModificationName(name);
                    mod.setIndexAa(XmlUtil.readRequiredIntegerAttribute(reader, "index_aa", SITE));
                    mod.setPeptideIndex(XmlUtil.readRequiredIntegerAttribute(reader, "peptide_index", SITE));
                    result.add(mod);
                }
            }
        }

        return result;
    }

    private List<Peptide.StructuralModification> readStructuralModifications(XMLStreamReader reader, String parentElement, String childElement) throws XMLStreamException
    {
        List<Peptide.StructuralModification> modifications = new ArrayList<>();
        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, parentElement))
            {
                break;
            }

            else if (XmlUtil.isStartElement(reader, evtType, childElement))
            {
                modifications.add(readStructuralModification(reader));
            }
        }
        return modifications;
    }

    private List<Peptide.StructuralModification> readCrosslinkers(XMLStreamReader reader, String parentElement, String childElement) throws XMLStreamException
    {
        List<Peptide.StructuralModification> modifications = new ArrayList<>();
        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, parentElement))
            {
                break;
            }

            else if (XmlUtil.isStartElement(reader, evtType, childElement))
            {
                modifications.addAll(readCrosslink(reader));
            }
        }
        return modifications;
    }

    private List<Peptide.IsotopeModification> readIsotopeModifications(XMLStreamReader reader, String parentElement, String childElement) throws XMLStreamException
    {
        String isotopeLabel = reader.getAttributeValue(null, "isotope_label");
        if(isotopeLabel == null)
        {
            isotopeLabel = PeptideSettings.HEAVY_LABEL;
        }

        List<Peptide.IsotopeModification> modifications = new ArrayList<>();
        while(reader.hasNext())
        {
           int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, parentElement))
            {
                break;
            }

            else if (XmlUtil.isStartElement(reader, evtType, childElement))
            {
                modifications.add(readIsotopeModification(reader, isotopeLabel));
            }
        }
        return modifications;
    }

    private GeneralMoleculeChromInfo readGeneralMoleculeChromInfo(XMLStreamReader reader)
    {
        GeneralMoleculeChromInfo chromInfo = new GeneralMoleculeChromInfo();
        chromInfo.setReplicateName(XmlUtil.readRequiredAttribute(reader, "replicate", PEPTIDE_RESULT));
        chromInfo.setSkylineSampleFileId(getSkylineSampleFileId(reader, chromInfo.getReplicateName()));
        chromInfo.setRetentionTime(XmlUtil.readDoubleAttribute(reader, "retention_time"));
        chromInfo.setPeakCountRatio(XmlUtil.readDoubleAttribute(reader, "peak_count_ratio"));
        chromInfo.setExcludeFromCalibration(XmlUtil.readBooleanAttribute(reader, "exclude_from_calibration", false));
        chromInfo.setPredictedRetentionTime(XmlUtil.readDoubleAttribute(reader, "predicted_retention_time"));
        return chromInfo;
    }

    private String getSkylineSampleFileId(XMLStreamReader reader, String replicateName)
    {
        String skylineSampleFileId = XmlUtil.readAttribute(reader, "file");
        if(skylineSampleFileId == null)
        {
            skylineSampleFileId = _replicateSampleFileIdMap.get(replicateName);
            if(skylineSampleFileId == null)
            {
                throw new IllegalStateException("Could not find Skyline-given sample file Id for chrom info in replicate " + replicateName);
            }
        }
        return skylineSampleFileId;
    }

    private MoleculePrecursor readMoleculePrecursor(XMLStreamReader reader, Molecule molecule, boolean decoy) throws XMLStreamException, IOException
    {
        MoleculePrecursor moleculePrecursor = new MoleculePrecursor();
        List<MoleculeTransition> moleculeTransitionList = new ArrayList<>();
        moleculePrecursor.setTransitionsList(moleculeTransitionList);

        List<PrecursorAnnotation> annotations = new ArrayList<>();
        moleculePrecursor.setAnnotations(annotations);

        List<PrecursorChromInfo> chromInfoList = new ArrayList<>();
        moleculePrecursor.setChromInfoList(chromInfoList);

        String precursorMz = reader.getAttributeValue(null, PRECURSOR_MZ);
        if(null != precursorMz)
            moleculePrecursor.setMz(Double.parseDouble(precursorMz));
        moleculePrecursor.setIsotopeLabel(XmlUtil.readAttribute(reader, "isotope_label", PeptideSettings.IsotopeLabel.LIGHT));

        String collisionEnergy = reader.getAttributeValue(null, COLLISION_ENERGY);
        if(null != collisionEnergy)
            moleculePrecursor.setCollisionEnergy(Double.parseDouble(collisionEnergy));

        String ionFormula = reader.getAttributeValue(null, ION_FORMULA);
        if(null != ionFormula)
            moleculePrecursor.setIonFormula(ionFormula);

        moleculePrecursor.setMassMonoisotopic(readRequiredMass(reader, true, MASS_MONOISOTOPIC));
        moleculePrecursor.setMassAverage(readRequiredMass(reader, false, MASS_AVERAGE));

        Double explicitIonMobility = XmlUtil.readDoubleAttribute(reader, "explicit_ion_mobility");
        // fall back to support older documents
        if (null == explicitIonMobility)
            explicitIonMobility = XmlUtil.readDoubleAttribute(reader, "explicit_drift_time_msec");
        moleculePrecursor.setExplicitIonMobility(explicitIonMobility);

        moleculePrecursor.setCcs(XmlUtil.readDoubleAttribute(reader, "ccs"));

        String explicitIonMobilityUnits = XmlUtil.readAttribute(reader, "explicit_ion_mobility_units");
        if (null != explicitIonMobilityUnits)
            moleculePrecursor.setExplicitIonMobilityUnits(explicitIonMobilityUnits);

        moleculePrecursor.setExplicitCcsSqa(XmlUtil.readDoubleAttribute(reader, "explicit_ccs_sqa"));
        moleculePrecursor.setExplicitCompensationVoltage(XmlUtil.readDoubleAttribute(reader, "explicit_compensation_voltage"));
        moleculePrecursor.setPrecursorConcentration(XmlUtil.readDoubleAttribute(reader, "precursor_concentration"));

        String customIonName = reader.getAttributeValue(null, CUSTOM_ION_NAME);
        if(null != customIonName)
            moleculePrecursor.setCustomIonName(customIonName);

        String charge = reader.getAttributeValue(null, CHARGE);
        if(null != charge)
            moleculePrecursor.setCharge(Integer.parseInt(charge));
        List<SpectrumFilter.FilterClause> spectrumFilterClauses = new ArrayList<>();
        while(reader.hasNext())
        {
            int evtType = reader.next();

            if(evtType == XMLStreamReader.END_ELEMENT && PRECURSOR.equalsIgnoreCase(reader.getLocalName()))
                break;

            if (XmlUtil.isStartElement(reader, evtType, TRANSITION))
                moleculeTransitionList.add(readSmallMoleculeTransition(reader, decoy));
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION_DATA)) {
                moleculeTransitionList.addAll(readTransitionData(reader, this::transitionProtoToMoleculeTransition));
            }
            else if(XmlUtil.isStartElement(reader, evtType, PRECURSOR_PEAK))
                chromInfoList.add(readPrecursorChromInfo(reader));
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
                annotations.add(readAnnotation(reader, new PrecursorAnnotation()));
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
                moleculePrecursor.setNote(readNote(reader));
            else if (XmlUtil.isStartElement(reader, evtType, SPECTRUM_FILTER))
                spectrumFilterClauses.add(SpectrumFilter.FilterClause.parse(reader));
        }
        moleculePrecursor.setSpectrumFilter(SpectrumFilter.fromFilterClauses(spectrumFilterClauses)
                .map(SpectrumFilter::toByteArray).orElse(null));
        List<ChromGroupHeaderInfo> chromatograms = tryLoadChromatogram(moleculeTransitionList, molecule, moleculePrecursor, _matchTolerance);
        populateChromInfoChromatograms(moleculePrecursor, chromatograms);

        _precursorCount++;

        computePrecursorChromInfoValues(moleculePrecursor);

        return moleculePrecursor;
    }

    /**
     * Populates additional information on PrecursorChromInfos that can be calculated from the TransitionChromInfos.
     *
     * Issue 41973: Populate additional information for rendering chromatograms
     * Set the BestMassErrorPpm for each PrecursorChromInfo of the given precursor. This is the mass error of the most intense
     * transition peak in a replicate. Skyline does not give us this value but we need it to label the precursor peaks in
     * chromatogram charts. We can query it from TransitionChromInfos but we are saving it with the PrecursorChromInfo
     * because TransitionChromInfos do not get saved for large documents.
     *
     * Also: populate TotalAreaMs1 and TotalAreaFragment
     */
    private void computePrecursorChromInfoValues(GeneralPrecursor<?> precursor)
    {
        TransitionSettings.FullScanSettings fullScanSettings = _transitionSettings == null ? null : _transitionSettings.getFullScanSettings();

        // Key is the replicate name
        // Value is a List where each member is a Pair of TransitionChromInfo and a Boolean value indicating if the transition is quantitative
        Map<String, List<Tuple3<TransitionChromInfo, Boolean, Integer>>> replicateToTciListMap = new HashMap<>();
        int ms1TransitionCount = 0;
        int fragmentTransitionCount = 0;

        for (GeneralTransition transition : precursor.getTransitionsList())
        {
            List<TransitionChromInfo> tciList = transition.getChromInfoList();
            Boolean quantitative = transition.isQuantitative(fullScanSettings);
            Integer msLevel = transition.isMs1() ? 1 : 2;
            for(TransitionChromInfo tci: tciList)
            {
                List<Tuple3<TransitionChromInfo, Boolean, Integer>> tcisForReplicate = replicateToTciListMap.computeIfAbsent(tci.getReplicateName(), list -> new ArrayList<>());
                tcisForReplicate.add(Tuple3.of(tci, quantitative, msLevel));
            }
            if (quantitative)
            {
                if (transition.isMs1())
                {
                    ms1TransitionCount++;
                }
                else
                {
                    fragmentTransitionCount++;
                }
            }
        }

        for(PrecursorChromInfo pci: precursor.getChromInfoList())
        {
            List<Tuple3<TransitionChromInfo, Boolean, Integer>> tciList = replicateToTciListMap.get(pci.getReplicateName());
            double totalAreaFragment = 0;
            double totalAreaMs1 = 0;
            int ms1AreaCount = 0;
            int fragmentAreaCount = 0;

            if(tciList != null && !tciList.isEmpty())
            {
                double maxHeight = 0.0;
                Double bestMassError = null;
                for(Tuple3<TransitionChromInfo, Boolean, Integer> tciInfo: tciList)
                {
                    TransitionChromInfo tci = tciInfo.first;
                    Boolean quantitative = tciInfo.second;
                    Integer msLevel = tciInfo.third;

                    Double height = tci.getHeight();
                    if(quantitative && height != null && height > maxHeight)
                    {
                        maxHeight = tci.getHeight();
                        bestMassError = tci.getMassErrorPPM();
                    }
                    if (quantitative && tci.getArea() != null)
                    {
                        if (msLevel == 1)
                        {
                            totalAreaMs1 += tci.getArea();
                            ms1AreaCount++;
                        }
                        else
                        {
                            totalAreaFragment += tci.getArea();
                            fragmentAreaCount++;
                        }
                    }
                }
                pci.setBestMassErrorPPM(bestMassError);
            }
            pci.setTotalAreaMs1(ms1AreaCount == ms1TransitionCount ? totalAreaMs1 : null);
            pci.setTotalAreaFragment(fragmentAreaCount == fragmentTransitionCount ? totalAreaFragment : null);
        }
    }

    private Precursor readPrecursor(XMLStreamReader reader, Peptide peptide, boolean decoy) throws XMLStreamException, IOException
    {
        Precursor precursor = new Precursor();
        List<Transition> transitionList = new ArrayList<>();
        precursor.setTransitionsList(transitionList);
        List<PrecursorAnnotation> annotations = new ArrayList<>();
        precursor.setAnnotations(annotations);

        List<PrecursorChromInfo> chromInfoList = new ArrayList<>();
        precursor.setChromInfoList(chromInfoList);

        String charge = reader.getAttributeValue(null, CHARGE);
        if(null != charge)
            precursor.setCharge(Integer.parseInt(charge));

        String calcNeutralMass = reader.getAttributeValue(null, "calc_neutral_mass");
        if(null != calcNeutralMass)
            precursor.setNeutralMass(Double.parseDouble(calcNeutralMass));

        String precursorMz = reader.getAttributeValue(null, PRECURSOR_MZ);
        if(null != precursorMz)
            precursor.setMz(Double.parseDouble(precursorMz));

        // Skyline-daily 3.5.1.9426 (and patch release of Skyline 3.5) changed the format of the modified_sequence attribute
        // of the <precursor> element to always have a decimal place in the modification mass.
        // Example: [+80.0] instead of [+80].
        // If the file being uploaded is from an older version fo Skyline, replace modification mass strings like [+80] with [+80.0].
        // e.g. K[+96.2]VN[-17]K[+34.1]TES[+80]K[+62.1] -->  K[+96.2]VN[-17.0]K[+34.1]TES[+80.0]K[+62.1]
        precursor.setModifiedSequence(ensureDecimalInModMass(reader.getAttributeValue(null, "modified_sequence")));

        precursor.setIsotopeLabel(XmlUtil.readAttribute(reader, "isotope_label", PeptideSettings.IsotopeLabel.LIGHT));

        String collisionEnergy = reader.getAttributeValue(null, COLLISION_ENERGY);
        if(null != collisionEnergy)
            precursor.setCollisionEnergy(Double.parseDouble(collisionEnergy));

        String declustPotential = reader.getAttributeValue(null, DECLUSTERING_POTENTIAL);
        if(null != declustPotential)
            precursor.setDeclusteringPotential(Double.parseDouble(declustPotential));

        Double explicitIonMobility = XmlUtil.readDoubleAttribute(reader, "explicit_ion_mobility");
        // fall back to support older documents
        if (null == explicitIonMobility)
            explicitIonMobility = XmlUtil.readDoubleAttribute(reader, "explicit_drift_time_msec");
        precursor.setExplicitIonMobility(explicitIonMobility);

        precursor.setCcs(XmlUtil.readDoubleAttribute(reader, "ccs"));

        String explicitIonMobilityUnits = XmlUtil.readAttribute(reader, "explicit_ion_mobility_units");
        if (null != explicitIonMobilityUnits)
            precursor.setExplicitIonMobilityUnits(explicitIonMobilityUnits);

        precursor.setExplicitCcsSqa(XmlUtil.readDoubleAttribute(reader, "explicit_ccs_sqa"));
        precursor.setExplicitCompensationVoltage(XmlUtil.readDoubleAttribute(reader, "explicit_compensation_voltage"));
        precursor.setPrecursorConcentration(XmlUtil.readDoubleAttribute(reader, "precursor_concentration"));
        List<SpectrumFilter.FilterClause> spectrumFilterClauses = new ArrayList<>();
        while (reader.hasNext()) {

            int evtType = reader.next();
            if (evtType == XMLStreamReader.END_ELEMENT &&
               PRECURSOR.equalsIgnoreCase(reader.getLocalName()))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, BIBLIOSPEC_SPECTRUM_INFO))
            {
                precursor.setBibliospecLibraryInfo(readBibliospecLibraryInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, HUNTER_SPECTRUM_INFO))
            {
                precursor.setHunterLibraryInfo(readHunterLibraryInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NIST_SPECTRUM_INFO))
            {
                precursor.setNistLibraryInfo(readNistLibraryInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, SPECTRAST_SPECTRUM_INFO))
            {
                precursor.setSpectrastLibraryInfo(readSpectrastLibraryInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, CHROMATOGRAM_LIBRARY_SPECTRUM_INFO))
            {
                precursor.setChromatogramLibraryInfo(readChromatogramLibraryInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION))
            {
                transitionList.add(readProteomicTransition(reader, decoy));
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION_DATA)) {
                transitionList.addAll(readTransitionData(reader, this::transitionProtoToTransition));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRECURSOR_PEAK))
            {
               chromInfoList.add(readPrecursorChromInfo(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PrecursorAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                precursor.setNote(readNote(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, SPECTRUM_FILTER))
            {
                spectrumFilterClauses.add(SpectrumFilter.FilterClause.parse(reader));
            }
        }

        precursor.setSpectrumFilter(SpectrumFilter.fromFilterClauses(spectrumFilterClauses)
                .map(SpectrumFilter::toByteArray).orElse(null));

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.precursor);
        for (String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PrecursorAnnotation());
        }

        List<ChromGroupHeaderInfo> chromatograms = tryLoadChromatogram(transitionList, peptide, precursor, _matchTolerance);
        populateChromInfoChromatograms(precursor, chromatograms);

        _precursorCount++;

        computePrecursorChromInfoValues(precursor);
        return precursor;
    }
    private Transition transitionProtoToTransition(SkylineDocument.SkylineDocumentProto.Transition transitionProto) {
        Transition transition = new Transition();
        transition.setNeutralLosses(new ArrayList<>());
        transition.setNeutralMass(transitionProto.getCalcNeutralMass());
        transition.setNeutralLossMass(transitionProto.getLostMass());
        if (0 != transitionProto.getFragmentOrdinal()) {
            transition.setFragmentOrdinal(transitionProto.getFragmentOrdinal());
        }
        if (0 != transitionProto.getCleavageAa()) {
            transition.setCleavageAa(String.valueOf((char) transitionProto.getCleavageAa()));
        }
        if (null != transitionProto.getDecoyMassShift()) {
            transition.setDecoyMassShift((double) transitionProto.getDecoyMassShift().getValue());
        }
        transition.setMeasuredIonName(fromOptional(transitionProto.getMeasuredIonName()));
        transition.setPrecursorMz(transitionProto.getPrecursorMz());
        transition.setCollisionEnergy(fromOptional(transitionProto.getCollisionEnergy()));
        transition.setDeclusteringPotential(fromOptional(transitionProto.getDeclusteringPotential()));
        for (SkylineDocument.SkylineDocumentProto.TransitionLoss transitionLossProto : transitionProto.getLossesList()) {
            transition.getNeutralLosses().add(toTransitionLoss(transitionLossProto));
        }
        if (transitionProto.getLinkedIonsCount() != 0)
        {
            ComplexFragmentIonName complexFragmentIonName;
            if (transitionProto.getOrphanedCrosslinkIon())
            {
                complexFragmentIonName = new ComplexFragmentIonName(null, 0);
            }
            else
            {
                // Get the ion type from the transitionProto, because transition.getFragmentType() has not been set yet
                String ionType = ionTypeToString(transitionProto.getFragmentType());

                complexFragmentIonName = new ComplexFragmentIonName(ionType, transitionProto.getFragmentOrdinal());
            }
            for (SkylineDocument.SkylineDocumentProto.LinkedIon linkedIon : transitionProto.getLinkedIonsList())
            {
                complexFragmentIonName.addChild(readLinkedIon(linkedIon));
            }
            transition.setComplexFragmentIon(complexFragmentIonName.toString());
            transition.setCharge(transitionProto.getCharge());

        }

        return transition;
    }

    private MoleculeTransition transitionProtoToMoleculeTransition(SkylineDocument.SkylineDocumentProto.Transition transitionProto) {
        MoleculeTransition transition = new MoleculeTransition();
        transition.setIonFormula(fromOptional(transitionProto.getFormula()));
        transition.setCustomIonName(fromOptional(transitionProto.getCustomIonName()));
        transition.setMassMonoisotopic(fromOptional(transitionProto.getMonoMass()));
        transition.setMassAverage(fromOptional(transitionProto.getAverageMass()));
        return transition;
    }

    private <T extends GeneralTransition> List<T> readTransitionData(XMLStreamReader reader, Function<SkylineDocument.SkylineDocumentProto.Transition, T> createTransitionFunc) {
        try {
            List<T> list = new ArrayList<>();
            String elementText = reader.getElementText();
            byte[] bytes = Base64.getDecoder().decode(elementText);
            SkylineDocument.SkylineDocumentProto.TransitionData transitionData = SkylineDocument.SkylineDocumentProto.TransitionData.parseFrom(bytes);
            for (SkylineDocument.SkylineDocumentProto.Transition transitionProto : transitionData.getTransitionsList()) {
                T transition = createTransitionFunc.apply(transitionProto);
                transition.setAnnotations(new ArrayList<>());
                transition.setChromInfoList(new ArrayList<>());
                transition.setMz(transitionProto.getProductMz());
                transition.setFragmentType(ionTypeToString(transitionProto.getFragmentType()));
                if (transitionProto.getFragmentType() != SkylineDocument.SkylineDocumentProto.IonType.ION_TYPE_precursor) {
                    transition.setCharge(transitionProto.getCharge());
                }
                if (0 != transitionProto.getMassIndex() || transition.isPrecursorIon()) {
                    transition.setMassIndex(transitionProto.getMassIndex());
                }
                transition.setIsotopeDistRank(fromOptional(transitionProto.getIsotopeDistRank()));
                transition.setIsotopeDistProportion(fromOptional(transitionProto.getIsotopeDistProportion()));
                transition.setExplicitCollisionEnergy(fromOptional(transitionProto.getCollisionEnergy()));
                transition.setExplicitDeclusteringPotential(fromOptional(transitionProto.getDeclusteringPotential()));
                transition.setQuantitative(!transitionProto.getNotQuantitative());
                transition.setExplicitIonMobilityHighEnergyOffset(fromOptional(transitionProto.getExplicitIonMobilityHighEnergyOffset()));
                transition.setCollisionEnergy(fromOptional(transitionProto.getCollisionEnergy()));
                transition.setDeclusteringPotential(fromOptional(transitionProto.getDeclusteringPotential()));
                transition.setExplicitSLens(transition.getExplicitSLens());
                transition.setExplicitConeVoltage(transition.getExplicitConeVoltage());
                if (null != transitionProto.getLibInfo()) {
                    transition.setRank(transitionProto.getLibInfo().getRank());
                    transition.setIntensity((double) transitionProto.getLibInfo().getIntensity());
                }

                if (null != transitionProto.getResults()) {
                    transition.getChromInfoList().addAll(makeTransitionChromInfos(transitionProto.getResults()));
                }
                if (null != transitionProto.getAnnotations()) {
                    if (!StringUtils.isEmpty(transitionProto.getAnnotations().getNote())) {
                        transition.setNote(transitionProto.getAnnotations().getNote());
                    }
                    List<TransitionAnnotation> annotations = new ArrayList<>();
                    for (SkylineDocument.SkylineDocumentProto.AnnotationValue transitionValue : transitionProto.getAnnotations().getValuesList()) {
                        annotations.add(readAnnotationValue(transitionValue, new TransitionAnnotation()));
                    }
                    transition.setAnnotations(annotations);
                }

                if (transition.getFragmentType() == null)
                {
                    // If we are unable to read the fragment type from the compressed transition data, it probably means that we need to add support for a new ion type.
                    throw new IllegalStateException("Unable to read fragment type from transition data. Transition m/z: " + transition.getMz() + ", charge: " +transition.getCharge());
                }

                list.add(transition);
            }
            _transitionCount += list.size();
            return list;
        } catch (Exception e) {
            throw UnexpectedException.wrap(e);
        }
    }

    private Pair<ComplexFragmentIonName.ModificationSite, ComplexFragmentIonName> readLinkedIon(SkylineDocument.SkylineDocumentProto.LinkedIon linkedIon)
    {
        ComplexFragmentIonName complexFragmentIonName;
        if (linkedIon.getOrphan())
        {
            complexFragmentIonName = new ComplexFragmentIonName(null, null);
        }
        else
        {
            complexFragmentIonName = new ComplexFragmentIonName(ionTypeToString(linkedIon.getIonType()), linkedIon.getOrdinal());

        }
        for (SkylineDocument.SkylineDocumentProto.LinkedIon child : linkedIon.getChildrenList())
        {
            complexFragmentIonName.addChild(readLinkedIon(child));
        }
        ComplexFragmentIonName.ModificationSite modificationSite = null;
        if (!linkedIon.getModificationName().isEmpty()) {
            modificationSite = new ComplexFragmentIonName.ModificationSite(linkedIon.getModificationIndex(), linkedIon.getModificationName());
        }
        return Pair.of(modificationSite, complexFragmentIonName);
    }

    // Replace strings like [+80] in the modified sequence with [+80.0]
    // e.g. K[+96.2]VN[-17]K[+34.1]TES[+80]K[+62.1] --> K[+96.2]VN[-17.0]K[+34.1]TES[+80.0]K[+62.1]
    public static String ensureDecimalInModMass(String modifiedSequence)
    {
        return oldModMassPattern.matcher(modifiedSequence).replaceAll("$1.0]");
    }

    private void populateChromInfoChromatograms(GeneralPrecursor<?> precursor, List<ChromGroupHeaderInfo> chromatograms) throws IOException
    {
        Map<String, ChromGroupHeaderInfo> filePathChromatogramMap = new HashMap<>();
        for(ChromGroupHeaderInfo chromatogram : chromatograms)
        {
            filePathChromatogramMap.put(_binaryParser.getFilePath(chromatogram), chromatogram);
        }

        Map<String, PrecursorChromInfo> pciBySkylineSampleFileId = new HashMap<>();

        for(Iterator<PrecursorChromInfo> i = precursor.getChromInfoList().iterator(); i.hasNext(); )
        {
            PrecursorChromInfo chromInfo = i.next();
            String filePath = _sampleFileIdToFilePathMap.get(chromInfo.getSkylineSampleFileId());
            ChromGroupHeaderInfo chromatogram = getChromGroupHeaderInfoForFile(filePathChromatogramMap, i, filePath);
            pciBySkylineSampleFileId.put(chromInfo.getSkylineSampleFileId(), chromInfo);
            if (chromatogram != null)
            {
                // Do not load the chromatogram bytes here since we will read them from the skyd file on-demand.
                chromInfo.setChromatogramFormat(chromatogram.getChromatogramBinaryFormat().ordinal());
                chromInfo.setChromatogramOffset(chromatogram.getLocationPoints());
                chromInfo.setChromatogramLength(chromatogram.getCompressedSize());

                chromInfo.setNumPoints(chromatogram.getNumPoints());
                chromInfo.setNumTransitions(chromatogram.getNumTransitions());
                chromInfo.setUncompressedSize(chromatogram.getUncompressedSize());
            }
        }

        List<? extends GeneralTransition> transitionsList = precursor.getTransitionsList();
        for (GeneralTransition transition: transitionsList)
        {
            for (Iterator<TransitionChromInfo> iter = transition.getChromInfoList().iterator(); iter.hasNext(); )
            {
                TransitionChromInfo transChromInfo = iter.next();
                String filePath = _sampleFileIdToFilePathMap.get(transChromInfo.getSkylineSampleFileId());
                ChromGroupHeaderInfo c = getChromGroupHeaderInfoForFile(filePathChromatogramMap, iter, filePath);
                if (c != null)
                {
                    int matchIndex = -1;
                    // Figure out which index into the list of transitions we're inserting.
                    double deltaNearestMz = Double.MAX_VALUE;
                    SignedMz[] transitions = Arrays.stream(_binaryParser.getTransitions(c))
                            .map(t->t.getProduct(c))
                            .toArray(SignedMz[]::new);
                    double transitionMz = transition.getMz();
                    if (transChromInfo.isOptimizationPeak())
                    {
                        // From the CE Optimization tutorial:
                        // The product m/z value is incremented slightly for each value as first described by Sherwood et al., 2009
                        transitionMz += OPTIMIZE_SHIFT_SIZE * transChromInfo.getOptimizationStep();
                    }
                    for (int i = 0; i < transitions.length; i++)
                    {
                        double deltaMz = Math.abs(transitionMz - transitions[i].getMz());

                        if (deltaMz < _transitionSettings.getInstrumentSettings().getMzMatchTolerance() &&
                            deltaMz < deltaNearestMz)
                        {
                            // If there are multiple matches within the given mz match tolerance return the closest match.
                            matchIndex = i;
                            deltaNearestMz = deltaMz;
                        }
                    }
                    if (matchIndex == -1)
                    {
                        incrementMissingChromatograms(filePath,
                                "Unable to find a matching chromatogram for file path " + filePath +
                                ". SKYD file may be out of sync with primary Skyline document. Transition " + transition +
                                ", " + precursor + ", " +precursor.getCharge());
                    }
                    else
                    {
                        PrecursorChromInfo pci = pciBySkylineSampleFileId.get(transChromInfo.getSkylineSampleFileId());
                        pci.addTransitionChromatogramIndex(matchIndex);
                    }
                    transChromInfo.setChromatogramIndex(matchIndex);
                }
            }
        }
    }

    private void incrementMissingChromatograms(String filePath, String logMessage)
    {
        AtomicInteger count = _missingChromatograms.computeIfAbsent(filePath, s -> {
            _log.warn(logMessage);
            return new AtomicInteger(0);
        });
        count.incrementAndGet();
    }

    @Nullable
    private ChromGroupHeaderInfo getChromGroupHeaderInfoForFile(Map<String, ChromGroupHeaderInfo> filePathChromatogramMap, Iterator<?> i, String filePath)
    {
        ChromGroupHeaderInfo chromatogram = filePathChromatogramMap.get(filePath);

        // Issue 40959 - Try stripping off the URI parameters if we don't have a match
        if (chromatogram == null)
        {
            int queryParamIdx = filePath.lastIndexOf('?');
            if (queryParamIdx != -1)
            {
                filePath = filePath.substring(0, queryParamIdx);
                chromatogram = filePathChromatogramMap.get(filePath);
            }
        }

        // Issue 40959 - If we still don't have a match, strip off values from the map for more possible matches
        if (chromatogram == null)
        {
            for (Map.Entry<String, ChromGroupHeaderInfo> entry : filePathChromatogramMap.entrySet())
            {
                if (entry.getKey().startsWith(filePath + "?"))
                {
                    chromatogram = entry.getValue();
                    break;
                }
            }
        }

        if (chromatogram == null)
        {
            incrementMissingChromatograms(filePath, "Unable to find at least one chromatogram for file path " + filePath);
            i.remove();
        }
        return chromatogram;
    }

    private Precursor.BibliospecLibraryInfo readBibliospecLibraryInfo(XMLStreamReader reader)
    {
        // <bibliospec_spectrum_info library_name="Yeast_mini" count_measured="895" />
        Precursor.BibliospecLibraryInfo libInfo = new Precursor.BibliospecLibraryInfo();
        libInfo.setLibraryName(XmlUtil.readAttribute(reader, LIBRARY_NAME, BIBLIOSPEC_SPECTRUM_INFO));
        libInfo.setCountMeasured(XmlUtil.readDoubleAttribute(reader, COUNT_MEASURED));
        libInfo.setScore(XmlUtil.readDoubleAttribute(reader, SCORE));
        libInfo.setScoreType(XmlUtil.readAttribute(reader, SCORE_TYPE, BIBLIOSPEC_SPECTRUM_INFO));
        return libInfo;
    }

    private Precursor.HunterLibraryInfo readHunterLibraryInfo(XMLStreamReader reader)
    {
        // <hunter_spectrum_info library_name="GPM_Hunter_yeast" expect="1.030315E-10" processed_intensity="213.6469" />
        Precursor.HunterLibraryInfo libInfo = new Precursor.HunterLibraryInfo();
        libInfo.setLibraryName(XmlUtil.readAttribute(reader, LIBRARY_NAME, HUNTER_SPECTRUM_INFO));
        libInfo.setExpect(XmlUtil.readDoubleAttribute(reader, EXPECT));
        libInfo.setProcessedIntensity(XmlUtil.readDoubleAttribute(reader, PROCESSED_INTENSITY));
        return libInfo;
    }

    private Precursor.NistLibraryInfo readNistLibraryInfo(XMLStreamReader reader)
    {
        // <nist_spectrum_info library_name="NIST_MSP_Yeast_qtof" count_measured="14" total_intensity="75798" tfratio="17000" />
        Precursor.NistLibraryInfo libInfo = new Precursor.NistLibraryInfo();
        libInfo.setLibraryName(XmlUtil.readAttribute(reader, LIBRARY_NAME, NIST_SPECTRUM_INFO));
        libInfo.setCountMeasured(XmlUtil.readDoubleAttribute(reader, COUNT_MEASURED));
        libInfo.setTotalIntensity(XmlUtil.readDoubleAttribute(reader,TOTAL_INTENSITY ));
        libInfo.setTfRatio(XmlUtil.readDoubleAttribute(reader, TFRATIO));
        return libInfo;
    }

    private Precursor.SpectrastLibraryInfo readSpectrastLibraryInfo(XMLStreamReader reader)
    {
        // <spectrast_spectrum_info library_name="ISB_SpectraST_yeast" count_measured="62" total_intensity="94691.2" tfratio="1000" />
        // <spectrast_spectrum_info library_name="NIST_SpectraST_Yeast_qtof" count_measured="14" total_intensity="75798" tfratio="17000" />
        Precursor.SpectrastLibraryInfo libInfo = new Precursor.SpectrastLibraryInfo();
        libInfo.setLibraryName(XmlUtil.readAttribute(reader, LIBRARY_NAME, SPECTRAST_SPECTRUM_INFO));
        libInfo.setCountMeasured(XmlUtil.readDoubleAttribute(reader, COUNT_MEASURED));
        libInfo.setTotalIntensity(XmlUtil.readDoubleAttribute(reader, TOTAL_INTENSITY));
        libInfo.setTfRatio(XmlUtil.readDoubleAttribute(reader, TFRATIO));
        return libInfo;
    }

    private Precursor.ChromatogramLibraryInfo readChromatogramLibraryInfo(XMLStreamReader reader)
    {
        Precursor.ChromatogramLibraryInfo libInfo = new Precursor.ChromatogramLibraryInfo();
        libInfo.setLibraryName(XmlUtil.readAttribute(reader, LIBRARY_NAME, CHROMATOGRAM_LIBRARY_SPECTRUM_INFO));
        libInfo.setPeakArea(XmlUtil.readDoubleAttribute(reader, PEAK_AREA));
        return libInfo;
    }

    private PrecursorChromInfo readPrecursorChromInfo(XMLStreamReader reader) throws XMLStreamException
    {
        PrecursorChromInfo chromInfo = new PrecursorChromInfo();
        List<PrecursorChromInfoAnnotation> annotations = new ArrayList<>();
        chromInfo.setAnnotations(annotations);

        chromInfo.setReplicateName(XmlUtil.readRequiredAttribute(reader, "replicate", PRECURSOR_PEAK));
        chromInfo.setSkylineSampleFileId(getSkylineSampleFileId(reader, chromInfo.getReplicateName()));
        chromInfo.setOptimizationStep(XmlUtil.readIntegerAttribute(reader, "step"));
        chromInfo.setBestRetentionTime(XmlUtil.readDoubleAttribute(reader, "retention_time"));
        chromInfo.setMinStartTime(XmlUtil.readDoubleAttribute(reader, "start_time"));
        chromInfo.setMaxEndTime(XmlUtil.readDoubleAttribute(reader, "end_time"));
        chromInfo.setTotalArea(XmlUtil.readDoubleAttribute(reader, "area"));
        chromInfo.setTotalBackground(XmlUtil.readDoubleAttribute(reader, "background"));
        chromInfo.setMaxHeight(XmlUtil.readDoubleAttribute(reader, "height"));
        chromInfo.setAverageMassErrorPPM(XmlUtil.readDoubleAttribute(reader, "mass_error_ppm"));
        Double fwhm =  XmlUtil.readDoubleAttribute(reader, "fwhm");
        // TODO: Found NaN value for fwhm in Study7.sky.  Should this happen?
        fwhm = (fwhm != null && fwhm.isNaN()) ? null : fwhm;
        chromInfo.setMaxFwhm(fwhm);
        chromInfo.setNumTruncated(XmlUtil.readIntegerAttribute(reader, "truncated"));
        chromInfo.setIdentified(XmlUtil.readAttribute(reader, "identified"));
        chromInfo.setLibraryDotP(XmlUtil.readDoubleAttribute(reader, "library_dotp"));
        chromInfo.setIsotopeDotP(XmlUtil.readDoubleAttribute(reader, "isotope_dotp"));
        chromInfo.setPeakCountRatio(XmlUtil.readDoubleAttribute(reader, "peak_count_ratio"));
        chromInfo.setUserSet(XmlUtil.readAttribute(reader, "user_set"));
        chromInfo.setQvalue(XmlUtil.readDoubleAttribute(reader, "qvalue"));
        chromInfo.setZscore(XmlUtil.readDoubleAttribute(reader, "zscore"));
        chromInfo.setCcs(XmlUtil.readDoubleAttribute(reader, "ccs"));

        // Support old Skyline documents that used "drift time" instead of "ion mobility"
        Double driftTimeMs1 = XmlUtil.readDoubleAttribute(reader, "drift_time_ms1");
        Double driftTimeWindow = XmlUtil.readDoubleAttribute(reader, "drift_time_window");
        Double driftTimeFragment = XmlUtil.readDoubleAttribute(reader, "drift_time_fragment");

        if (driftTimeMs1 != null || driftTimeWindow != null || driftTimeFragment != null)
        {
            chromInfo.setIonMobilityMs1(driftTimeMs1);
            chromInfo.setIonMobilityFragment(driftTimeFragment);
            chromInfo.setIonMobilityWindow(driftTimeWindow);
            chromInfo.setIonMobilityType("drift_time_ms");
        }
        else
        {
            chromInfo.setIonMobilityMs1(XmlUtil.readDoubleAttribute(reader, "ion_mobility_ms1"));
            chromInfo.setIonMobilityFragment(XmlUtil.readDoubleAttribute(reader, "ion_mobility_fragment"));
            chromInfo.setIonMobilityWindow(XmlUtil.readDoubleAttribute(reader, "ion_mobility_window"));
            chromInfo.setIonMobilityType(XmlUtil.readAttribute(reader, "ion_mobility_type"));
        }

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PRECURSOR_PEAK))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new PrecursorChromInfoAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                chromInfo.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.precursor_result);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new PrecursorChromInfoAnnotation());
        }

        return chromInfo;
    }
    private Double readMass(XMLStreamReader reader, boolean monoisotopic)
    {
        Double massH = XmlUtil.readDoubleAttribute(reader, monoisotopic ? MASS_MONOISOTOPIC : MASS_AVERAGE);
        if (massH != null)
        {
            // Consider: should we subtract a proton from massH so we are always returning neutral mass?
            return massH;
        }
        return XmlUtil.readDoubleAttribute(reader,
                monoisotopic ? NEUTRAL_MASS_MONOISOTOPIC : NEUTRAL_MASS_AVERAGE);
    }

    private double readRequiredMass(XMLStreamReader reader, boolean monoisotopic, String elementName)
    {
        Double mass = readMass(reader, monoisotopic);
        if (mass == null)
        {
            throw new IllegalStateException("Missing mass attribute for element " + elementName);
        }
        return mass;
    }

    /** @param decoy if the transition is associated with a molecule group that's marked as a decoy and we can skip inserting its TransitionChromInfos*/
    private MoleculeTransition readSmallMoleculeTransition(XMLStreamReader reader, boolean decoy) throws XMLStreamException
    {
        MoleculeTransition transition = new MoleculeTransition();
        readGeneralTransition(reader, transition);

        // read molecule transition-specific attributes
        transition.setIonFormula(_reader.getAttributeValue(null, ION_FORMULA));
        transition.setCustomIonName(_reader.getAttributeValue(null, CUSTOM_ION_NAME));
        transition.setMassMonoisotopic(readRequiredMass(reader, true, TRANSITION));
        transition.setMassAverage(readRequiredMass(reader, false, TRANSITION));
        transition.setMoleculeTransitionId(XmlUtil.readAttribute(reader, ID));

        List<TransitionChromInfo> chromInfoList = new ArrayList<>();
        transition.setChromInfoList(chromInfoList);

        List<TransitionAnnotation> annotations = new ArrayList<>();
        transition.setAnnotations(annotations);

        while(reader.hasNext()) {

            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, TRANSITION))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new TransitionAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRODUCT_MZ))
            {
                Double productMz = XmlUtil.readDouble(reader, PRODUCT_MZ);
                if (productMz != null)
                {
                    transition.setMz(productMz);
                }
                // Should we blow up if productMz was null?
            }
            else if (XmlUtil.isStartElement(reader, evtType, COLLISION_ENERGY))
            {
                transition.setCollisionEnergy(XmlUtil.readDouble(reader, COLLISION_ENERGY));
            }
            else if (XmlUtil.isStartElement(reader, evtType, DECLUSTERING_POTENTIAL))
            {
                transition.setDeclusteringPotential(Double.parseDouble(reader.getElementText()));
            }
            else if(XmlUtil.isStartElement(reader, evtType, TRANSITION_PEAK))
            {
                TransitionChromInfo chromInfo = readTransitionChromInfo(reader);
                _transitionChromInfoCount++;
                if (!decoy)
                {
                    chromInfoList.add(chromInfo);
                }
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION_LIB_INFO))
            {
                transition.setRank(XmlUtil.readIntegerAttribute(reader, "rank"));
                transition.setIntensity(Double.parseDouble(reader.getAttributeValue(null, "intensity")));
                reader.nextTag();
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations, DataSettings.AnnotationTarget.transition);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new TransitionAnnotation());
        }

        return transition;
    }

    /** @param decoy if the transition is associated with a peptide or protein that's marked as a decoy and we can skip inserting its TransitionChromInfos*/
    private Transition readProteomicTransition(XMLStreamReader reader, boolean decoy) throws XMLStreamException
    {
        Transition transition = new Transition();
        readGeneralTransition(reader, transition);

        // read proteomics transition-specific attributes
        String calcNeutralMass = reader.getAttributeValue(null, "calc_neutral_mass");
        if(calcNeutralMass != null)
            transition.setNeutralMass(Double.parseDouble(calcNeutralMass));

        String neutralMassLoss = reader.getAttributeValue(null, "loss_neutral_mass");
        if(neutralMassLoss != null)
            transition.setNeutralLossMass(Double.parseDouble(neutralMassLoss));

        String fragmentOrdinal = reader.getAttributeValue(null, "fragment_ordinal");
        if(fragmentOrdinal != null)
            transition.setFragmentOrdinal(Integer.parseInt(fragmentOrdinal));

        String cleavageAa = reader.getAttributeValue(null, "cleavage_aa");
        if(cleavageAa != null)
            transition.setCleavageAa(cleavageAa);

        String decoyMassShift = reader.getAttributeValue(null, "decoy_mass_shift");
        if(decoyMassShift != null)
            transition.setDecoyMassShift(Double.parseDouble(decoyMassShift));

        transition.setMeasuredIonName(reader.getAttributeValue(null, "measured_ion_name"));

        List<TransitionChromInfo> chromInfoList = new ArrayList<>();
        transition.setChromInfoList(chromInfoList);

        List<TransitionAnnotation> annotations = new ArrayList<>();
        transition.setAnnotations(annotations);

        List<TransitionLoss> neutralLosses = new ArrayList<>();
        transition.setNeutralLosses(neutralLosses);

        ComplexFragmentIonName complexFragmentIonName;
        if (XmlUtil.readBooleanAttribute(reader, "orphaned_crosslink_ion", false)) {
            complexFragmentIonName = new ComplexFragmentIonName(null, 0);
        }
        else {
            complexFragmentIonName = new ComplexFragmentIonName(transition.getFragmentType(),
                    transition.getFragmentOrdinal());
        }

        while(reader.hasNext()) {

            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, TRANSITION))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new TransitionAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRECURSOR_MZ))
            {
                Double precursorMz = XmlUtil.readDouble(reader, PRECURSOR_MZ);
                if (precursorMz != null)
                {
                    transition.setPrecursorMz(precursorMz);
                }
                // Should we blow up if precursorMz was null?
            }
            else if (XmlUtil.isStartElement(reader, evtType, PRODUCT_MZ))
            {
                Double productMz = XmlUtil.readDouble(reader, PRODUCT_MZ);
                if (productMz != null)
                {
                    transition.setMz(productMz);
                }
                // Should we blow up if productMz was null?
            }
            else if (XmlUtil.isStartElement(reader, evtType, COLLISION_ENERGY))
            {
                transition.setCollisionEnergy(XmlUtil.readDouble(reader, COLLISION_ENERGY));
            }
            else if (XmlUtil.isStartElement(reader, evtType, DECLUSTERING_POTENTIAL))
            {
                transition.setDeclusteringPotential(Double.parseDouble(reader.getElementText()));
            }
            else if(XmlUtil.isStartElement(reader, evtType, TRANSITION_PEAK))
            {
                TransitionChromInfo chromInfo = readTransitionChromInfo(reader);
                _transitionChromInfoCount++;
                if (!decoy)
                {
                    chromInfoList.add(chromInfo);
                }
            }
            else if (XmlUtil.isStartElement(reader, evtType, LOSSES))
            {
                neutralLosses.addAll(readLosses(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, LINKED_FRAGMENT_ION)) {
                complexFragmentIonName.addChild( readLinkedFragmentIon(reader));
            }
            else if (XmlUtil.isStartElement(reader, evtType, TRANSITION_LIB_INFO))
            {
                transition.setRank(XmlUtil.readIntegerAttribute(reader, "rank"));
                transition.setIntensity(Double.parseDouble(reader.getAttributeValue(null, "intensity")));
                reader.nextTag();
            }
        }

        if (complexFragmentIonName.hasChildren())
        {
            transition.setComplexFragmentIon(complexFragmentIonName.toString());
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations, DataSettings.AnnotationTarget.transition);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new TransitionAnnotation());
        }

        return transition;
    }

    private void readGeneralTransition(XMLStreamReader reader, GeneralTransition transition)
    {
        String fragment = reader.getAttributeValue(null, "fragment_type");
        transition.setFragmentType(fragment);

        String charge =  reader.getAttributeValue(null, "product_charge");
        if(charge != null)
            transition.setCharge(Integer.parseInt(charge));

        String massIndex = reader.getAttributeValue(null, "mass_index");
        if(massIndex != null)
            transition.setMassIndex(Integer.parseInt(massIndex));

        if(transition.isPrecursorIon() && transition.getMassIndex() == null)
        {
            transition.setMassIndex(0);
        }

        String isotopeDistrRank = reader.getAttributeValue(null, "isotope_dist_rank");
        if(isotopeDistrRank != null)
            transition.setIsotopeDistRank(Integer.parseInt(isotopeDistrRank));

        String isotopeDistrProportion = reader.getAttributeValue(null, "isotope_dist_proportion");
        if(isotopeDistrProportion != null)
            transition.setIsotopeDistProportion(Double.parseDouble(isotopeDistrProportion));

        //are these supposed to be set here? Are the strings correct?
        String explicitCollisionEnergy = reader.getAttributeValue(null, "explicit_collision_energy");
        if(explicitCollisionEnergy != null)
            transition.setExplicitCollisionEnergy(Double.valueOf(explicitCollisionEnergy));

        String explicitSLens = reader.getAttributeValue(null, "explicit_s_lens");
        if (explicitSLens == null)
            // Fall back on older attribute name
            explicitSLens = reader.getAttributeValue(null, "s_lens");
        if(explicitSLens != null)
            transition.setExplicitSLens(Double.valueOf(explicitSLens));

        String explicitConeVoltage = reader.getAttributeValue(null, "explicit_cone_voltage");
        if (explicitConeVoltage == null)
            explicitConeVoltage = reader.getAttributeValue(null, "cone_voltage");
        if(explicitConeVoltage != null)
            transition.setExplicitConeVoltage(Double.valueOf(explicitConeVoltage));

        String explicitDeclusteringPotential = reader.getAttributeValue(null, "explicit_declustering_potential");
        if(explicitDeclusteringPotential != null)
            transition.setExplicitDeclusteringPotential(Double.valueOf(explicitDeclusteringPotential));

        String explicitIonMobilityHighEnergyOffsetMsec = reader.getAttributeValue(null, "explicit_ion_mobility_high_energy_offset");
        if (explicitIonMobilityHighEnergyOffsetMsec == null)
            explicitIonMobilityHighEnergyOffsetMsec = reader.getAttributeValue(null, "explicit_drift_time_high_energy_offset_msec");
        if(explicitIonMobilityHighEnergyOffsetMsec != null)
            transition.setExplicitIonMobilityHighEnergyOffset(Double.valueOf(explicitIonMobilityHighEnergyOffsetMsec));

        transition.setQuantitative(XmlUtil.readBooleanAttribute(reader, "quantitative"));
        transition.setCollisionEnergy(XmlUtil.readDoubleAttribute(reader, "collision_energy"));
        transition.setDeclusteringPotential(XmlUtil.readDoubleAttribute(reader, "declustering_potential"));

        _transitionCount++;
    }

    private List<TransitionLoss> readLosses(XMLStreamReader reader) throws XMLStreamException
    {
        List<TransitionLoss> result = new ArrayList<>();
        while (reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, LOSSES))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, NEUTRAL_LOSS))
            {
                TransitionLoss loss = new TransitionLoss();
                loss.setModificationName(XmlUtil.readAttribute(reader, "modification_name"));
                loss.setLossIndex(XmlUtil.readIntegerAttribute(reader, "loss_index", (loss.getModificationName() != null ? 0 : null)));
                loss.setFormula(XmlUtil.readAttribute(reader, "formula"));
                loss.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, "massdiff_monoisotopic"));
                loss.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, "massdiff_average"));
                result.add(loss);
            }
        }
        return result;
    }

    private <AnnotationTargetType extends AbstractAnnotation> AnnotationTargetType readAnnotation(XMLStreamReader reader, AnnotationTargetType annotation) throws XMLStreamException
    {
        annotation.setName(reader.getAttributeValue(null, "name"));
        StringBuilder value = new StringBuilder();
        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, ANNOTATION))
            {
                break;
            }
            if (XmlUtil.isText(evtType))
            {
                value.append(reader.getText());
            }
        }
        if (_dataSettings.isBooleanAnnotation(annotation.getName()))
        {
            // Boolean types are omitted if they're false, so consider it to be "true"
            annotation.setValue(Boolean.TRUE.toString());
        }
        else if (!value.isEmpty())
        {
            annotation.setValue(value.toString());
        }
        return annotation;
    }

    private <AnnotationTargetType extends AbstractAnnotation> void addMissingBooleanAnnotation(List<AnnotationTargetType> annotations,
                                                                                 String missingAnotName,
                                                                                 AnnotationTargetType annotation)
    {
        annotation.setName(missingAnotName);
        annotation.setValue(Boolean.FALSE.toString());
        annotations.add(annotation);
    }

    private TransitionChromInfo readTransitionChromInfo(XMLStreamReader reader) throws XMLStreamException
    {
        TransitionChromInfo chromInfo = new TransitionChromInfo();
        List<TransitionChromInfoAnnotation> annotations = new ArrayList<>();
        chromInfo.setAnnotations(annotations);

        chromInfo.setReplicateName(XmlUtil.readRequiredAttribute(reader, "replicate", TRANSITION_PEAK));
        chromInfo.setSkylineSampleFileId(getSkylineSampleFileId(reader, chromInfo.getReplicateName()));
        chromInfo.setOptimizationStep(XmlUtil.readIntegerAttribute(reader, "step"));
        chromInfo.setRetentionTime(XmlUtil.readDoubleAttribute(reader, "retention_time"));
        chromInfo.setStartTime(XmlUtil.readDoubleAttribute(reader, "start_time"));
        chromInfo.setEndTime(XmlUtil.readDoubleAttribute(reader, "end_time"));
        chromInfo.setArea(XmlUtil.readDoubleAttribute(reader, "area"));
        chromInfo.setBackground(XmlUtil.readDoubleAttribute(reader, "background"));
        chromInfo.setHeight(XmlUtil.readDoubleAttribute(reader, "height"));
        chromInfo.setMassErrorPPM(XmlUtil.readDoubleAttribute(reader, "mass_error_ppm"));
        Double fwhm =  XmlUtil.readDoubleAttribute(reader, "fwhm");
        // TODO: Found NaN value for fwhm in Study7.sky.  Should this happen?
        fwhm = (fwhm != null && fwhm.isNaN()) ? null : fwhm;
        chromInfo.setFwhm(fwhm);
        chromInfo.setFwhmDegenerate(XmlUtil.readBooleanAttribute(reader, "fwhm_degenerate"));
        chromInfo.setTruncated(XmlUtil.readBooleanAttribute(reader, "truncated"));
        chromInfo.setIdentified(XmlUtil.readAttribute(reader, "identified"));
        chromInfo.setPeakRank(XmlUtil.readIntegerAttribute(reader, "rank"));
        chromInfo.setUserSet(XmlUtil.readAttribute(reader, "user_set"));
        chromInfo.setPointsAcrossPeak(XmlUtil.readIntegerAttribute(reader, "points_across", null));
        chromInfo.setCcs(XmlUtil.readDoubleAttribute(reader, "ccs"));

        Double driftTime = XmlUtil.readDoubleAttribute(reader, "drift_time");
        Double driftTimeWindow = XmlUtil.readDoubleAttribute(reader, "drift_time_window");
        if (driftTime != null || driftTimeWindow != null)
        {
            // Support old Skyline documents
            chromInfo.setIonMobility(driftTime);
            chromInfo.setIonMobilityWindow(driftTimeWindow);
            chromInfo.setIonMobilityType("drift_time_ms");
        }
        else
        {
            chromInfo.setIonMobility(XmlUtil.readDoubleAttribute(reader, "ion_mobility"));
            chromInfo.setIonMobilityWindow(XmlUtil.readDoubleAttribute(reader, "ion_mobility_window"));
            chromInfo.setIonMobilityType(XmlUtil.readAttribute(reader, "ion_mobility_type"));
        }

        chromInfo.setRank(XmlUtil.readIntegerAttribute(reader, "rank"));
        chromInfo.setRankByLevel(XmlUtil.readIntegerAttribute(reader, "rank_by_level"));
        chromInfo.setForcedIntegration(XmlUtil.readBooleanAttribute(reader, "forced_integration"));

        chromInfo.setSkewness(XmlUtil.readDoubleAttribute(reader, "skewness"));
        chromInfo.setKurtosis(XmlUtil.readDoubleAttribute(reader, "kurtosis"));
        chromInfo.setStdDev(XmlUtil.readDoubleAttribute(reader, "std_dev"));
        chromInfo.setShapeCorrelation(XmlUtil.readDoubleAttribute(reader, "shape_correlation"));

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, TRANSITION_PEAK))
            {
                break;
            }
            else if (XmlUtil.isStartElement(reader, evtType, ANNOTATION))
            {
                annotations.add(readAnnotation(reader, new TransitionChromInfoAnnotation()));
            }
            else if (XmlUtil.isStartElement(reader, evtType, NOTE))
            {
                chromInfo.setNote(readNote(reader));
            }
        }

        // Boolean type annotations are not listed in the .sky file if their value was false.
        // We would still like to store them in the database.
        List<String> missingBooleanAnnotations = _dataSettings.getMissingBooleanAnnotations(annotations,
                                                                                            DataSettings.AnnotationTarget.transition_result);
        for(String missingAnotName: missingBooleanAnnotations)
        {
            addMissingBooleanAnnotation(annotations, missingAnotName, new TransitionChromInfoAnnotation());
        }

        return chromInfo;
    }

    private List<TransitionChromInfo> makeTransitionChromInfos(
            SkylineDocument.SkylineDocumentProto.TransitionResults transitionResults) {
        List<TransitionChromInfo> list = new ArrayList<>();

        for (SkylineDocument.SkylineDocumentProto.TransitionPeak transitionPeak : transitionResults.getPeaksList()) {
            TransitionChromInfo chromInfo = new TransitionChromInfo();
            _transitionChromInfoCount++;

            List<TransitionChromInfoAnnotation> annotations = new ArrayList<>();
            chromInfo.setAnnotations(annotations);
            SkylineReplicate replicate = _replicateList.get(transitionPeak.getReplicateIndex());
            chromInfo.setSampleFileId(replicate.getId());
            chromInfo.setReplicateName(replicate.getName());
            chromInfo.setSkylineSampleFileId(replicate.getSampleFileList().get(transitionPeak.getFileIndexInReplicate()).getSkylineId());
            if (0 != transitionPeak.getOptimizationStep()) {
                chromInfo.setOptimizationStep(transitionPeak.getOptimizationStep());
            }
            chromInfo.setRetentionTime((double) transitionPeak.getRetentionTime());
            chromInfo.setStartTime((double) transitionPeak.getStartRetentionTime());
            chromInfo.setEndTime((double) transitionPeak.getEndRetentionTime());
            chromInfo.setArea((double) transitionPeak.getArea());
            chromInfo.setBackground((double) transitionPeak.getBackgroundArea());
            chromInfo.setHeight((double) transitionPeak.getHeight());
            chromInfo.setMassErrorPPM((double) transitionPeak.getMassError().getValue());
            chromInfo.setFwhm((double) transitionPeak.getFwhm());
            chromInfo.setFwhmDegenerate(transitionPeak.getIsFwhmDegenerate());
            chromInfo.setTruncated(fromOptional(transitionPeak.getTruncated()));
            chromInfo.setIdentified(peakIdentificationToString(transitionPeak.getIdentified()));
            chromInfo.setPeakRank(transitionPeak.getRank());
            chromInfo.setUserSet(userSetToString(transitionPeak.getUserSet()));
            chromInfo.setPointsAcrossPeak(fromOptional(transitionPeak.getPointsAcrossPeak()));

            chromInfo.setSkewness((double) transitionPeak.getPeakShapeValues().getSkewness());
            chromInfo.setKurtosis((double) transitionPeak.getPeakShapeValues().getKurtosis());
            chromInfo.setStdDev((double) transitionPeak.getPeakShapeValues().getStdDev());
            chromInfo.setShapeCorrelation((double) transitionPeak.getPeakShapeValues().getShapeCorrelation());

            if (!StringUtils.isEmpty(transitionPeak.getAnnotations().getNote())) {
                chromInfo.setNote(transitionPeak.getAnnotations().getNote());
            }
            for (SkylineDocument.SkylineDocumentProto.AnnotationValue annotationValue : transitionPeak.getAnnotations().getValuesList()) {
                annotations.add(readAnnotationValue(annotationValue, new TransitionChromInfoAnnotation()));
            }

            for(String missingAnotName: _dataSettings.getMissingBooleanAnnotations(annotations,
                    DataSettings.AnnotationTarget.transition_result))
            {
                addMissingBooleanAnnotation(annotations, missingAnotName, new TransitionChromInfoAnnotation());
            }
            list.add(chromInfo);
        }
        return list;
    }

    private <AnnotationTargetType extends AbstractAnnotation> AnnotationTargetType readAnnotationValue(SkylineDocument.SkylineDocumentProto.AnnotationValue annotationValue, AnnotationTargetType annotation) {
        annotation.setName(annotationValue.getName());
        if (_dataSettings.isBooleanAnnotation(annotation.getName())) {
            annotation.setValue(Boolean.TRUE.toString());
        } else {
            annotation.setValue(Boolean.FALSE.toString());
        }
        return annotation;
    }

    private TransitionLoss toTransitionLoss(SkylineDocument.SkylineDocumentProto.TransitionLoss transitionLossProto) {
        TransitionLoss transitionLoss = new TransitionLoss();
        if (StringUtils.isEmpty(transitionLossProto.getModificationName())) {
            transitionLoss.setFormula(transitionLossProto.getFormula());
            transitionLoss.setMassDiffMono(transitionLossProto.getMonoisotopicMass());
            transitionLoss.setMassDiffAvg(transitionLossProto.getAverageMass());
        }
        else {
            transitionLoss.setModificationName(transitionLossProto.getModificationName());
            transitionLoss.setLossIndex(transitionLossProto.getLossIndex());
        }
        return transitionLoss;
    }

    private static Boolean fromOptional(SkylineDocument.SkylineDocumentProto.OptionalBool optionalBool) {
        return switch (optionalBool)
                {
                    case OPTIONAL_BOOL_MISSING -> null;
                    case OPTIONAL_BOOL_TRUE -> true;
                    case OPTIONAL_BOOL_FALSE -> false;
                    default -> null;
                };
    }

    private static Integer fromOptional(SkylineDocument.SkylineDocumentProto.OptionalInt optionalInt) {
        return optionalInt == null ? null : optionalInt.getValue();
    }

    private static Double fromOptional(com.google.protobuf.DoubleValue optionalDouble) {
        return optionalDouble == null ? null : optionalDouble.getValue();
    }

    private static Double fromOptional(com.google.protobuf.FloatValue optionalFloat) {
        return optionalFloat == null ? null : Double.valueOf(optionalFloat.getValue());
    }

    private static String fromOptional(com.google.protobuf.StringValue optionalString) {
        return optionalString == null ? null : optionalString.getValue();
    }

    private static String peakIdentificationToString(SkylineDocument.SkylineDocumentProto.PeakIdentification peakIdentification) {
        return switch (peakIdentification)
                {
                    case PEAK_IDENTIFICATION_ALIGNED -> "ALIGNED";
                    case PEAK_IDENTIFICATION_TRUE -> "TRUE";
                    case PEAK_IDENTIFICATION_FALSE -> "FALSE";
                    default -> null;
                };
    }

    private static String ionTypeToString(SkylineDocument.SkylineDocumentProto.IonType ionType) {
        return switch (ionType)
                {
                    case ION_TYPE_a -> IonType.a.name();
                    case ION_TYPE_b -> IonType.b.name();
                    case ION_TYPE_c -> IonType.c.name();
                    case ION_TYPE_x -> IonType.x.name();
                    case ION_TYPE_y -> IonType.y.name();
                    case ION_TYPE_z -> IonType.z.name();
                    case ION_TYPE_zH -> IonType.zh.name();
                    case ION_TYPE_zHH -> IonType.zhh.name();
                    case ION_TYPE_precursor -> IonType.precursor.name();
                    case ION_TYPE_custom -> IonType.custom.name();
                    default -> null;
                };
    }

    private static String userSetToString(SkylineDocument.SkylineDocumentProto.UserSet userSet) {
        return switch (userSet)
                {
                    case USER_SET_TRUE -> "TRUE";
                    case USER_SET_FALSE -> "FALSE";
                    case USER_SET_IMPORTED -> "IMPORTED";
                    case USER_SET_REINTEGRATED -> "REINTEGRATED";
                    case USER_SET_MATCHED -> "MATCHED";
                    default -> null;
                };
    }

    private int findEntry(SignedMz precursorMz, double tolerance, ChromGroupHeaderInfo[] chromatograms, int left, int right)
    {
        // Binary search for the right precursorMz
        if (left > right)
            return -1;
        int mid = (left + right) / 2;
        int compare = compareMz(precursorMz, chromatograms[mid].getPrecursor(), tolerance);
        if (compare < 0)
            return findEntry(precursorMz, tolerance, chromatograms, left, mid - 1);
        if (compare > 0)
            return findEntry(precursorMz, tolerance, chromatograms, mid + 1, right);

        // Scan backward until the first matching element is found.
        while (mid > 0 && matchMz(precursorMz, chromatograms[mid - 1].getPrecursor(), tolerance))
            mid--;

        return mid;
    }

    private static boolean matchMz(SignedMz mz1, SignedMz mz2, double tolerance)
    {
        return compareMz(mz1, mz2, tolerance) == 0;
    }

    private static int compareMz(SignedMz precursorMz1, SignedMz precursorMz2, double tolerance)
    {
        return precursorMz1.compareTolerant(precursorMz2, tolerance);
    }

    public List<SampleFileChromInfo> getSampleFileChromInfos(Map<String, SampleFile> pathToSampleFile)
    {
        List<SampleFileChromInfo> result = new ArrayList<>();
        if (_binaryParser == null)
        {
            return Collections.emptyList();
        }
        for (ChromGroupHeaderInfo chromatogram : _binaryParser.getChromatograms())
        {
            // Sample-scoped chromatograms have a magic precursor MZ value
            if (chromatogram.getPrecursorMz() == 0.0)
            {
                String path = _binaryParser.getFilePath(chromatogram);
                SampleFile sampleFile = pathToSampleFile.get(path);

                // Issue 40959 - Try stripping off the URI parameters if we don't have a match
                if (sampleFile == null)
                {
                    int queryParamIdx = path.lastIndexOf('?');
                    if (queryParamIdx != -1)
                    {
                        sampleFile = pathToSampleFile.get(path.substring(0, queryParamIdx));
                    }
                }

                if (sampleFile == null)
                {
                    incrementMissingChromatograms(path, "Unable to resolve " + path + " to SampleFile, will not import its sample-scoped chromatogram");
                }
                else
                {
                    SampleFileChromInfo info = new SampleFileChromInfo(_container);
                    info.setSampleFileId(sampleFile.getId());
                    info.setStartTime(chromatogram.getStartTime());
                    info.setEndTime(chromatogram.getEndTime());
                    ChromatogramGroupId chromatogramGroupId = _binaryParser.getTextId(chromatogram);
                    if (chromatogramGroupId != null)
                    {
                        info.setTextId(chromatogramGroupId.getQcTraceName());
                    }
                    info.setChromatogramFormat(chromatogram.getChromatogramBinaryFormat().ordinal());
                    info.setChromatogramOffset(chromatogram.getLocationPoints());
                    info.setChromatogramLength(chromatogram.getCompressedSize());
                    info.setNumPoints(chromatogram.getNumPoints());
                    info.setUncompressedSize(chromatogram.getUncompressedSize());
                    info.setFlags(Short.toUnsignedInt(chromatogram.getFlagBits()));

                    result.add(info);
                }
            }
        }
        return result;
    }

    private List<ChromGroupHeaderInfo> tryLoadChromatogram(
            List<? extends GeneralTransition> transitions,
            GeneralMolecule molecule,
            GeneralPrecursor<?> precursor,
            double tolerance)
    {
        // Add precursor matches to a list, if they match at least 1 transition
        // in this group, and are potentially the maximal transition match.

        // Using only the maximum works well for the case where there are 2
        // precursors in the same document that match a single entry.
        // TODO: But it messes up when there are 2 sets of transitions for
        //       the same precursor covering different numbers of transitions.
        //       Skyline never creates this case, but it has been reported
        // int maxTranMatch = 1;

        if (_binaryParser != null && _binaryParser.getChromatograms() != null)
        {
            // ChromatogramCache.TryLoadChromInfo() in Skyline code:
            // Filter the list of chromatograms based on our precursor mZ
            int i = findEntry(precursor.getSignedMz(), tolerance, _binaryParser.getChromatograms(), 0, _binaryParser.getChromatograms().length - 1);
            if (i == -1)
            {
                return Collections.emptyList();
            }

            Double explicitRT = molecule.getExplicitRetentionTime();

            // Add entries to a list until they no longer match
            List<ChromGroupHeaderInfo> listChromatograms = new ArrayList<>();
            while (i < _binaryParser.getChromatograms().length &&
                    matchMz(precursor.getSignedMz(), _binaryParser.getChromatograms()[i].getPrecursor(), tolerance))
            {
                ChromGroupHeaderInfo chrom = _binaryParser.getChromatograms()[i++];
                // Sequence matching for extracted chromatogram data added in v1.5
                ChromatogramGroupId chromTextId = _binaryParser.getTextId(chrom);
                if (chromTextId != null) 
                {
                    if (!molecule.targetMatches(chromTextId.getTarget()))
                        continue;
                    try
                    {
                        SpectrumFilter spectrumFilter = SpectrumFilter.fromByteArray(precursor.getSpectrumFilter());
                        if (!Objects.equals(spectrumFilter, chromTextId.getSpectrumFilter())) 
                        {
                            continue;
                        }
                    }
                    catch (InvalidProtocolBufferException e)
                    {
                        _log.warn("Error parsing spectrum filter {}", e);
                        return Collections.emptyList();
                    }
                }

                // If explicit retention time info is available, use that to discard obvious mismatches
                if (explicitRT == null || !chrom.excludesTime(explicitRT))
                {
                    listChromatograms.add(chrom);
                }
            }

            // MeasuredResults.TryLoadChromatogram in Skyline code:
            // Since we are reading and returning chromatograms for all replicates we need to maintain
            // the number of maximum transition matches for each replicate.
            // MeasuredResults.TryLoadChromatogram in Skyline reads and returns chromatograms for a single replicate.
            int[] maxTranMatches = new int[_binaryParser.getCacheFileSize()];

            ChromGroupHeaderInfo[] chromArray = new ChromGroupHeaderInfo[_binaryParser.getCacheFileSize()];

            for (ChromGroupHeaderInfo chromInfo : listChromatograms)
            {
                // If the chromatogram set has an optimization function then the number
                // of matching chromatograms per transition is a reflection of better
                // matching.  Otherwise, we only expect one match per transition.
                // TODO - do we need this on the Java side?
                boolean multiMatch = false;//chromatogram.OptimizationFunction != null;

                int tranMatch = _binaryParser.matchTransitions(chromInfo, transitions, explicitRT, tolerance, multiMatch);

                int fileIndex = chromInfo.getFileIndex();
                int maxTranMatch = maxTranMatches[fileIndex];

                if (tranMatch >= maxTranMatch)
                {
                    // If new maximum, clear anything collected at the previous maximum
                    if (tranMatch > maxTranMatch)
                    {
                        chromArray[fileIndex] = null;
                    }

                    maxTranMatches[fileIndex] = tranMatch;

                    if(chromArray[fileIndex] != null)
                    {
                        // If more than one value was found, ensure that there
                        // is only one precursor match per file.
                        // Use the entry with the m/z closest to the target
                        ChromGroupHeaderInfo currentChromForFileIndex = chromArray[fileIndex];
                        // Use the entry with the m/z closest to the target
                        if (Math.abs(precursor.getMz() - chromInfo.getPrecursorMz()) <
                                Math.abs(precursor.getMz() - currentChromForFileIndex.getPrecursorMz()))
                        {
                            chromArray[fileIndex] = chromInfo;
                        }
                    }
                    else
                    {
                        chromArray[fileIndex] = chromInfo;
                    }
                }
            }

            List<ChromGroupHeaderInfo> finalList = new ArrayList<>();
            for (ChromGroupHeaderInfo info : chromArray)
            {
                if (info != null)
                {
                    finalList.add(info);
                }
            }
            return finalList;
        }

        return Collections.emptyList();
    }

    public int getPeptideGroupCount()
    {
        return _peptideGroupCount;
    }

    public int getPeptideCount()
    {
        return _peptideCount;
    }

    public int getSmallMoleculeCount()
    {
        return _smallMoleculeCount;
    }

    public int getPrecursorCount()
    {
        return _precursorCount;
    }

    public int getTransitionCount()
    {
        return _transitionCount;
    }

    public int getTransitionChromInfoCount()
    {
        return _transitionChromInfoCount;
    }

    public int getReplicateCount()
    {
        return _replicateCount;
    }

    public int getListCount()
    {
        return _listCount;
    }

    private void updateProgress()
    {
        if(_progressStatus != null)
        {
            long bytesRead = _inputStream != null ? _inputStream.getBytesRead() : 0;
            _progressStatus.updateProgress(bytesRead, _fileSize);
        }
    }

    private static class ProgressInputStream extends FilterInputStream
    {
        private long _bytesRead = 0;

        protected ProgressInputStream(InputStream in)
        {
            super(in);
        }

        @Override
        public int read() throws IOException
        {
            int read = super.read();
            _bytesRead += read;
            return read;
        }

        @Override
        public int read(byte @NotNull [] b) throws IOException
        {
            int read = super.read(b);
            _bytesRead += read;
            return read;
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) throws IOException
        {
            int read = super.read(b, off, len);
            _bytesRead += read;
            return read;
        }

        @Override
        public long skip(long n) throws IOException
        {
            long skipped = super.skip(n);
            _bytesRead += skipped;
            return skipped;
        }

        long getBytesRead()
        {
            return _bytesRead;
        }
    }

    private Pair<ComplexFragmentIonName.ModificationSite, ComplexFragmentIonName> readLinkedFragmentIon(
            XMLStreamReader reader) throws XMLStreamException
    {
        ComplexFragmentIonName linkedIon;
        String strFragmentType = reader.getAttributeValue(null, "fragment_type");
        if (strFragmentType == null)
        {
            // blank fragment type means orphaned fragment ion
            linkedIon = new ComplexFragmentIonName(null, 0);
        }
        else
        {
            linkedIon = new ComplexFragmentIonName(strFragmentType, XmlUtil.readIntegerAttribute(reader, "fragment_ordinal"));
        }

        ComplexFragmentIonName.ModificationSite modificationSite = null;
        String modificationSiteName = reader.getAttributeValue(null, "modification_name");
        if (modificationSiteName != null) {
            modificationSite = new ComplexFragmentIonName.ModificationSite(XmlUtil.readIntegerAttribute(reader, "index_aa"),
                    modificationSiteName);
        }
        while(reader.hasNext()) {

            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, LINKED_FRAGMENT_ION))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, LOSSES))
            {
                var losses = readLosses(reader);
                if (losses != null)
                {
                    for  (TransitionLoss loss : losses)
                    {
                        // TODO
                    }
                }
            }
            else if (XmlUtil.isStartElement(reader, evtType, LINKED_FRAGMENT_ION))
            {
                linkedIon.addChild(readLinkedFragmentIon(reader));
            }
            reader.next();
        }

        return Pair.of(modificationSite, linkedIon);

    }
}
