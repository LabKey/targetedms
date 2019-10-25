package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.VirtualTable;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.List;

public class SkylineListUnionTable extends VirtualTable<SkylineListSchema>
{
    private final List<SkylineListTable> _tables = new ArrayList<>();

    public SkylineListUnionTable(SkylineListSchema schema, SkylineListTable listTable)
    {
        super(schema.getDbSchema(), listTable._listDefinition.getUnionUserSchemaTableName(), schema);
        _tables.add(listTable);

        for (ColumnInfo childColumn : listTable.getColumns())
        {
            BaseColumnInfo column = new ExprColumn(this, childColumn.getFieldKey(), new SQLFragment(ExprColumn.STR_TABLE_ALIAS + " ." + childColumn.getAlias()), childColumn.getJdbcType());
            column.setKeyField(childColumn.isKeyField());
            addColumn(column);
        }
        setTitleColumn(listTable.getTitleColumn());
        if (getColumn("RunId") != null)
        {
            removeColumn(getColumn("RunId"));
        }

        BaseColumnInfo runIdColumn = new BaseColumnInfo(FieldKey.fromParts("RunId"), this, JdbcType.INTEGER);
        runIdColumn.setKeyField(true);
        runIdColumn.setFk(new QueryForeignKey.Builder(schema, schema.getDefaultContainerFilter()).schema(TargetedMSSchema.SCHEMA_NAME).table(TargetedMSSchema.TABLE_RUNS));
        addColumn(runIdColumn);
    }

    @Override
    public @NotNull SQLFragment getFromSQL()
    {
        String separator = "";
        SQLFragment result = new SQLFragment();
        for (SkylineListTable table : _tables)
        {
            result.append(separator);
            separator = "\nUNION\n";
            String innerAlias = "list" + table._listDefinition.getId();
            result.append(" SELECT ");
            result.append(table._listDefinition.getRunId()).append(" AS RunId ");
            for (ColumnInfo colInfo : table.getColumns())
            {
                result.append(",\n ");
                result.append(colInfo.getValueSql(innerAlias));
                result.append(" AS ");
                result.append(getSqlDialect().makeLegalIdentifier(colInfo.getAlias()));
            }
            result.append(" FROM ");
            result.append(table.getFromSQL(innerAlias));
        }
        return result;
    }

    public void addUnionTable(SkylineListTable skylineListTable)
    {
        _tables.add(skylineListTable);
    }
}
