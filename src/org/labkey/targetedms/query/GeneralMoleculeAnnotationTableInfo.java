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
import org.labkey.api.data.TableInfo;
import org.labkey.targetedms.TargetedMSSchema;

public class GeneralMoleculeAnnotationTableInfo extends TargetedMSTable
{
    public GeneralMoleculeAnnotationTableInfo(TableInfo table, TargetedMSSchema schema, ContainerFilter cf, TargetedMSSchema.ContainerJoinType joinType)
    {
        super(table, schema, cf, joinType);

        var generalMoleculeId = getMutableColumn("GeneralMoleculeId");
        generalMoleculeId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE, cf));
        generalMoleculeId.setHidden(true);

        var peptideId = wrapColumn("PeptideId", getRealTable().getColumn(generalMoleculeId.getFieldKey()));
        peptideId.setFk(new TargetedMSForeignKey(getUserSchema(), TargetedMSSchema.TABLE_PEPTIDE, cf));
        addColumn(peptideId);
    }
}