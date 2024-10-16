package org.labkey.targetedms.view.passport;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.UrlColumn;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.passport.PassportController;
import org.springframework.validation.Errors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ProteinListView extends QueryView
{
    public ProteinListView(UserSchema schema, QuerySettings settings, @Nullable Errors errors)
    {
        super(schema, settings, errors);
    }

    @Override
    protected void renderView(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        super.renderView(model, request, response);
    }

    @Override
    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);
    }

    public static ProteinListView createView(ViewContext model)
    {
        UserSchema schema = QueryService.get().getUserSchema(model.getUser(), model.getContainer(), "targetedms");
        QuerySettings querySettings = new QuerySettings(model, "Passport_proteins", "Passport_proteins");
        ProteinListView view = new ProteinListView(schema, querySettings, null);
        view.setAllowableContainerFilterTypes(ContainerFilter.Type.Current);
        view.setTitle("Proteins");
        view.setHidePageTitle(true);
        view.setFrame(FrameType.PORTAL);
        return view;
    }

    @Override
    protected void setupDataView(DataView ret)
    {
        super.setupDataView(ret);
        ActionURL url = new ActionURL(PassportController.ProteinAction.class, getContainer());
        url.addParameter("proteinId", "${proteinId}");
        SimpleDisplayColumn urlColumn = new UrlColumn(url, "PASSPORT VIEW");
        ret.getDataRegion().addDisplayColumn(0, urlColumn);

        ActionURL urlDownload = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getContainer());
        urlDownload.addParameter("id", "${runid}");
        SimpleDisplayColumn urlColumnDownload = new UrlColumn(urlDownload, "Download");
        urlColumnDownload.setName("Skyline"); // TODO check if works
        ret.getDataRegion().addDisplayColumn(10, urlColumnDownload);
        ret.getDataRegion().removeColumns("runid");
    }
}
