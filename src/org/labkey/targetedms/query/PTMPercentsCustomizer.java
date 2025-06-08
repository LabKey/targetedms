package org.labkey.targetedms.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;

import java.util.ArrayList;
import java.util.List;

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
