/*
 * Copyright (c) 2012-2016 LabKey Corporation
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


import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.targetedms.RepresentativeDataState;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.Protein;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: vsharma
 * Date: 5/2/12
 * Time: 9:12 PM
 */
public class PeptideGroupManager
{
    private PeptideGroupManager() {}

    public static PeptideGroup get(long peptideGroupId)
    {
        return new TableSelector(TargetedMSManager.getTableInfoPeptideGroup(), new SimpleFilter(FieldKey.fromParts("Id"), peptideGroupId), null).getObject(PeptideGroup.class);
	}

    public static PeptideGroup getPeptideGroup(Container c, long id)
    {
        SQLFragment sql = new SQLFragment("SELECT pg.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("pg.RunId = r.Id AND r.Deleted = ? AND r.Container = ? AND pg.Id = ?");
        sql.add(false);
        sql.add(c.getId());
        sql.add(id);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(PeptideGroup.class);
    }

    public static List<Protein> getProteinsForPeptideGroup(long peptideGroupId, boolean includeNullSeqIds)
    {
        SQLFragment sql = new SQLFragment("SELECT s.ProtSequence AS Sequence, p.* FROM ");
        sql.append(TargetedMSManager.getTableInfoProtein(), "p");
        sql.append(" LEFT OUTER JOIN ");
        sql.append(ProteinService.get().getSequencesTable(), "s");
        sql.append(" ON p.SequenceId = s.SeqId WHERE p.PeptideGroupId = ?");
        sql.add(peptideGroupId);
        if (!includeNullSeqIds)
        {
            sql.append(" AND p.SequenceId IS NOT NULL");
        }
        sql.append(" ORDER BY p.Id");

        return Collections.unmodifiableList(new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Protein.class));
    }

    public static List<Protein> getProteinsForRun(long runId)
    {
        // Include the Sequence from the prot.sequences table
        SQLFragment sql = new SQLFragment("SELECT s.ProtSequence AS Sequence, p.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" INNER JOIN ");
        sql.append(TargetedMSManager.getTableInfoProtein(), "p");
        sql.append(" ON p.PeptideGroupId = pg.Id LEFT OUTER JOIN " );
        sql.append(ProteinService.get().getSequencesTable(), "s");
        sql.append(" ON p.SequenceId = s.SeqId WHERE pg.RunId = ?");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(Protein.class);
    }


    public static List<PeptideGroup> getPeptideGroupsForRun(long runId)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE RunId = ?");
        sql.add(runId);

        return new SqlSelector(TargetedMSManager.getSchema(), sql).getArrayList(PeptideGroup.class);
    }

