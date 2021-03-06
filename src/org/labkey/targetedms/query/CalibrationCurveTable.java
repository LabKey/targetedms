/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nicksh on 12/6/2016.
 */
public class CalibrationCurveTable extends TargetedMSTable
{
    public CalibrationCurveTable(TargetedMSSchema schema, ContainerFilter cf)
    {
        super(TargetedMSManager.getTableInfoCalibrationCurve(), schema, cf, TargetedMSSchema.ContainerJoinType.RunFK);
        Map<String, Object> params = new HashMap<>();
        params.put("id", FieldKey.fromParts("RunId"));
        params.put("calibrationCurveId", FieldKey.fromParts("Id"));
        DetailsURL detailsURL = new DetailsURL(new ActionURL(TargetedMSController.ShowCalibrationCurveAction.class, getContainer()), params);
        setDetailsURL(detailsURL);
        getMutableColumn(FieldKey.fromParts("Id")).setHidden(true);
        getMutableColumn(FieldKey.fromParts("RunId")).setHidden(true);
        getMutableColumn(FieldKey.fromParts("QuantificationSettingsId")).setHidden(true);
        getMutableColumn(FieldKey.fromParts("GeneralMoleculeId")).setURL(detailsURL);

        List<FieldKey> visibleCols = new ArrayList<>(getDefaultVisibleColumns());
        visibleCols.add(FieldKey.fromParts("GeneralMoleculeId", "PeptideGroupId", "Label"));
        setDefaultVisibleColumns(visibleCols);
    }

    public static class PeptideCalibrationCurveTable extends CalibrationCurveTable
    {
        public PeptideCalibrationCurveTable(TargetedMSSchema schema, ContainerFilter cf)
        {
            super(schema, cf);
            var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
            generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_PEPTIDE, cf));
            generalMoleculeId.setLabel("Peptide");
            SimpleFilter.SQLClause isPeptideFilter =
                    new SimpleFilter.SQLClause(new SQLFragment(generalMoleculeId.getName()
                            + " IN (SELECT Id FROM targetedms.Peptide)"),
                            generalMoleculeId.getFieldKey());
            addCondition(new SimpleFilter(isPeptideFilter));
        }
    }

    public static class MoleculeCalibrationCurveTable extends CalibrationCurveTable
    {
        public MoleculeCalibrationCurveTable(TargetedMSSchema schema, ContainerFilter cf)
        {
            super(schema, cf);
            var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
            generalMoleculeId.setFk(new TargetedMSForeignKey(_userSchema, TargetedMSSchema.TABLE_MOLECULE, cf));
            generalMoleculeId.setLabel("Molecule");
            SimpleFilter.SQLClause isMoleculeFilter =
                    new SimpleFilter.SQLClause(new SQLFragment(generalMoleculeId.getName()
                            + " IN (SELECT Id FROM targetedms.Molecule)"),
                            generalMoleculeId.getFieldKey());
            addCondition(new SimpleFilter(isMoleculeFilter));
        }
    }
}
