package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.NamedObjectList;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.targetedms.parser.list.ListColumn;
import org.labkey.targetedms.parser.list.ListDefinition;

import java.util.List;

public class SkylineListTable extends AbstractTableInfo
{
    ListDefinition _listDefinition;
    List<ListColumn> _listColumns;
    public SkylineListTable(UserSchema schema, ListDefinition listDefinition, List<ListColumn> columns) {
        super(schema.getDbSchema(), listDefinition.getName());
        _listDefinition = listDefinition;
        _listColumns = columns;
        for (ListColumn listColumn : _listColumns) {
            addColumn(new ListColumnInfo(this, listColumn));
        }
    }

    @Override
    public @Nullable UserSchema getUserSchema()
    {
        return null;
    }

    @Override
    public @NotNull NamedObjectList getSelectList(String columnName)
    {
        return null;
    }

    @Override
    public boolean hasDbTriggers()
    {
        return false;
    }

    @Override
    public boolean allowQueryTableURLOverrides()
    {
        return false;
    }

    @Override
    protected SQLFragment getFromSQL()
    {
        SQLFragment fragment = new SQLFragment("SELECT Id FROM targetedms.ListItem WHERE ListDefinitionId = ?", _listDefinition.getId());
        return fragment;
    }

    public BaseColumnInfo makeColumn(ListColumn listColumn) {
        return new ListColumnInfo(this, listColumn);
    }

    class ListColumnInfo extends BaseColumnInfo
    {
        ListColumn _listColumn;
        public ListColumnInfo(SkylineListTable listTable, ListColumn listColumn) {
            super(new FieldKey(null, listColumn.getName()), listTable);
            _listColumn = listColumn;
        }

        public SkylineListTable getListTable() {
            return (SkylineListTable) super.getParentTable();
        }

        @Override
        public SQLFragment getValueSql(String tableAliasName)
        {
            SQLFragment sqlFragment = new SQLFragment();
            sqlFragment.append("(SELECT ");
            switch (_listColumn.getAnnotationTypeEnum()) {
                default:
                    sqlFragment.append("TextValue");
                    break;
                case number:
                    sqlFragment.append("NumberValue");
                    break;
                case true_false:
                    sqlFragment.append("NumberValue = 1");
                    break;
            }
            sqlFragment.append(new SQLFragment(" FROM targetedms.ListItemValue WHERE ListItemId = ? AND ColumnIndex = ?",
                    getListTable()._listDefinition.getId(), _listColumn.getColumnIndex()));
            sqlFragment.append(")");
            return sqlFragment;
        }
    }
}
