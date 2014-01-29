package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: vsharma
 * Date: 12/19/13
 * Time: 2:29 PM
 */
public class ExperimentAnnotationsTableInfo extends FilteredTable
{

    public ExperimentAnnotationsTableInfo(final TargetedMSSchema schema, User user)
    {
        this(TargetedMSManager.getTableInfoExperimentAnnotations(), schema, user);
    }

    public ExperimentAnnotationsTableInfo(TableInfo tableInfo, UserSchema schema, User user)
    {
        super(tableInfo, schema, new ContainerFilter.CurrentAndSubfolders(user));

        wrapAllColumns(true);
        setDetailsURL(new DetailsURL(new ActionURL(TargetedMSController.ShowExperimentAnnotationsAction.class,
                getContainer()), "id", FieldKey.fromParts("Id")));

        ColumnInfo citationCol = getColumn(FieldKey.fromParts("Citation"));
        citationCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new PublicationLinkDisplayColumn(colInfo);
            }
        });
        citationCol.setURLTargetWindow("_blank");
        citationCol.setLabel("Publication");

        ColumnInfo spikeInColumn = getColumn(FieldKey.fromParts("SpikeIn"));
        spikeInColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new YesNoDisplayColumn(colInfo);
            }
        });

        SQLFragment runCountSQL = new SQLFragment("(SELECT COUNT(r.Id) FROM ");
        runCountSQL.append(TargetedMSManager.getTableInfoExperimentAnnotationsRun(), "r");
        runCountSQL.append(" WHERE r.ExperimentAnnotationsId = ");
        runCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        runCountSQL.append(".Id)");
        ExprColumn runCountColumn = new ExprColumn(this, "Runs", runCountSQL, JdbcType.INTEGER);
        addColumn(runCountColumn);

        List<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("Title"));
        visibleColumns.add(FieldKey.fromParts("Organism"));
        visibleColumns.add(FieldKey.fromParts("Instrument"));
        visibleColumns.add(FieldKey.fromParts("SpikeIn"));
        visibleColumns.add(FieldKey.fromParts("Citation"));
        visibleColumns.add(FieldKey.fromParts("Runs"));
        visibleColumns.add(FieldKey.fromParts("Container"));

        setDefaultVisibleColumns(visibleColumns);
    }

    @Override
    public String getName()
    {
        return TargetedMSSchema.TABLE_EXPERIMENT_ANNOTATIONS;
    }

    public static  class PublicationLinkDisplayColumn extends DataColumn
    {
        public PublicationLinkDisplayColumn(ColumnInfo colInfo)
        {
            super(colInfo);
        }

        @Override
        public Object getValue(RenderContext ctx)
        {
            Object citation = ctx.get("Citation");
            Object publicationLink = ctx.get("PublicationLink");

            if(citation != null)
            {
                if(publicationLink != null)
                {
                    setURL((String)publicationLink);
                }
                return citation;
            }
            return "";
        }

        @Override
        public Object getDisplayValue(RenderContext ctx)
        {
            return getValue(ctx);
        }

        @Override
        public String getFormattedValue(RenderContext ctx)
        {
            return h(getValue(ctx));
        }
        @Override
        public void addQueryFieldKeys(Set<FieldKey> keys)
        {
            super.addQueryFieldKeys(keys);
            keys.add(FieldKey.fromParts("PublicationLink"));
        }
    }

    public static  class YesNoDisplayColumn extends DataColumn
    {
        public YesNoDisplayColumn(ColumnInfo colInfo)
        {
            super(colInfo);
        }

        @Override
        public Object getValue(RenderContext ctx)
        {
            Object value =  super.getValue(ctx);
            if(value != null)
            {
                return (Boolean)value ? "Yes" : "No";
            }
            return "";
        }

        @Override
        public Object getDisplayValue(RenderContext ctx)
        {
            return getValue(ctx);
        }

        @Override
        public String getFormattedValue(RenderContext ctx)
        {
            return h(getValue(ctx));
        }


    }
}
