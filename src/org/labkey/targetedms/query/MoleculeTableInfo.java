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
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.WrappedColumnInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;
import java.util.List;

public class MoleculeTableInfo extends AbstractGeneralMoleculeTableInfo
{
    public MoleculeTableInfo(TargetedMSSchema schema, ContainerFilter cf, boolean omitAnnotations)
    {
        super(schema, TargetedMSManager.getTableInfoMolecule(), cf, omitAnnotations ? null : "Molecule");

        var peptideGroupId = getMutableColumnOrThrow("PeptideGroupId");
        peptideGroupId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_MOLECULE_GROUP, cf));

        SQLFragment molSQL = new SQLFragment(" COALESCE(" + ExprColumn.STR_TABLE_ALIAS + ".CustomIonName, " +
                ExprColumn.STR_TABLE_ALIAS + ".IonFormula, "  +
                "CAST ((" + getSqlDialect().getRoundFunction( " 10000.0 * " + ExprColumn.STR_TABLE_ALIAS + ".MassAverage") + ")/10000 AS VARCHAR)) ");
        ExprColumn molExprCol = new ExprColumn(this, "MoleculeName", molSQL, JdbcType.VARCHAR);
        molExprCol.setHidden(true);
        addColumn(molExprCol);

        var moleculeCol = WrappedColumnInfo.wrapAsCopy(this, FieldKey.fromParts("Molecule"), getColumn("MoleculeName"), "Molecule", null);
        moleculeCol.setDescription("Custom Ion Name");
        moleculeCol.setDisplayColumnFactory(IconColumn.MoleculeDisplayCol::new);
        moleculeCol.setURL(getDetailsURL(null, null));
        addColumn(moleculeCol);

        setTitleColumn(moleculeCol.getColumnName());

        List<FieldKey> defaultCols = new ArrayList<>(getDefaultVisibleColumns());
        defaultCols.add(0, FieldKey.fromParts("PeptideGroupId", "RunId", "Folder", "Path"));
        defaultCols.add(1, FieldKey.fromParts("PeptideGroupId", "RunId", "File"));
        defaultCols.add(2, FieldKey.fromParts("PeptideGroupId", "Label"));
        defaultCols.add(3, FieldKey.fromParts("Molecule"));
        defaultCols.remove(FieldKey.fromParts("PeptideGroupId"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected Class<? extends Controller> getDetailsActionClass()
    {
        return TargetedMSController.ShowMoleculeAction.class;
    }
}