/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.targetedms.parser.skyaudit;

import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUtil
{

    public static Object retrieveSimpleType(SQLFragment pQuery){

        Object result = new SqlSelector(TargetedMSManager.getSchema(), pQuery)
                .getObject(Object.class );
        return result;
    }

    public static Map<String, Object> retrieveTuple(SQLFragment pQuery){
        BaseSelector.ResultSetHandler<Map<String, Object>> resultSetHandler = (rs, conn) -> {
            if(rs.next()){
                Map<String, Object> result = new HashMap<>();
                for(int i= 0; i < rs.getMetaData().getColumnCount(); i++){
                    String colName = rs.getMetaData().getColumnName(i);
                    result.put(colName, rs.getObject(i));
                    if(rs.wasNull())
                        result.put(colName, null);
                }
                return result;
            }
            else
                return new HashMap<>();
        };

        Map<String, Object> result = new SqlExecutor(TargetedMSSchema.getSchema()).executeWithResults(pQuery, resultSetHandler);
        return result;
    }
}
