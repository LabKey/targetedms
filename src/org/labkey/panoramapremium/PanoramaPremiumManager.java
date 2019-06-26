package org.labkey.panoramapremium;

import org.apache.log4j.Logger;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;

public class PanoramaPremiumManager
{
    private static final PanoramaPremiumManager _instance = new PanoramaPremiumManager();

    private static Logger _log = Logger.getLogger(PanoramaPremiumManager.class);

    private PanoramaPremiumManager()
    {
        // prevent external construction with a private default constructor
    }

    public static PanoramaPremiumManager get()
    {
        return _instance;
    }

    public String getSchemaName()
    {
        return PanoramaPremiumSchema.SCHEMA_NAME;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(PanoramaPremiumSchema.SCHEMA_NAME, DbSchemaType.Module);
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoQCEmailNotifications()
    {
        return getSchema().getTable(PanoramaPremiumSchema.TABLE_QC_EMAIL_NOTIFICATIONS);
    }
}
