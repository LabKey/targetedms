/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

package org.labkey.targetedms;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.AbstractFileXarSource;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.XarSource;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.pipeline.TargetedMSImportPipelineJob;
import org.labkey.targetedms.query.RepresentativeStateManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_FOLDER_TYPE;

public class TargetedMSManager
{
    private static final TargetedMSManager _instance = new TargetedMSManager();

    private static Logger _log = Logger.getLogger(TargetedMSManager.class);

    private TargetedMSManager()
    {
        // prevent external construction with a private default constructor
    }

    public static TargetedMSManager get()
    {
        return _instance;
    }

    public String getSchemaName()
    {
        return TargetedMSSchema.SCHEMA_NAME;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(TargetedMSSchema.SCHEMA_NAME);
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoRuns()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUNS);
    }

    public static TableInfo getTableInfoTransInstrumentSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_INSTRUMENT_SETTINGS);
    }

    public static TableInfo getTableInfoPredictor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PREDICTOR);
    }

    public static TableInfo getTableInfoPredictorSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PREDICTOR_SETTINGS);
    }

    public static TableInfo getTableInfoReplicate()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE);
    }

    public static TableInfo getTableInfoSampleFile()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SAMPLE_FILE);
    }

    public static TableInfo getTableInfoReplicateAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_CHROM_INFO);
    }

    public static TableInfo getTableInfoTransitionAreaRatio()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_AREA_RATIO);
    }

    public static TableInfo getTableInfoPeptideGroup()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
    }

    public static TableInfo getTableInfoPeptideGroupAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP_ANNOTATION);
    }

    public static TableInfo getTableInfoPeptide()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE);
    }

    public static TableInfo getTableInfoPeptideAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_ANNOTATION);
    }

    public static TableInfo getTableInfoProtein()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PROTEIN);
    }

    public static TableInfo getTableInfoPrecursor()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR);
    }

    public static TableInfo getTableInfoPrecursorAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_ANNOTATION);
    }

    public static TableInfo getTableInfoPrecursorChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO);
    }

    public static TableInfo getTableInfoPrecursorAreaRatio()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_AREA_RATIO);
    }

    public static TableInfo getTableInfoPrecursorChromInfoAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_CHROM_INFO_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionChromInfoAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_CHROM_INFO_ANNOTATION);
    }

    public static TableInfo getTableInfoPeptideChromInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_CHROM_INFO);
    }

    public static TableInfo getTableInfoPeptideAreaRatio()
    {
       return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_AREA_RATIO);
    }

    public static TableInfo getTableInfoInstrument()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_INSTRUMENT);
    }

    public static TableInfo getTableInfoIsotopeEnrichment()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_ENRICHMENT);
    }

    public static TableInfo getTableInfoRetentionTimePredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RETENTION_TIME_PREDICTION_SETTINGS);
    }

    public static TableInfo getTableInfoTransitionPredictionSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_PREDICITION_SETTINGS);
    }

    public static TableInfo getTableInfoTransitionFullScanSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_FULL_SCAN_SETTINGS);
    }

    public static TableInfo getTableInfoTransition()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION);
    }

    public static TableInfo getTableInfoTransitionLoss()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_LOSS);
    }

    public static TableInfo getTableInfoModificationSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_MODIFICATION_SETTINGS);
    }

    public static TableInfo getTableInfoPeptideStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoPeptideIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PEPTIDE_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoIsotopeLabel()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_LABEL);
    }

    public static TableInfo getTableInfoIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoStructuralModLoss()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_STRUCTURAL_MOD_LOSS);
    }

    public static TableInfo getTableInfoRunIsotopeModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_ISOTOPE_MODIFICATION);
    }

    public static TableInfo getTableInfoRunEnzyme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_ENZYME);
    }

    public static TableInfo getTableInfoRunStructuralModification()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_RUN_STRUCTURAL_MODIFICATION);
    }

    public static TableInfo getTableInfoTransitionAnnotation()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_ANNOTATION);
    }

    public static TableInfo getTableInfoTransitionOptimization()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_TRANSITION_OPTIMIZATION);
    }

    public static TableInfo getTableInfoLibrarySettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIBRARY_SETTINGS);
    }

    public static TableInfo getTableInfoSpectrumLibrary()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_SPECTRUM_LIBRARY);
    }

    public static TableInfo getTableInfoEnzyme()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ENZYME);
    }

    public static TableInfo getTableInfoLibrarySource()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_LIBRARY_SOURCE);
    }

    public static TableInfo getTableInfoPrecursorLibInfo()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_PRECURSOR_LIB_INFO);
    }

    public static TableInfo getTableInfoAnnotationSettings()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_ANNOTATION_SETTINGS);
    }

    public static TableInfo getTableInfoiRTPeptide()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_IRT_PEPTIDE);
    }

    public static TableInfo getTableInfoiRTScale()
    {
        return getSchema().getTable(TargetedMSSchema.TABLE_IRT_SCALE);
    }

    public static int addRunToQueue(ViewBackgroundInfo info,
                                     final File file,
                                     PipeRoot root) throws SQLException, IOException, XarFormatException
    {
        String description = "Skyline document import - " + file.getName();
        XarContext xarContext = new XarContext(description, info.getContainer(), info.getUser());
        User user =  info.getUser();
        Container container = info.getContainer();

        // If an entry does not already exist for this data file in exp.data create it now.
		// This should happen only if a file was copied to the pipeline directory instead
		// of being uploaded via the files browser.
        ExpData expData = ExperimentService.get().getExpDataByURL(file, container);
        if(expData == null)
        {
            XarSource source = new AbstractFileXarSource("Wrap Targeted MS Run", container, user)
            {
                public File getLogFile() throws IOException
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public File getRoot()
                {
                    return file.getParentFile();
                }

                @Override
                public ExperimentArchiveDocument getDocument() throws XmlException, IOException
                {
                    throw new UnsupportedOperationException();
                }
            };

            expData = ExperimentService.get().createData(file.toURI(), source);
        }

        TargetedMSModule.FolderType folderType = TargetedMSManager.getFolderType(container);
        // Default folder type or Experiment is not representative
        TargetedMSRun.RepresentativeDataState representative = TargetedMSRun.RepresentativeDataState.NotRepresentative;
        if (folderType == TargetedMSModule.FolderType.Library)
            representative = TargetedMSRun.RepresentativeDataState.Representative_Peptide;
        else if (folderType == TargetedMSModule.FolderType.LibraryProtein)
            representative = TargetedMSRun.RepresentativeDataState.Representative_Protein;

        SkylineDocImporter importer = new SkylineDocImporter(user, container, file.getName(), expData, null, xarContext, representative);
        SkylineDocImporter.RunInfo runInfo = importer.prepareRun();
        TargetedMSImportPipelineJob job = new TargetedMSImportPipelineJob(info, expData, runInfo, root, representative);
        try
        {
            PipelineService.get().queueJob(job);
            return PipelineService.get().getJobId(user, container, job.getJobGUID());
        }
        catch (PipelineValidationException e)
        {
            throw new IOException(e);
        }
    }

    public static ExpRun ensureWrapped(TargetedMSRun run, User user) throws ExperimentException
    {
        ExpRun expRun;
        if (run.getExperimentRunLSID() != null)
        {
            expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
            if (expRun != null && expRun.getContainer().equals(run.getContainer()))
            {
                return expRun;
            }
        }
        return wrapRun(run, user);
    }

    private static ExpRun wrapRun(TargetedMSRun run, User user) throws ExperimentException
    {
        try
        {
            ExperimentService.get().getSchema().getScope().ensureTransaction();

            Container container = run.getContainer();

            // Make sure that we have a protocol in this folder
            String protocolPrefix = run.isZipFile() ? TargetedMSModule.IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX :
                                                      TargetedMSModule.IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX;

            Lsid lsid = new Lsid("Protocol.Folder-" + container.getRowId(), protocolPrefix);
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(lsid.toString());
            if (protocol == null)
            {
                protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ProtocolApplication, "Skyline Document Import", lsid.toString());
                protocol.setMaxInputMaterialPerInstance(0);
                protocol = ExperimentService.get().insertSimpleProtocol(protocol, user);
            }

            ExpData expData = ExperimentService.get().getExpData(run.getDataId());
            File skylineFile = expData.getFile();

            ExpRun expRun = ExperimentService.get().createExperimentRun(container, run.getDescription());
            expRun.setProtocol(protocol);
            expRun.setFilePathRoot(skylineFile.getParentFile());
            ViewBackgroundInfo info = new ViewBackgroundInfo(container, user, null);

            Map<ExpData, String> inputDatas = new HashMap<ExpData, String>();
            Map<ExpData, String> outputDatas = new HashMap<ExpData, String>();

            outputDatas.put(expData, "sky");

            expRun = ExperimentService.get().saveSimpleExperimentRun(expRun,
                                                                     Collections.<ExpMaterial, String>emptyMap(),
                                                                     inputDatas,
                                                                     Collections.<ExpMaterial, String>emptyMap(),
                                                                     outputDatas,
                                                                     Collections.<ExpData, String>emptyMap(),
                                                                     info, _log, false);

            run.setExperimentRunLSID(expRun.getLSID());
            TargetedMSManager.updateRun(run, user);

            ExperimentService.get().getSchema().getScope().commitTransaction();
            return expRun;
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
        finally
        {
            ExperimentService.get().getSchema().getScope().closeConnection();
        }
    }

    public static TargetedMSRun getRunByDataId(int dataId, Container c)
    {
        TargetedMSRun[] runs = getRuns("DataId = ? AND Deleted = ? AND Container = ?", dataId, Boolean.FALSE, c.getId());
        if(null == runs || runs.length == 0)
        {
            return null;
        }
        if(runs.length == 1)
        {
            return runs[0];
        }
        throw new IllegalStateException("There is more than one non-deleted Targeted MS Run for dataId " + dataId);
    }

    private static TargetedMSRun[] getRuns(String whereClause, Object... params)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append(whereClause);
        sql.addAll(params);
        return new SqlSelector(getSchema(), sql).getArray(TargetedMSRun.class);
    }

    public static TargetedMSRun getRun(int runId)
    {
        TargetedMSRun run = null;

        TargetedMSRun[] runs = getRuns("Id = ? AND deleted = ?", runId, false);

        if (runs != null && runs.length == 1)
        {
            run = runs[0];
        }

        return run;
    }

    public static boolean isRunConflicted(TargetedMSRun run)
    {
        if(run == null)
            return false;

        TargetedMSRun.RepresentativeDataState representativeState = run.getRepresentativeDataState();
        switch (representativeState)
        {
            case NotRepresentative:
                return false;
            case Representative_Protein:
                return getConflictedProteinCount(run.getId()) > 0;
            case Representative_Peptide:
                return getConflictedPeptideCount(run.getId()) > 0;
        }
        return false;
    }

    private static int getConflictedProteinCount(int runId)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(" WHERE runid = ?");
        sql.add(runId);
        sql.append(" AND representativedatastate = ?");
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null ? count : 0;
    }

    private static int getConflictedPeptideCount(int runId)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(*) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(" WHERE pepgrp.runid = ?");
        sql.add(runId);
        sql.append(" AND prec.representativedatastate = ?");
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        sql.append(" AND prec.PeptideId = pep.Id");
        sql.append(" AND pep.PeptideGroupId = pepgrp.Id");
        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null ? count : 0;
    }

    public static boolean hasConflictedProteins(Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(pepgrp.id) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE run.container = ?");
        sql.add(container.getId());
        sql.append(" AND run.id = pepgrp.runid");
        sql.append(" AND pepgrp.representativedatastate = ?");
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null ? count > 0 : false;
    }

    public static boolean hasConflictedPeptides(Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT COUNT(prec.id) FROM ");
        sql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE run.container = ?");
        sql.add(container.getId());
        sql.append(" AND prec.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Conflicted.ordinal());
        sql.append(" AND prec.PeptideId = pep.Id");
        sql.append(" AND pep.PeptideGroupId = pepgrp.Id");
        sql.append(" AND pepgrp.RunId = run.Id");

        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null ? count > 0 : false;
    }

    public static void markRunsNotRepresentative(Container container, TargetedMSRun.RepresentativeDataState representativeState)
    {
        Collection<Integer> representativeRunIds = null;

        if(representativeState == TargetedMSRun.RepresentativeDataState.Representative_Protein)
        {
            representativeRunIds = getProteinRepresentativeRunIds(container);
        }
        else if(representativeState == TargetedMSRun.RepresentativeDataState.Representative_Peptide)
        {
            representativeRunIds = getPeptideRepresentativeRunIds(container);
        }

        if(representativeRunIds == null || representativeRunIds.size() == 0)
        {
            return;
        }

        SQLFragment updateSql = new SQLFragment();
        updateSql.append("UPDATE "+TargetedMSManager.getTableInfoRuns());
        updateSql.append(" SET RepresentativeDataState = ?");
        updateSql.add(TargetedMSRun.RepresentativeDataState.NotRepresentative.ordinal());
        updateSql.append(" WHERE Container = ?");
        updateSql.add(container);
        updateSql.append(" AND RepresentativeDataState = ? ");
        updateSql.add(representativeState.ordinal());
        updateSql.append(" AND Id NOT IN ("+StringUtils.join(representativeRunIds, ",")+")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(updateSql);
    }

    public static List<Integer> getCurrentRepresentativeRunIds(Container container)
    {
        List<Integer> representativeRunIds = new ArrayList<Integer>();
        Collection<Integer> proteinRepresentativeRunIds = getCurrentProteinRepresentativeRunIds(container);
        if(proteinRepresentativeRunIds != null)
        {
            representativeRunIds.addAll(proteinRepresentativeRunIds);
        }
        Collection<Integer> peptideRepresentativeRunIds = getCurrentPeptideRepresentativeRunIds(container);
        if(peptideRepresentativeRunIds != null)
        {
            representativeRunIds.addAll(peptideRepresentativeRunIds);
        }
        return representativeRunIds;
    }

    private static Collection<Integer> getCurrentProteinRepresentativeRunIds(Container container)
    {
        return getProteinRepresentativeRunIds(container, new Integer[]{RepresentativeDataState.Representative.ordinal()});
    }

    private static Collection<Integer> getProteinRepresentativeRunIds(Container container)
    {
        return getProteinRepresentativeRunIds(container, new Integer[]{RepresentativeDataState.Representative.ordinal(),
                                                                       RepresentativeDataState.Deprecated.ordinal(),
                                                                       RepresentativeDataState.Conflicted.ordinal()});
    }

    private static Collection<Integer> getProteinRepresentativeRunIds(Container container, Integer[] stateArray)
    {
        SQLFragment reprRunIdSql = new SQLFragment();
        reprRunIdSql.append("SELECT DISTINCT (RunId) FROM ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoRuns(), "runs");
        String states = StringUtils.join(stateArray, ",");

        reprRunIdSql.append(" WHERE pepgrp.RepresentativeDataState IN (" + states + ")");
        reprRunIdSql.append(" AND runs.Container = ?");
        reprRunIdSql.add(container);
        reprRunIdSql.append(" AND runs.Id = pepgrp.RunId");

        return new SqlSelector(TargetedMSManager.getSchema(), reprRunIdSql).getCollection(Integer.class);
    }

    private static Collection<Integer> getCurrentPeptideRepresentativeRunIds(Container container)
    {
        return getPeptideRepresentativeRunIds(container, new Integer[]{RepresentativeDataState.Representative.ordinal()});
    }

    private static Collection<Integer> getPeptideRepresentativeRunIds(Container container)
    {
        return getPeptideRepresentativeRunIds(container, new Integer[]{RepresentativeDataState.Representative.ordinal(),
                                                                       RepresentativeDataState.Deprecated.ordinal(),
                                                                       RepresentativeDataState.Conflicted.ordinal()});
    }

    private static Collection<Integer> getPeptideRepresentativeRunIds(Container container, Integer[] stateArray)
    {
        // Get a list of runIds that have proteins that are either representative, deprecated or conflicted.
        SQLFragment reprRunIdSql = new SQLFragment();
        reprRunIdSql.append("SELECT DISTINCT (pepgrp.RunId) FROM ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoPrecursor(), "prec");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        reprRunIdSql.append(", ");
        reprRunIdSql.append(TargetedMSManager.getTableInfoRuns(), "runs");

        String states = StringUtils.join(stateArray, ",");

        reprRunIdSql.append(" WHERE prec.RepresentativeDataState IN (" + states + ")");
        reprRunIdSql.append(" AND prec.PeptideId = pep.Id");
        reprRunIdSql.append(" AND pep.PeptideGroupId = pepgrp.Id");
        reprRunIdSql.append(" AND Container = ?");
        reprRunIdSql.add(container);
        reprRunIdSql.append(" AND runs.Id = pepgrp.RunId");

        return new SqlSelector(TargetedMSManager.getSchema(), reprRunIdSql).getCollection(Integer.class);
    }

    public static void updateRun(TargetedMSRun run, User user) throws SQLException
    {
        Table.update(user, getTableInfoRuns(), run, run.getRunId());
    }

    // For safety, simply mark runs as deleted.  This allows them to be (manually) restored.
    // TODO: Do we really want to hang on to the data of a deleted run?
    private static void markAsDeleted(List<Integer> runIds, Container c, User user)
    {
        if (runIds.isEmpty())
            return;

        // Save these to delete after we've deleted the runs
        List<ExpRun> experimentRunsToDelete = new ArrayList<ExpRun>();

        for (Integer runId : runIds)
        {
            TargetedMSRun run = getRun(runId);
            if (run != null && run.getDataId() != null)
            {
                ExpData data = ExperimentService.get().getExpData(run.getDataId());
                if (data != null)
                {
                    ExpRun expRun = data.getRun();
                    if (expRun != null)
                    {
                        experimentRunsToDelete.add(expRun);
                    }
                }
            }
        }

        markDeleted(runIds, c, user);

        for (ExpRun run : experimentRunsToDelete)
        {
            run.delete(user);
        }
    }

    public static void markAsDeleted(Container c, User user)
    {
        List<Integer> runIds = new SqlSelector(getSchema(), "SELECT Run FROM " + getTableInfoRuns() + " WHERE Container = ?", c).getArrayList(Integer.class);
        markAsDeleted(runIds, c, user);
    }

    // pulled out into separate method so could be called by itself from data handlers
    public static void markDeleted(List<Integer> runIds, Container c, User user)
    {
        try
        {
            for(int runId: runIds)
            {
                TargetedMSRun run = getRun(runId);
                // Revert the representative state if any of the runs are representative at the protein or peptide level.
                if(run.isRepresentative())
                {
                    RepresentativeStateManager.setRepresentativeState(user, c, run, TargetedMSRun.RepresentativeDataState.NotRepresentative);
                }
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        SQLFragment markDeleted = new SQLFragment("UPDATE " + getTableInfoRuns() + " SET ExperimentRunLSID = NULL, Deleted=?, Modified=? ", Boolean.TRUE, new Date());
        SimpleFilter where = new SimpleFilter();
        where.addCondition(FieldKey.fromParts("Container"), c.getId());
        where.addInClause(FieldKey.fromParts("Id"), runIds);
        markDeleted.append(where.getSQLFragment(getSqlDialect()));

        new SqlExecutor(getSchema()).execute(markDeleted);
    }

    public static TargetedMSRun getRunForPrecursor(int precursorId)
    {
        String sql = "SELECT run.* FROM "+
                     getTableInfoRuns()+" AS run, "+
                     getTableInfoPeptideGroup()+" AS pg, "+
                     getTableInfoPeptide()+" AS pep, "+
                     getTableInfoPrecursor()+" AS pre "+
                     "WHERE run.Id=pg.RunId "+
                     "AND pg.Id=pep.PeptideGroupId "+
                     "AND pep.Id=pre.PeptideId "+
                     "AND pre.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(precursorId);

        TargetedMSRun run = new SqlSelector(getSchema(), sf).getObject(TargetedMSRun.class);
        if(run == null)
        {
            throw new NotFoundException("No run found for precursor: "+precursorId);
        }
        return run;
    }

    public static TargetedMSRun getRunForPeptide(int peptideId)
    {
        String sql = "SELECT run.* FROM "+
                     getTableInfoRuns()+" AS run, "+
                     getTableInfoPeptideGroup()+" AS pg, "+
                     getTableInfoPeptide()+" AS pep "+
                     "WHERE run.Id=pg.RunId "+
                     "AND pg.Id=pep.PeptideGroupId "+
                     "AND pep.Id=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(peptideId);

        TargetedMSRun run = new SqlSelector(getSchema(), sf).getObject(TargetedMSRun.class);
        if(run == null)
        {
            throw new NotFoundException("No run found for peptide: "+peptideId);
        }
        return run;
    }

    public static boolean runHasProteins(int runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM ");
        sql.append(getTableInfoPeptideGroup()+" pg ");
        sql.append("WHERE ");
        sql.append("pg.RunId=? ");
        sql.append("AND ");
        sql.append("pg.SequenceID IS NOT NULL");
        sql.add(runId);

        Integer count = new SqlSelector(getSchema(), sql).getObject(Integer.class);

        return count > 0;
    }

    /** Actually delete runs that have been marked as deleted from the database */
    public static void purgeDeletedRuns()
    {
        try
        {
            // Delete from TransitionChromInfoAnnotation
            deleteTransitionChromInfoDependent(getTableInfoTransitionChromInfoAnnotation());
            // Delete from TransitionAreaRatio
            deleteTransitionChromInfoDependent(getTableInfoTransitionAreaRatio());

            // Delete from PrecursorChromInfoAnnotation
            deletePrecursorChromInfoDependent(getTableInfoPrecursorChromInfoAnnotation());
            // Delete from PrecursorAreaRatio
            deletePrecursorChromInfoDependent(getTableInfoPrecursorAreaRatio());

            // Delete from PeptideAreaRatio
            deletePeptideChromInfoDependent(getTableInfoPeptideAreaRatio());

            // Delete from TransitionChromInfo
            deleteTransitionDependent(getTableInfoTransitionChromInfo());
            // Delete from TransitionAnnotation
            deleteTransitionDependent(getTableInfoTransitionAnnotation());
            // Delete from TransitionLoss
            deleteTransitionDependent(getTableInfoTransitionLoss());
            // Delete from TransitionOptimization
            deleteTransitionDependent(getTableInfoTransitionOptimization());

            // Delete from Transition
            deletePrecursorDependent(getTableInfoTransition());
            // Delete from PrecursorChromInfo
            deletePrecursorDependent(getTableInfoPrecursorChromInfo());
            // Delete from PrecursorAnnotation
            deletePrecursorDependent(getTableInfoPrecursorAnnotation());
            // Delete from PrecursorLibInfo
            deletePrecursorDependent(getTableInfoPrecursorLibInfo());


            // Delete from PeptideAnnotation
            deletePeptideDependent(getTableInfoPeptideAnnotation());
            // Delete from Precursor
            deletePeptideDependent(getTableInfoPrecursor());
            // Delete from PeptideChromInfo
            deletePeptideDependent(getTableInfoPeptideChromInfo());
            // Delete from PeptideStructuralModification
            deletePeptideDependent(getTableInfoPeptideStructuralModification());
            // Delete from PeptideIsotopeModification
            deletePeptideDependent(getTableInfoPeptideIsotopeModification());


            // Delete from Peptide
            deletePeptideGroupDependent(getTableInfoPeptide());
            // Delete from Protein
            deletePeptideGroupDependent(getTableInfoProtein());
            // Delete from PeptideGroupAnnotation
            deletePeptideGroupDependent(getTableInfoPeptideGroupAnnotation());


            // Delete from sampleFile
            deleteReplicateDependent(getTableInfoSampleFile());
			// Delete from ReplicateAnnotation
            deleteReplicateDependent(getTableInfoReplicateAnnotation());

            // Delete from PredictorSettings and Predictor
            deleteTransitionPredictionSettingsDependent();

            // Delete from PeptideGroup
            deleteRunDependent(getTableInfoPeptideGroup());
            // Delete from Replicate
            deleteRunDependent(getTableInfoReplicate());
            // Delete from TransitionInstrumentSettings
            deleteRunDependent(getTableInfoTransInstrumentSettings());
            // Delete from Instrument
            deleteRunDependent(getTableInfoInstrument());
            // Delete from RetentionTimePredictionSettings
            deleteRunDependent(getTableInfoRetentionTimePredictionSettings());
            // Delete from TransitionPredictionSettings
            deleteRunDependent(getTableInfoTransitionPredictionSettings());
            // Delete from TransitionFullScanSettings
            deleteRunDependent(getTableInfoTransitionFullScanSettings());
            // Delete from IsotopeEnrichment (part of Full Scan settings)
            deleteRunDependent(getTableInfoIsotopeEnrichment());
            // Delete from ModificationSettings
            deleteRunDependent(getTableInfoModificationSettings());
            // Delete from RunStructuralModification
            deleteRunDependent(getTableInfoRunStructuralModification());
            // Delete from RunIsotopeModification
            deleteRunDependent(getTableInfoRunIsotopeModification());
            // Delete from IsotopeLabel
            deleteRunDependent(getTableInfoIsotopeLabel());
            // Delete from LibrarySettings
            deleteRunDependent(getTableInfoLibrarySettings());
            // Delete from SpectrumLibrary
            deleteRunDependent(getTableInfoSpectrumLibrary());
            // Delete from RunEnzyme
            deleteRunDependent(getTableInfoRunEnzyme());
            // Delete from AnnotationSettings
            deleteRunDependent(getTableInfoAnnotationSettings());


            // Delete from runs
            Table.execute(getSchema(), "DELETE FROM " + getTableInfoRuns() + " WHERE Deleted = ?", true);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public static void deleteTransitionChromInfoDependent(TableInfo tableInfo) throws SQLException
    {
         Table.execute(getSchema(), "DELETE FROM " + tableInfo +
                    " WHERE TransitionChromInfoId IN (SELECT Id FROM " +
                    getTableInfoTransitionChromInfo() + " WHERE TransitionId IN (SELECT Id FROM " +
                    getTableInfoTransition() + " WHERE PrecursorId IN (SELECT Id FROM " +
                    getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?))))))", true);
    }

    public static void deletePrecursorChromInfoDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PrecursorChromInfoId IN (SELECT Id FROM "
                + getTableInfoPrecursorChromInfo() + " WHERE PrecursorId IN (SELECT Id FROM "
                + getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM "
                + getTableInfoPeptide() + " WHERE " +
                "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                getTableInfoRuns() + " WHERE Deleted = ?)))))", true);
    }

    public static void deletePeptideChromInfoDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PeptideChromInfoId IN (SELECT Id FROM "
                + getTableInfoPeptideChromInfo() + " WHERE PeptideId IN (SELECT Id FROM "
                + getTableInfoPeptide() + " WHERE " +
                "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                getTableInfoRuns() + " WHERE Deleted = ?))))", true);
    }

    public static void deleteTransitionDependent(TableInfo tableInfo) throws SQLException
    {
         Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE TransitionId IN (SELECT Id FROM " +
                    getTableInfoTransition() + " WHERE PrecursorId IN (SELECT Id FROM " +
                    getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?)))))", true);
    }

    private static void deletePrecursorDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PrecursorId IN (SELECT Id FROM " +
                    getTableInfoPrecursor() + " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?))))", true);
    }

    private static void deletePeptideDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE PeptideId IN (SELECT Id FROM "
                    + getTableInfoPeptide() + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?)))", true);
    }

    private static void deletePeptideGroupDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE " +
                    "PeptideGroupId IN (SELECT Id FROM " + getTableInfoPeptideGroup() + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void deleteRunDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo + " WHERE RunId IN (SELECT Id FROM " +
                    getTableInfoRuns() + " WHERE Deleted = ?)", true);
    }

    private static void deleteReplicateDependent(TableInfo tableInfo) throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + tableInfo+ " WHERE ReplicateId IN (SELECT Id FROM " +
                    getTableInfoReplicate() + " WHERE RunId IN (SELECT Id FROM " + getTableInfoRuns() + " WHERE Deleted = ?))", true);
    }

    private static void deleteTransitionPredictionSettingsDependent() throws SQLException
    {
        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPredictorSettings() + " WHERE PredictorId IN (SELECT Id FROM " +
                    getTableInfoPredictor() + " WHERE " +
                        "Id IN (SELECT CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)" +
                        "OR Id IN (SELECT DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?))"
                , true, true);

        Table.execute(getSchema(), "DELETE FROM " + getTableInfoPredictor() + " WHERE " +
                        "Id IN (SELECT CePredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)" +
                        "OR Id IN (SELECT DpPredictorId FROM " + getTableInfoTransitionPredictionSettings() + " tps, " + getTableInfoRuns() + " r WHERE r.Id = tps.RunId AND r.Deleted = ?)"
                , true, true);
    }

    // return the ModuleProperty value for "TARGETED_MS_FOLDER_TYPE"
    public static TargetedMSModule.FolderType getFolderType(Container c) {
        TargetedMSModule targetedMSModule = null;
        for (Module m : c.getActiveModules())
        {
            if (m instanceof TargetedMSModule)
            {
                targetedMSModule = (TargetedMSModule) m;
            }
        }
        if (targetedMSModule == null)
        {
            return null; // no TargetedMS module found - do nothing
        }
        ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(TARGETED_MS_FOLDER_TYPE);
        String svalue = moduleProperty.getValueContainerSpecific(c);
        try
        {
            return TargetedMSModule.FolderType.valueOf(svalue);
        }
        catch (IllegalArgumentException e)
        {
            // return undefined if the string does not match any type
            return TargetedMSModule.FolderType.Undefined;
        }
    }

    public static void renameRun(int runId, String newDescription)
    {
        if (newDescription == null || newDescription.length() == 0)
            return;

        try
        {
            Table.execute(getSchema(), "UPDATE " + getTableInfoRuns() + " SET Description=? WHERE Id = ?",
                    newDescription, runId);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

}