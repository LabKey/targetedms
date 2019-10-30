package org.labkey.api.targetedms;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;

public abstract class SearchResultColumnInfo
{
    private final SQLFragment _colSql;
    private final String _name;
    private final JdbcType _jdbcType;

    public SearchResultColumnInfo(@NotNull String name, @NotNull SQLFragment colSql, @NotNull JdbcType jdbcType)
    {
        _colSql = colSql;
        _name = name;
        _jdbcType = jdbcType;
    }

    public FieldKey getFieldKey()
    {
        return FieldKey.fromParts(_name);
    }

    public ExprColumn createColumn(TableInfo table, Container container)
    {
        ExprColumn col = new ExprColumn(table, _name, _colSql, _jdbcType);
        setupColumn(col, container);
        return col;
    }

    public abstract void setupColumn(ExprColumn col, Container container);
}
