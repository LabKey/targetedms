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

package org.labkey.targetedms.view;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.search.ProteinSearchForm;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QueryViewProvider;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.TargetedMSTable;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
* User: jeckels
* Date: May 10, 2012
*/
public class TransitionProteinSearchViewProvider implements QueryViewProvider<ProteinSearchForm>
{
    @Override
    public String getDataRegionName()
    {
        return "TargetedMSMatches";
    }

    @Nullable
    @Override
    public QueryView createView(ViewContext viewContext, final ProteinSearchForm form, BindException errors)
    {
        if (! viewContext.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class)))
            return null;  // only enable this view if the TargetedMSModule is active

        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), "Protein");
        settings.addAggregates(new Aggregate(FieldKey.fromParts("PeptideGroupId", "RunId", "File"), Aggregate.BaseType.COUNT, null, true));

        // Issue 17576: Peptide and Protein searches do not work for searching in subfolders
        if (form.isIncludeSubfolders())
            settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

        QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
                TargetedMSTable inner = (TargetedMSTable) super.createTable();
                FilteredTable<?> result = new FilteredTable<>(inner, getSchema());
                result.wrapAllColumns(true);

                // Apply a filter to restrict to the set of matching proteins
                SQLFragment sql = new SQLFragment("Id IN (SELECT p.Id FROM ");
                sql.append(TargetedMSManager.getTableInfoProtein(), "p");
                sql.append(" INNER JOIN ");
                sql.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");

                sql.append(" ON p.PeptideGroupId = pg.Id WHERE ((");
                int[] seqIds = form.getSeqId();
                if (seqIds.length > 0)
                {
                    sql.append("p.SequenceId IN (");
                    String separator = "";
                    for (int seqId : seqIds)
                    {
                        sql.append(separator);
                        sql.appendValue(seqId);
                        separator = ",";
                    }
                    sql.append(")");
                    sql.append(" OR ");
                }

                sql.append(getProteinLabelCondition("p.Label", getProteinLabels(form.getIdentifier()), form.isExactMatch()));

                ContainerFilter cf = form.isIncludeSubfolders() ? ContainerFilter.Type.CurrentAndSubfolders.create(getContainer(), getUser()) : ContainerFilter.current(getContainer());
                sql.append(")) AND pg.RunId IN (SELECT Id FROM ");
                sql.append(TargetedMSManager.getTableInfoRuns(), "r");
                sql.append(" WHERE ");
                sql.append(cf.getSQLFragment(result.getSchema(), new SQLFragment("Container")));
                sql.append("))");
                result.addCondition(sql);

                List<FieldKey> visibleColumns = new ArrayList<>();
                visibleColumns.add(FieldKey.fromParts("Label"));
                visibleColumns.add(FieldKey.fromParts("Description"));
                visibleColumns.add(FieldKey.fromParts("Accession"));
                visibleColumns.add(FieldKey.fromParts("PreferredName"));
                visibleColumns.add(FieldKey.fromParts("Gene"));
                visibleColumns.add(FieldKey.fromParts("Species"));
                visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
                if(form.isIncludeSubfolders())
                {
                    visibleColumns.add(FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
                }

                result.setDefaultVisibleColumns(visibleColumns);

                List<TableCustomizer> customizers = TargetedMSService.get().getProteinSearchResultCustomizer();
                for(TableCustomizer customizer : customizers)
                {
                    customizer.customize(result);
                }

                return result;
            }
        };
        result.setTitle("Targeted MS Proteins");
        result.enableExpandCollapse("TargetedMSProteins", false);
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }

    private List<String> getProteinLabels(String labels)
    {
        if(StringUtils.isBlank(labels))
            return Collections.emptyList();

        return Arrays.asList(StringUtils.split(labels, " \t\n\r,"));
    }

    private SQLFragment getProteinLabelCondition (String columnName, List<String> labels, boolean exactMatch)
    {
        SQLFragment sqlFragment = new SQLFragment();
        String separator = "";
        sqlFragment.append("(");
        if (labels.isEmpty())
        {
            sqlFragment.append("1 = 2");
        }
        for (String param : labels)
        {
            sqlFragment.append(separator);
            sqlFragment.append("LOWER (").append(columnName).append(")");
            if (exactMatch)
            {
                sqlFragment.append(" = LOWER(?)");
                sqlFragment.add(param);
            }
            else
            {
                sqlFragment.append(" LIKE LOWER(?)");
                sqlFragment.add("%" + param + "%");
            }
            separator = " OR ";
        }
        sqlFragment.append(")");
        return sqlFragment;
    }
}
