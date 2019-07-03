package org.labkey.targetedms.parser.skyaudit;

import org.labkey.api.data.BaseSelector;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.targetedms.TargetedMSSchema;

public class DatabaseUtil
{

    public static Object retrieveSimpleType(SQLFragment pQuery){
        BaseSelector.ResultSetHandler<Object> resultSetHandler = (rs, conn) -> {
            if(rs.next()){
                return rs.getObject(1);
            }
            else
                return 0;
        };

        Object result = new SqlExecutor(TargetedMSSchema.getSchema()).executeWithResults(pQuery, resultSetHandler);
        return result;
    }
}
