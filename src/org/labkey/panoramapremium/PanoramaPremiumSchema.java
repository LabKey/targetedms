package org.labkey.panoramapremium;

import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.panoramapremium.query.QCEmailNotificationsTable;

import java.util.Set;

public class PanoramaPremiumSchema extends UserSchema
{
    public static final String SCHEMA_NAME = "panoramapremium";
    public static final String SCHEMA_DESCR = "Contains data for Panorama Premium Features.";

    public static final String TABLE_QC_EMAIL_NOTIFICATIONS = "QCEmailNotifications";

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                return module == null || schema.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule("targetedms"));
            }

            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new PanoramaPremiumSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public PanoramaPremiumSchema(User user, Container container)
    {
        super(SCHEMA_NAME, SCHEMA_DESCR, user, container, PanoramaPremiumManager.getSchema());
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }

    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if(TABLE_QC_EMAIL_NOTIFICATIONS.equalsIgnoreCase(name))
        {
            return new QCEmailNotificationsTable(this, cf);
        }
        return null;
    }

    @Override
    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_QC_EMAIL_NOTIFICATIONS);
        return hs;
    }
}
