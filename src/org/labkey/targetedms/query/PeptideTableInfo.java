/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.targetedms.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.WrappedColumnInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;
import java.util.List;

public class PeptideTableInfo extends AbstractGeneralMoleculeTableInfo
{
    public PeptideTableInfo(TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        super(schema, TargetedMSManager.getTableInfoPeptide(), cf, omitAnnotations ? null : "Peptide");

        var peptideGroupId = getMutableColumnOrThrow("PeptideGroupId");
        peptideGroupId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE_GROUP, cf));

        var sequenceColumn = getMutableColumnOrThrow("Sequence");
        sequenceColumn.setURL(getDetailsURL(null, null));

        var modSeqCol = WrappedColumnInfo.wrapAsCopy(this, FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME), getColumn("PeptideModifiedSequence"), "Peptide", null);
        modSeqCol.setDescription("Modified peptide sequence");
        modSeqCol.setDisplayColumnFactory(new ModifiedSequenceDisplayColumn.PeptideDisplayColumnFactory());
        modSeqCol.setURL(getDetailsURL(null, null));
        addColumn(modSeqCol);

        var structuralModCol = WrappedColumnInfo.wrapAsCopy(this, FieldKey.fromParts("StructuralModifications"), getColumn("Id"), null, null);
        structuralModCol.setKeyField(false);
        structuralModCol.setFk(new MultiValuedForeignKey(new QueryForeignKey.Builder(getUserSchema(), getContainerFilter()).table(TargetedMSSchema.TABLE_PEPTIDE_STRUCTURAL_MODIFICATION), "StructuralModId"));
        addColumn(structuralModCol);

        var isotopeModCol = WrappedColumnInfo.wrapAsCopy(this, FieldKey.fromParts("IsotopeModifications"), getColumn("Id"), null, null);
        isotopeModCol.setKeyField(false);
        isotopeModCol.setFk(new MultiValuedForeignKey(new QueryForeignKey.Builder(getUserSchema(), getContainerFilter()).table(TargetedMSSchema.TABLE_PEPTIDE_ISOTOPE_MODIFICATION), "IsotopeModId"));
        addColumn(isotopeModCol);

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
        defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
        defaultCols.add(2, FieldKey.fromParts("PeptideGroupId", "Label"));
        defaultCols.add(3, FieldKey.fromParts(ModifiedSequenceDisplayColumn.PEPTIDE_COLUMN_NAME));
        defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected Class<? extends Controller> getDetailsActionClass()
    {
        return TargetedMSController.ShowPeptideAction.class;
    }
}