    public static void updateRepresentativeStatus(List<Long> peptideGroupIds, RepresentativeDataState representativeState)
    {
        if(peptideGroupIds == null || peptideGroupIds.size() == 0)
            return;

        String peptideGroupIdsString = StringUtils.join(peptideGroupIds, ",");

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" SET RepresentativeDataState = ? ");
        sql.add(representativeState.ordinal());
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPeptideGroup()+".Id IN (");
        sql.append(peptideGroupIdsString);
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);

        // Update the representative state of all precursors of the given peptide groups
        updatePrecursorRepresentativeState(peptideGroupIdsString);
    }

    private static void updatePrecursorRepresentativeState(String peptideGroupIdsString)
    {
        SQLFragment sql;
        sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoGeneralPrecursor());
        sql.append(" SET RepresentativeDataState = pg.RepresentativeDataState");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptide(), "pep");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(" WHERE pg.Id IN (");
        sql.append(peptideGroupIdsString);
        sql.append(")");
        sql.append(" AND pep.Id = gm.Id ");
        sql.append(" AND pg.Id = gm.peptideGroupId ");
        sql.append(" AND gm.Id = "+TargetedMSManager.getTableInfoGeneralPrecursor()+".GeneralMoleculeId");
        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    // Set to either NotRepresentative or Representative_Deprecated.
    // If the original status was Representative it will be updated to Representative_Deprecated.
    // If the original status was Conflicted it will be update to NotRepresentative.
    public static void updateStatusToDeprecatedOrNotRepresentative(long[] peptideGroupIds)
    {
        if(peptideGroupIds == null || peptideGroupIds.length == 0)
            return;
        List<Long> peptideGroupIdList = new ArrayList<>(peptideGroupIds.length);
        for(int i = 0; i < peptideGroupIds.length; i++)
        {
            peptideGroupIdList.add(peptideGroupIds[i]);
        }

        updateStatusToDeprecatedOrNotRepresentative(peptideGroupIdList);
    }

    public static void updateStatusToDeprecatedOrNotRepresentative(List<Long> peptideGroupIds)
    {
        if(peptideGroupIds == null || peptideGroupIds.size() == 0)
            return;

        String peptideGroupIdsString = StringUtils.join(peptideGroupIds, ",");

        SQLFragment sql = new SQLFragment("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" SET RepresentativeDataState = ");
        sql.append(" CASE WHEN RepresentativeDataState = "+RepresentativeDataState.Conflicted.ordinal());
        sql.append(" THEN "+RepresentativeDataState.NotRepresentative.ordinal());
        sql.append(" ELSE "+RepresentativeDataState.Deprecated.ordinal());
        sql.append(" END");
        sql.append(" WHERE "+TargetedMSManager.getTableInfoPeptideGroup()+".Id IN (");
        sql.append(peptideGroupIdsString);
        sql.append(")");

        new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);

        // Update the representative state of all precursors of the given peptide groups
        updatePrecursorRepresentativeState(peptideGroupIdsString);
    }

    public static List<PeptideGroup> getRepresentativePeptideGroups(long runId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RunId"), runId);
        filter.addCondition(FieldKey.fromParts("RepresentativeDataState"), RepresentativeDataState.Representative.ordinal());

        Collection<PeptideGroup> groups = new TableSelector(TargetedMSManager.getTableInfoPeptideGroup(), filter, null).getCollection(PeptideGroup.class);
        return new ArrayList<>(groups);
    }

    public static PeptideGroup getLastDeprecatedPeptideGroup(PeptideGroup pepGrp, Container container)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT pepgrp.* FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "run");
        sql.append(" WHERE run.container = ?");
        sql.add(container);
        sql.append(" AND pepgrp.runId = run.Id");
        sql.append(" AND pepgrp.RepresentativeDataState = ?");
        sql.add(RepresentativeDataState.Deprecated.ordinal());
        sql.append(" AND (");

        List<Protein> proteins = getProteinsForPeptideGroup(pepGrp.getId(), false);
        Set<Integer> seqIds = proteins.stream().map(Protein::getSequenceId).collect(Collectors.toSet());

        if (!seqIds.isEmpty())
        {
            // Look for overlapping SequenceIds in other peptide group's protein
            sql.append("pepgrp.Id IN (SELECT PeptideGroupId FROM ");
            sql.append(TargetedMSManager.getTableInfoProtein(), "p");
            sql.append(" WHERE SequenceId ");
            TargetedMSSchema.getSchema().getSqlDialect().appendInClauseSql(sql, seqIds);
            sql.append(") OR ");
        }
        sql.append(" pepgrp.Label = ?");
        sql.add(pepGrp.getLabel());
        sql.append(")");
        sql.append(" ORDER BY pepgrp.Modified DESC ");

        PeptideGroup[] deprecatedGroups = new SqlSelector(TargetedMSManager.getSchema(), sql).getArray(PeptideGroup.class);
        if(deprecatedGroups == null || deprecatedGroups.length == 0)
        {
            return null;
        }
        return deprecatedGroups[0];
    }

    public static int setRepresentativeState(long runId, RepresentativeDataState state)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("UPDATE "+TargetedMSManager.getTableInfoPeptideGroup());
        sql.append(" SET RepresentativeDataState = ?");
        sql.add(state.ordinal());
        sql.append(" WHERE runId = ?");
        sql.add(runId);
        return new SqlExecutor(TargetedMSManager.getSchema()).execute(sql);
    }

    public static boolean ensureContainerMembership(List<Long> peptideGroupIds, Container container)
    {
        if(peptideGroupIds == null || peptideGroupIds.isEmpty())
            return false;

        Set<Long> uniqueIds = new HashSet<>(peptideGroupIds);

        StringBuilder pepGrpIds = new StringBuilder();
        for(Long id: uniqueIds)
        {
            pepGrpIds.append(",").append(id);
        }
        if(pepGrpIds.length() > 0)
            pepGrpIds.deleteCharAt(0);
        SQLFragment sql = new SQLFragment("SELECT COUNT(pg.Id) FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append("pg.RunId = r.Id AND r.Container = ? AND pg.Id IN (" + pepGrpIds + ")");
        sql.add(container.getId());

        Integer count = new SqlSelector(TargetedMSManager.getSchema(), sql).getObject(Integer.class);
        return count != null && count == uniqueIds.size();
    }

    public static Long getBestReplicateId(PeptideGroup peptideGroup)
    {
        if(peptideGroup == null)
            return null;

        SQLFragment sql = new SQLFragment();
        sql.append(" SELECT pepgrp.Id, sfile.ReplicateId ReplicateId, SUM(pci.TotalArea) areaSum");
        sql.append(" FROM ");
        sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pepgrp");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralMolecule(), "gm");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoGeneralPrecursor(), "prec");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoPrecursorChromInfo(), "pci");
        sql.append(", ");
        sql.append(TargetedMSManager.getTableInfoSampleFile(), "sfile");
        sql.append(" WHERE");
        sql.append(" sfile.Id = pci.SampleFileId");
        sql.append(" AND");
        sql.append(" pci.PrecursorId = prec.Id");
        sql.append(" AND");
        sql.append(" prec.GeneralMoleculeId = gm.Id");
        sql.append(" AND");
        sql.append(" gm.PeptideGroupId = pepgrp.Id");
        sql.append(" AND");
        sql.append(" pci.TotalArea IS NOT NULL");
        sql.append(" AND");
        sql.append(" (pci.NumTruncated IS NULL OR pci.NumTruncated = 0)");
        sql.append(" AND");
        sql.append(" pepgrp.Id = ?");
        sql.add(peptideGroup.getId());
        sql.append(" GROUP BY pepgrp.Id, sfile.ReplicateId");
        sql.append(" ORDER BY areaSUM DESC");

        Map<String, Object>[] mapArray = new SqlSelector(TargetedMSManager.getSchema(), sql).getMapArray();
        if(mapArray != null && mapArray.length > 0)
        {
            return (Long) mapArray[0].get("ReplicateId");
        }
        return null;
    }
}
