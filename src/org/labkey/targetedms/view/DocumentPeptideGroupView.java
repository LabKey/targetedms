/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.targetedms.view;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.TargetedMSTable;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.targetedms.query.AnnotatedTargetedMSTable.NOTE_ANNOTATIONS_COLUMN_NAME;

public class DocumentPeptideGroupView extends QueryView
{
    private final TargetedMSSchema _schema;
    private final long _runId;

    public DocumentPeptideGroupView(ViewContext ctx, TargetedMSSchema schema, long runId,
                                    String tableName, String title)
    {
        super(schema, schema.getSettings(ctx, tableName, tableName), null);
        _schema = schema;
        _runId = runId;
        setTitle(title);
    }

    @Override
    public TableInfo createTable()
    {
        TargetedMSTable tinfo = (TargetedMSTable) _schema.getTable(getSettings().getQueryName(), null, true, true);
        tinfo.addContainerTableFilter(new CompareType.EqualsCompareClause(FieldKey.fromParts("Id"), CompareType.EQUAL, _runId));
        String queryName = getSettings().getQueryName();
        if (TargetedMSSchema.TABLE_PEPTIDE_GROUP.equalsIgnoreCase(queryName))
            getSettings().getBaseFilter().addCondition(FieldKey.fromParts("PeptideCount"), 0, CompareType.GT);
        else if (TargetedMSSchema.TABLE_MOLECULE_GROUP.equalsIgnoreCase(queryName))
            getSettings().getBaseFilter().addCondition(FieldKey.fromParts("MoleculeCount"), 0, CompareType.GT);
        List<FieldKey> cols = new ArrayList<>(tinfo.getDefaultVisibleColumns());
        cols.remove(FieldKey.fromParts("RunId"));
        cols.remove(FieldKey.fromParts(NOTE_ANNOTATIONS_COLUMN_NAME));
        cols.remove(FieldKey.fromParts("Modified"));
        tinfo.setDefaultVisibleColumns(cols);
        tinfo.setLocked(true);
        return tinfo;
    }
}
