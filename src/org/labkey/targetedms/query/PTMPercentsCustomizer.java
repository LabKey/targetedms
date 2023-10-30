package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ConditionalFormat;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Pair;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.parser.Protein;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Customizes the set of columns available on a pivot query that operates on samples, hiding all of the pivot values
 * that aren't part of the run that's being filtered on. This lets you view a single document's worth of data
 * without seeing empty columns for all of the other samples in the same container.
 */
public class PTMPercentsCustomizer implements TableCustomizer
{

    /** Referenced from query XML metadata */
    @SuppressWarnings("unused")
    public PTMPercentsCustomizer(MultiValuedMap<String, String> props)
    {

    }

    @Override
    public void customize(TableInfo tableInfo)
    {
        List<FieldKey> defaultCols = new ArrayList<>(tableInfo.getDefaultVisibleColumns());
        defaultCols.remove(FieldKey.fromParts("AminoAcid"));
        defaultCols.remove(FieldKey.fromParts("Location"));
        tableInfo.setDefaultVisibleColumns(defaultCols);
    }
}
