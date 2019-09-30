package org.labkey.targetedms.parser.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListData
{
    private ListDefinition _listDefinition;
    private List<ListColumn> _columns;
    private List<List<Object>> _columnDatas;

    public ListData()
    {
        _listDefinition = new ListDefinition();
        _columns = new ArrayList<>();
        _columnDatas = new ArrayList<>();
    }

    public int getItemCount()
    {
        if (_columnDatas.isEmpty()) {
            return 0;
        }
        return _columnDatas.get(0).size();
    }

    public int getColumnCount() {
        return _columns.size();
    }

    public ListDefinition getListDefinition() {
        return _listDefinition;
    }

    public ListColumn getColumnDef(int i) {
        return _columns.get(i);
    }

    public Object getColumnValue(int iRow, int iCol)
    {
        return _columnDatas.get(iCol).get(iRow);
    }

    public void addColumnDefinition(ListColumn column)
    {
        _columns.add(column);
    }

    public void addColumnData(List<Object> data)
    {
        _columnDatas.add(data);
    }

    public List<ListColumn> getColumnDefinitions()
    {
        return Collections.unmodifiableList(_columns);
    }

    public List<List<Object>> getColumnDatas() {
        return Collections.unmodifiableList(_columnDatas);
    }
}
