package org.labkey.targetedms.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSModule;
import org.labkey.targetedms.TargetedMSRun;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QCFolderDisplayColumnFactory implements DisplayColumnFactory
{
    public QCFolderDisplayColumnFactory()
    {
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                final User user = ctx.getViewContext().getUser();
                String serialNumber = String.valueOf(ctx.get("SerialNumber"));
                var instrumentRunIds = TargetedMSManager.getRunIdsByInstrument(serialNumber);
                List<Container> qcContainers = new ArrayList<>();
                instrumentRunIds.forEach(runId -> {
                    TargetedMSRun run = TargetedMSManager.getRun(runId);
                    if (null != run && Objects.equals(TargetedMSModule.getFolderType(run.getContainer()), TargetedMSService.FolderType.QC))
                    {
                        if (run.getContainer().hasPermission(user, ReadPermission.class))
                        {
                            qcContainers.add(run.getContainer());
                        }
                    }
                });
                StringBuilder sb = new StringBuilder();
                qcContainers.forEach(qcContainer -> {
                    var url = qcContainer.getStartURL(user);
                    sb.append("<div><a href=\"")
                            .append(url)
                            .append("\"")
                            .append("target=\"_blank\">")
                            .append(PageFlowUtil.filter(qcContainer.getName()))
                            .append("</a></div>");
                });
                out.write(sb.toString());
            }
        };
    }
}
