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

public class LibPredictorDao extends BaseDaoImpl<LibPredictor>
{
    @Override
    protected List<LibPredictor> parseQueryResult(ResultSet rs) throws SQLException
    {
        List<LibPredictor> libPredictors = new ArrayList<>();
        while (rs.next())
        {
            LibPredictor libPredictor = new LibPredictor();
            libPredictor.setName(rs.getString(Constants.PredictorColumn.Name.name()));
            libPredictor.setStepSize(rs.getDouble(Constants.PredictorColumn.StepSize.name()));
            libPredictor.setStepCount(rs.getInt(Constants.PredictorColumn.StepCount.name()));

            libPredictors.add(libPredictor);
        }
        return libPredictors;
    }

    @Override
    protected void setValuesInStatement(LibPredictor predictor, PreparedStatement stmt) throws SQLException
    {
        int colIndex = 1;
        stmt.setString(colIndex++, predictor.getName());
        stmt.setDouble(colIndex++, predictor.getStepSize());
        stmt.setInt(colIndex, predictor.getStepCount());
    }

    @Override
    protected Constants.ColumnDef[] getColumns()
    {
        return Constants.PredictorColumn.values();
    }

    @Override
    public String getTableName()
    {
        return Constants.Table.Predictor.name();
    }
}
