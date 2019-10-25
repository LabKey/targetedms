package org.labkey.targetedms.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.list.ListDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SkylineListSchema extends UserSchema
{
    public static final String SCHEMA_NAME = "targetedmslists";

    public static final String ID_SEPARATOR = "_";
    public static final String UNION_PREFIX = "All" + ID_SEPARATOR;

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new SkylineListSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public SkylineListSchema(User user, Container container)
    {
        super(SCHEMA_NAME, "Contains data from custom lists imported from Skyline documents", user, container, TargetedMSSchema.getSchema());
    }

    @Override
    public @Nullable TableInfo createTable(String name, ContainerFilter cf)
    {
        if (name.toLowerCase().startsWith(UNION_PREFIX.toLowerCase()))
        {
            List<ListDefinition> listDefs = SkylineListManager.getListDefinitions(getContainer(), getDefaultContainerFilter());
            listDefs = listDefs.stream().filter((l) -> name.equalsIgnoreCase(l.getUnionUserSchemaTableName())).collect(Collectors.toList());
            if (!listDefs.isEmpty())
            {
                ListDefinition firstListDef = listDefs.get(0);
                SkylineListTable firstTable = new SkylineListTable(this, firstListDef);
                SkylineListUnionTable result = new SkylineListUnionTable(this, firstTable);

                for (int i = 1; i < listDefs.size(); i++)
                {
                    ListDefinition def = listDefs.get(i);
                    if (def.matches(firstListDef))
                    {
                        result.addUnionTable(new SkylineListTable(this, def));
                    }

                }
                return result;
            }
        }

        int separatorIndex = name.indexOf(ID_SEPARATOR);
        if (separatorIndex > 0)
        {
            try
            {
                int runId = Integer.parseInt(name.substring(0, separatorIndex));
                ListDefinition listDefinition = SkylineListManager.getListDefinition(cf == null ? getDefaultContainerFilter() : cf, getContainer(), runId, name);
                if (listDefinition != null)
                {
                    return new SkylineListTable(this, listDefinition);
                }
            }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @Override
    public Set<String> getTableNames()
    {
        List<ListDefinition> listDefs = SkylineListManager.getListDefinitions(getContainer(), getDefaultContainerFilter());
        Set<String> result = new CaseInsensitiveHashSet();
        result.addAll(listDefs.stream().map(ListDefinition::getUserSchemaTableName).collect(Collectors.toSet()));
        result.addAll(listDefs.stream().map(ListDefinition::getUnionUserSchemaTableName).collect(Collectors.toSet()));
        return result;
    }
}
