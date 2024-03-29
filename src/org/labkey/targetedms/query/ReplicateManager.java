/*
 * Copyright (c) 2012-2018 LabKey Corporation
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

package org.labkey.targetedms.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.QCMetricExclusion;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.ReplicateAnnotation;
import org.labkey.targetedms.parser.SampleFile;
import org.labkey.targetedms.parser.TransitionSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 1:54 PM
 */
public class ReplicateManager
{
    private ReplicateManager() {}

    public static SampleFile getSampleFile(long sampleFileId)
    {
        return new TableSelector(TargetedMSManager.getSchema().getTable(TargetedMSSchema.TABLE_SAMPLE_FILE))
            .getObject(sampleFileId, SampleFile.class);
    }

    public static Replicate getReplicate(long replicateId)
    {
        return new TableSelector(TargetedMSManager.getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE))
            .getObject(replicateId, Replicate.class);
    }

    public static Replicate getSampleReplicate(long sampleFileId, Container container)
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT rep.* FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoSampleFile(), "s");
        sqlFragment.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoReplicate(), "rep");
        sqlFragment.append(" ON s.replicateId = rep.Id");
        sqlFragment.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(" ON r.Id = rep.runId");
        sqlFragment.append(" WHERE s.Id=?").add(sampleFileId);
        sqlFragment.append(" AND r.Container=?").add(container);

        return new SqlSelector(TargetedMSManager.getSchema(), sqlFragment).getObject(Replicate.class);
    }


    /**
     * @return the distinct list of metric IDs that are set as exclusions for replicates of the supplied name.
     * Null indicates that there was an exclusion for all metrics for the replicate.
     */
    public static List<Integer> getReplicateExclusions(String name, Container container)
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT DISTINCT qme.MetricId FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoQCMetricExclusion(), "qme");
        sqlFragment.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoReplicate(), "rp");
        sqlFragment.append(" ON qme.ReplicateId = rp.Id");
        sqlFragment.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(" ON r.Id = rp.runId");
        sqlFragment.append(" WHERE rp.Name=?").add(name);
        sqlFragment.append(" AND r.Container=?").add(container);

        return new SqlSelector(TargetedMSManager.getSchema(), sqlFragment).getArrayList(Integer.class);
    }

    public static QCMetricExclusion insertReplicateExclusion(User user, Long replicateId, @Nullable Integer metricId)
    {
        // Note: null metricId indicates an exclusion for all metrics for the given replicate
        QCMetricExclusion exclusion = new QCMetricExclusion(replicateId, metricId);
        return Table.insert(user, TargetedMSManager.getTableInfoQCMetricExclusion(), exclusion);
    }

    public static ReplicateAnnotation getReplicateAnnotation(int annotationId)
    {
        return new TableSelector(TargetedMSManager.getSchema().getTable(TargetedMSSchema.TABLE_REPLICATE_ANNOTATION))
                .getObject(annotationId, ReplicateAnnotation.class);
    }

    public static ReplicateAnnotation getReplicateAnnotation(int annotationId, Container container)
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT ra.* FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "ra");
        sqlFragment.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoReplicate(), "rp");
        sqlFragment.append(" ON ra.replicateId = rp.Id");
        sqlFragment.append(" INNER JOIN ").append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(" ON r.Id = rp.runId");
        sqlFragment.append(" WHERE ra.Id=?").add(annotationId);
        sqlFragment.append(" AND r.Container=?").add(container);

        return new SqlSelector(TargetedMSManager.getSchema(), sqlFragment).getObject(ReplicateAnnotation.class);
    }

    public static List<SampleFile> getSampleFilesForRun(long runId)
    {
        String sql = "SELECT sf.* FROM "+
                     TargetedMSManager.getTableInfoSampleFile()+" AS sf, "+
                     TargetedMSManager.getTableInfoReplicate()+" AS rep "+
                     "WHERE rep.Id=sf.ReplicateId "+
                     "AND rep.RunId=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(runId);

        return new ArrayList<>(new SqlSelector(TargetedMSManager.getSchema(), sf).getCollection(SampleFile.class));
    }

    public static List<String> getSampleFilePaths(long runId)
    {
        String sql = "SELECT sf.filePath FROM "+
                TargetedMSManager.getTableInfoSampleFile()+" AS sf, "+
                TargetedMSManager.getTableInfoReplicate()+" AS rep "+
                "WHERE rep.Id=sf.ReplicateId "+
                "AND rep.RunId=?";
        SQLFragment sf = new SQLFragment(sql);
        sf.add(runId);

        return new ArrayList<>(new SqlSelector(TargetedMSManager.getSchema(), sf).getCollection(String.class));
    }

    public static List<Replicate> getReplicatesForRun(long runId)
    {
        return new ArrayList<>(
                                 new TableSelector(TargetedMSManager.getTableInfoReplicate(),
                                                   new SimpleFilter(FieldKey.fromParts("RunId"), runId),
                                                   null)
                                 .getCollection(Replicate.class));
    }

    public static List<String> getReplicateAnnotationNamesForRun(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT DISTINCT replAnnot.Name FROM ");
        sql.append(TargetedMSManager.getTableInfoReplicate(), "repl");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "replAnnot");
        sql.append(" WHERE ");
        sql.append(" repl.RunID = ? ");
        sql.append(" AND repl.Id = replAnnot.ReplicateId ");
        sql.add(runId);

        return new ArrayList<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(String.class));
    }

    public static List<ReplicateAnnotation> getReplicateAnnotationsForRun(long runId)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT replAnnot.* FROM ");
        sql.append(TargetedMSManager.getTableInfoReplicate(), "repl");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "replAnnot");
        sql.append(" WHERE ");
        sql.append(" repl.RunID = ? ");
        sql.append(" AND repl.Id = replAnnot.ReplicateId ");
        sql.add(runId);

        return new ArrayList<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(ReplicateAnnotation.class));
    }

    public static List<ReplicateAnnotation> getUniqueSortedAnnotationNameValue(long runId)
    {
        List<ReplicateAnnotation> allAnnotationsList = getReplicateAnnotationsForRun(runId);
        Map<String, ReplicateAnnotation> uniqueAnnotationsMap = new HashMap<>();

        for(ReplicateAnnotation annotation: allAnnotationsList)
        {
            uniqueAnnotationsMap.put(annotation.getDisplayName(), annotation);
        }

        List<ReplicateAnnotation> uniqueAnnotationsList = new ArrayList<>(uniqueAnnotationsMap.values());

        //Sorts alphabetically by Name then value if names are same
        uniqueAnnotationsList.sort((o1, o2) ->
        {
            //If ReplicateAnnotation.getName() for o1 and o2 are the same sorts by .getValue()
            if (o1.getName().equals(o2.getName()))
            {
                if (o1.getValue().matches("[-+]?\\d*\\.?\\d+") && o2.getValue().matches("[-+]?\\d*\\.?\\d+"))
                {
                    return Double.valueOf(o1.getValue()).compareTo(Double.valueOf(o2.getValue()));
                }
                else
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
            }
            else
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return uniqueAnnotationsList;
    }

    public static List<ReplicateAnnotation> getReplicateAnnotationNameValues(Container container)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT DISTINCT annot.Name AS Name, annot.Value AS Value FROM ");
        sql.append(TargetedMSManager.getTableInfoReplicateAnnotation(), "annot");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoReplicate(), "replicate").append(" ON annot.ReplicateId = replicate.Id");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "runs").append(" ON replicate.RunId = runs.Id");
        sql.append(" WHERE ");
        sql.append("runs.Container=?");
        sql.add(container);
        sql.append(" ORDER BY Name, Value");

        return new ArrayList<>(new SqlSelector(TargetedMSManager.getSchema(), sql).getCollection(ReplicateAnnotation.class));
    }

    public static TransitionSettings.Predictor getReplicatePredictor(long predictorId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPredictor(),
                new SimpleFilter(FieldKey.fromParts("Id"), predictorId), null)
                .getObject(TransitionSettings.Predictor.class);
    }
}
