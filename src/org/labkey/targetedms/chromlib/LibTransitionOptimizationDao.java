/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibTransitionOptimizationDao extends BaseDaoImpl<LibTransitionOptimization>
{
    @Override
    protected List<LibTransitionOptimization> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibTransitionOptimization> transitionOptimizations = new ArrayList<>();
        while (rs.next())
        {
            LibTransitionOptimization transitionOptimization = new LibTransitionOptimization();
            transitionOptimization.setTransitionId(rs.getInt(Constants.TransitionOptimizationColumn.TransitionId.name()));
            transitionOptimization.setOptimizationValue(readDouble(rs, Constants.TransitionOptimizationColumn.OptimizationValue.name()));
            transitionOptimization.setOptimizationType(rs.getString(Constants.TransitionOptimizationColumn.OptimizationType.name()));

            transitionOptimizations.add(transitionOptimization);
        }
        return transitionOptimizations;
    }

    @Override
    protected void setValuesInStatement(LibTransitionOptimization libTransitionOptimization, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setInt(colIndex++, libTransitionOptimization.getTransitionId());
        stmt.setString(colIndex++, libTransitionOptimization.getOptimizationType());
        stmt.setDouble(colIndex, libTransitionOptimization.getOptimizationValue());
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.TransitionOptimizationColumn.values();
    }

    @Override
    public String getTableName()
    {
        return Constants.Table.TransitionOptimization.name();
    }

}
