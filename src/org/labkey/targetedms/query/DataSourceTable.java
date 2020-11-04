package org.labkey.targetedms.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.List;

public class DataSourceTable extends FilteredTable<TargetedMSSchema>
{
    public DataSourceTable(@NotNull TargetedMSSchema userSchema, @Nullable ContainerFilter containerFilter)
    {
        super(TargetedMSManager.getTableInfoDataSource(), userSchema, containerFilter);

        wrapAllColumns(true);

        List<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("DataId", "Name"));
        visibleColumns.add(FieldKey.fromParts("DataId", "Created"));
        visibleColumns.add(FieldKey.fromParts("DataId", "CreatedBy"));
        visibleColumns.add(FieldKey.fromParts("Size"));
        visibleColumns.add(FieldKey.fromParts("InstrumentType"));

        setDefaultVisibleColumns(visibleColumns);
    }
}
