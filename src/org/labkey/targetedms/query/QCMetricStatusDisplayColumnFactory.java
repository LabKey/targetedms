package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.api.targetedms.model.QCMetricConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QCMetricStatusDisplayColumnFactory implements DisplayColumnFactory
{
    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new QCMetricStatusDisplayColumn(colInfo);
    }

    public static class QCMetricStatusDisplayColumn extends DataColumn
    {
        private Map<String, QCMetricConfiguration> _configs;


        public QCMetricStatusDisplayColumn(ColumnInfo col)
        {
            super(col);
        }

        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromString(getColumnInfo().getFieldKey().getParent(), "Name"));
        }

        @Override
        public Object getValue(RenderContext ctx)
        {
            String name = ctx.get(FieldKey.fromString(getColumnInfo().getFieldKey().getParent(), "Name"), String.class);
            if (_configs == null)
            {
                _configs = new HashMap<>();
                for (QCMetricConfiguration config : TargetedMSManager.getAllQCMetricConfigurations(new TargetedMSSchema(ctx.getViewContext().getUser(), ctx.getContainer())))
                {
                    _configs.put(config.getName(), config);
                }
            }
            QCMetricConfiguration config = _configs.get(name);
            if (config != null && config.getStatus() != null)
            {
                return config.getStatus().toString();
            }
            return super.getValue(ctx);
        }
    }
}
