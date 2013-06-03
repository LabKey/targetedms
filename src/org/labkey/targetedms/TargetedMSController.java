/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.targetedms;


import org.junit.Assert;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Test;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ApiUsageException;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.HasViewContext;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.NestableQueryView;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultFolderType;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.ms2.MS2Urls;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.ContainerContext;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.GridView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.Portal;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.PageConfig;
import org.labkey.targetedms.chart.ChromatogramChartMakerFactory;
import org.labkey.targetedms.chart.PrecursorPeakAreaChartMaker;
import org.labkey.targetedms.chromlib.ChromatogramLibraryUtils;
import org.labkey.targetedms.conflict.ConflictPeptide;
import org.labkey.targetedms.conflict.ConflictPrecursor;
import org.labkey.targetedms.conflict.ConflictProtein;
import org.labkey.targetedms.conflict.ConflictTransition;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideChromInfo;
import org.labkey.targetedms.parser.PeptideGroup;
import org.labkey.targetedms.parser.PeptideSettings;
import org.labkey.targetedms.parser.Precursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;
import org.labkey.targetedms.parser.Replicate;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.query.ConflictResultsManager;
import org.labkey.targetedms.query.IsotopeLabelManager;
import org.labkey.targetedms.query.PeptideChromatogramsTableInfo;
import org.labkey.targetedms.query.PeptideGroupManager;
import org.labkey.targetedms.query.PeptideManager;
import org.labkey.targetedms.query.PrecursorChromatogramsTableInfo;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.query.ReplicateManager;
import org.labkey.targetedms.query.RepresentativeStateManager;
import org.labkey.targetedms.query.TargetedMSTable;
import org.labkey.targetedms.query.TransitionManager;
import org.labkey.targetedms.search.ModificationSearchWebPart;
import org.labkey.targetedms.view.ChromatogramsDataRegion;
import org.labkey.targetedms.view.DocumentPrecursorsView;
import org.labkey.targetedms.view.DocumentTransitionsView;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;
import org.labkey.targetedms.view.PeptidePrecursorChromatogramsView;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatch;
import org.labkey.targetedms.view.spectrum.LibrarySpectrumMatchGetter;
import org.labkey.targetedms.view.spectrum.PeptideSpectrumView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.targetedms.TargetedMSModule.EXP_RUN_TYPE;
import static org.labkey.targetedms.TargetedMSModule.FolderType;
import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD;
import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_FOLDER_TYPE;
import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_PEPTIDE_GROUP_VIEW;
import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_PEPTIDE_VIEW;
import static org.labkey.targetedms.TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME;

public class TargetedMSController extends SpringActionController
{
    private static final Logger LOG = Logger.getLogger(TargetedMSController.class);
    
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(TargetedMSController.class);
    public static final String CONFIGURE_TARGETED_MS_FOLDER = "Configure Targeted MS Folder";

    public TargetedMSController()
    {
        setActionResolver(_actionResolver);
    }

    public static ActionURL getShowListURL(Container c)
    {
        return new ActionURL(ShowListAction.class, c);
    }

    public static ActionURL getShowRunURL(Container c)
    {
        return new ActionURL(ShowPrecursorListAction.class, c);
    }

    public static ActionURL getShowRunURL(Container c, int runId)
    {
        ActionURL url = getShowRunURL(c);
        url.addParameter("id", String.valueOf(runId));
        return url;
    }

    // ------------------------------------------------------------------------
    // Action to setup a new folder
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class FolderSetupAction extends FormHandlerAction<FolderSetupForm>
    {
        public static final String DATA_PINELINE_TAB = "Data Pipeline";

        public static final String MASS_SPEC_SEARCH_WEBPART = "Mass Spec Search (Tabbed)";
        public static final String DATA_PIPELINE_WEBPART = "Data Pipeline";

        @Override
        public void validateCommand(FolderSetupForm target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(FolderSetupForm folderSetupForm, BindException errors) throws Exception
        {
            Container c = getContainer();
            TargetedMSModule targetedMSModule = null;
            for (Module m : c.getActiveModules())
            {
                if (m instanceof TargetedMSModule)
                {
                    targetedMSModule = (TargetedMSModule) m;
                }
            }
            if (targetedMSModule == null)
            {
                return true; // no TargetedMS module found - do nothing
            }
            ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(TARGETED_MS_FOLDER_TYPE);
            switch (FolderType.valueOf(moduleProperty.getValueContainerSpecific(c)))
            {
                case Experiment:
                case Library:
                case LibraryProtein:
                    return true;  // Module type already set to LibraryProtein
                case Undefined:
                    // continue with the remainder of the function
                    break;
            }
            if (folderSetupForm.getFolderType() != null && folderSetupForm.getFolderType().equals(FolderType.Experiment.toString()))
            {
                moduleProperty.saveValue(getUser(), c, FolderType.Experiment.toString());

                // setup the EXPERIMENTAL_DATA default webparts
                ArrayList<Portal.WebPart> tab1 = new ArrayList<>();
                tab1.add(Portal.getPortalPart(MASS_SPEC_SEARCH_WEBPART).createWebPart());
                tab1.add(Portal.getPortalPart(TARGETED_MS_RUNS_WEBPART_NAME).createWebPart());
                Portal.saveParts(c, DefaultFolderType.DEFAULT_DASHBOARD, tab1);
                // Add a second portal page (tab) and webparts
                ArrayList<Portal.WebPart> tab2 = new ArrayList<>();
                tab2.add(Portal.getPortalPart(DATA_PIPELINE_WEBPART).createWebPart());
                Portal.saveParts(c, DATA_PINELINE_TAB, tab2);
                Portal.addProperty(c, DATA_PINELINE_TAB, Portal.PROP_CUSTOMTAB);

                return true;
            }
            else if (folderSetupForm.getFolderType() != null && folderSetupForm.getFolderType().equals(FolderType.Library.toString()))
            {
                // setup the CHROMATOGRAM_LIBRARY default webparts
                if (folderSetupForm.isPrecursorNormalized())
                {
                    moduleProperty.saveValue(getUser(), c, FolderType.LibraryProtein.toString());
                }
                else
                {
                    moduleProperty.saveValue(getUser(), c, FolderType.Library.toString());
                }


                // Add the appropriate web parts to the page
                ArrayList<Portal.WebPart> tab1 = new ArrayList<>();
                tab1.add(Portal.getPortalPart(TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD).createWebPart());
                tab1.add(Portal.getPortalPart(MASS_SPEC_SEARCH_WEBPART).createWebPart());
                tab1.add(Portal.getPortalPart(TARGETED_MS_PEPTIDE_GROUP_VIEW).createWebPart());
                tab1.add(Portal.getPortalPart(TARGETED_MS_PEPTIDE_VIEW).createWebPart());
                Portal.saveParts(c, DefaultFolderType.DEFAULT_DASHBOARD, tab1);
                // Add a second portal page (tab) and webparts
                ArrayList<Portal.WebPart> tab2 = new ArrayList<>();
                tab2.add(Portal.getPortalPart(DATA_PIPELINE_WEBPART).createWebPart());
                Portal.saveParts(c, DATA_PINELINE_TAB, tab2);
                Portal.addProperty(c, DATA_PINELINE_TAB, Portal.PROP_CUSTOMTAB);

                return true;
            }
            else
            {
                return true;  // no option selected - do nothing
            }
        }

        @Override
        public URLHelper getSuccessURL(FolderSetupForm folderSetupForm)
        {
            return getContainer().getStartURL(getUser());
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a list of uploaded documents
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(AdminPermission.class)
    public class SetupAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            JspView view = new JspView("/org/labkey/targetedms/view/folderSetup.jsp");
            view.setFrame(WebPartView.FrameType.NONE);

            getPageConfig().setNavTrail(ContainerManager.getCreateContainerWizardSteps(getContainer(), getContainer().getParent()));
            getPageConfig().setTemplate(PageConfig.Template.Wizard);
            getPageConfig().setTitle(CONFIGURE_TARGETED_MS_FOLDER);

            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a list of chromatogram library archived revisions
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ArchivedRevisionsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            JspView view = new JspView("/org/labkey/targetedms/view/archivedRevisionsDownload.jsp");
            getPageConfig().setTitle("Download Archived Revisions");

            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }


    // ------------------------------------------------------------------------
    // Action to show a list of uploaded documents
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    @ActionNames("showList, begin")
    public class ShowListAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            QueryView gridView = ExperimentService.get().createExperimentRunWebPart(getViewContext(), EXP_RUN_TYPE);
            gridView.setTitle(TARGETED_MS_RUNS_WEBPART_NAME);
            gridView.setTitleHref(new ActionURL(TargetedMSController.ShowListAction.class, getContainer()));
            VBox vbox = new VBox();
            vbox.addView(new JspView("/org/labkey/targetedms/view/conflictSummary.jsp"));
            vbox.addView(gridView);
            return vbox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("Skyline Documents");
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Chromatogram actions
    // ------------------------------------------------------------------------

    @RequiresPermissionClass(ReadPermission.class)
    public class TransitionChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            TransitionChromInfo tci = TransitionManager.getTransitionChromInfo(getContainer(), form.getId());
            if (tci == null)
            {
                throw new NotFoundException("No such TransitionChromInfo found in this folder: " + form.getId());
            }
            PrecursorChromInfo pci = PrecursorManager.getPrecursorChromInfo(getContainer(), tci.getPrecursorChromInfoId());
            if (pci == null)
            {
                throw new NotFoundException("No such PrecursorChromInfo found in this folder: " + tci.getPrecursorChromInfoId());
            }

            JFreeChart chart = ChromatogramChartMakerFactory.createTransitionChromChart(tci, pci);

            response.setContentType("image/png");
            writePNG(form, response, chart);
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PrecursorChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            PrecursorChromInfo pChromInfo = PrecursorManager.getPrecursorChromInfo(getContainer(), form.getId());
            if (pChromInfo == null)
            {
                throw new NotFoundException("No PrecursorChromInfo found in this folder for precursorChromInfoId: " + form.getId());
            }

            JFreeChart chart = ChromatogramChartMakerFactory.createPrecursorChromChart(pChromInfo, form.isSyncY(), form.isSyncX());
            response.setContentType("image/png");
            writePNG(form, response, chart);
        }
    }

    private void writePNG(AbstractChartForm form, HttpServletResponse response, JFreeChart chart)
            throws IOException
    {
        // TODO: Remove try/catch once we require Java 7
        // Ignore known Java 6 issue with sun.font.FileFontStrike.getCachedGlyphPtr(), http://bugs.sun.com/view_bug.do?bug_id=7007299
        try
        {
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, form.getChartWidth(), form.getChartHeight());
        }
        catch (NullPointerException e)
        {
            if ("getCachedGlyphPtr".equals(e.getStackTrace()[0].getMethodName()))
                LOG.warn("Ignoring known synchronization issue with FileFontStrike.getCachedGlyphPtr()", e);
            else
                throw e;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PeptideChromatogramChartAction extends ExportAction<ChromatogramForm>
    {
        @Override
        public void export(ChromatogramForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            PeptideChromInfo pChromInfo = PeptideManager.getPeptideChromInfo(getContainer(), form.getId());
            if (pChromInfo == null)
            {
                throw new NotFoundException("No PeptideChromInfo found in this folder for peptideChromInfoId: " + form.getId());
            }

            JFreeChart chart = ChromatogramChartMakerFactory.createPeptideChromChart(pChromInfo, form.isSyncY(), form.isSyncX());
            response.setContentType("image/png");
            writePNG(form, response, chart);
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class PrecursorAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private int _peptideId; // save for use in appendNavTrail

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors) throws Exception
        {
            int precursorId = form.getId();
            Precursor precursor = PrecursorManager.getPrecursor(getContainer(), precursorId);
            if (precursor == null)
            {
                throw new NotFoundException("No such Precursor found in this folder: " + precursorId);
            }

            _run = TargetedMSManager.getRunForPrecursor(precursorId);
            _peptideId = precursor.getPeptideId();

            Peptide peptide = PeptideManager.get(precursor.getPeptideId());

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            PeptideSettings.IsotopeLabel label = IsotopeLabelManager.getIsotopeLabel(precursor.getIsotopeLabelId());

            PrecursorChromatogramsViewBean bean = new PrecursorChromatogramsViewBean(
                    new ActionURL(PrecursorAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );

            bean.setForm(form);
            bean.setPrecursor(precursor);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setIsotopeLabel(label);
            bean.setRun(_run);

            JspView<PrecursorChromatogramsViewBean> precursorInfo = new JspView<PrecursorChromatogramsViewBean>("/org/labkey/targetedms/view/precursorChromatogramsView.jsp", bean);
            precursorInfo.setFrame(WebPartView.FrameType.PORTAL);
            precursorInfo.setTitle("Precursor");

            PrecursorChromatogramsTableInfo tableInfo = new PrecursorChromatogramsTableInfo(new TargetedMSSchema(getUser(), getContainer()));
            tableInfo.setPrecursorId(precursorId);
            tableInfo.addPrecursorFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                                                                   ChromatogramsDataRegion.PRECURSOR_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);

            VBox vbox = new VBox();
            vbox.addView(precursorInfo);
            vbox.addView(gridView);
            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));

                root.addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()));

                ActionURL precChromUrl = new ActionURL(PeptideAllChromatogramsChartAction.class, getContainer());
                precChromUrl.addParameter("id", String.valueOf(_peptideId));
                root.addChild("Peptide Chromatograms", precChromUrl);

                root.addChild("Precursor Chromatograms");
            }
            return root;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class PeptideAllChromatogramsChartAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors) throws Exception
        {
            int peptideId = form.getId();
            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if (peptide == null)
            {
                throw new NotFoundException("No such Peptide found in this folder: " + peptideId);
            }

            _run = TargetedMSManager.getRunForPeptide(peptideId);

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            PeptideChromatogramsViewBean bean = new PeptideChromatogramsViewBean(
                    new ActionURL(PeptideAllChromatogramsChartAction.class, getContainer()).getLocalURIString()
            );

            bean.setForm(form);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setRun(_run);
            bean.setLabels(IsotopeLabelManager.getIsotopeLabels(_run.getId()));
            bean.setPrecursorList(PrecursorManager.getPrecursorsForPeptide(peptide.getId()));

            JspView<PeptideChromatogramsViewBean> peptideInfo = new JspView<PeptideChromatogramsViewBean>("/org/labkey/targetedms/view/peptideSummaryView.jsp", bean);
            peptideInfo.setFrame(WebPartView.FrameType.PORTAL);
            peptideInfo.setTitle("Peptide");

            PeptideChromatogramsTableInfo tableInfo = new PeptideChromatogramsTableInfo(new TargetedMSSchema(getUser(), getContainer()));
            tableInfo.setPeptideId(peptideId);
            tableInfo.addPeptideFilter();

            ChromatogramsDataRegion dRegion = new ChromatogramsDataRegion(getViewContext(), tableInfo,
                                                              ChromatogramsDataRegion.PEPTIDE_CHROM_DATA_REGION);
            GridView gridView = new GridView(dRegion, errors);

            VBox vbox = new VBox();
            vbox.addView(peptideInfo);
            vbox.addView(gridView);
            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild("Peptide Chromatograms");
            }
            return root;
        }
    }

    public static class PrecursorChromatogramsViewBean extends PeptideChromatogramsViewBean
    {
        private Precursor _precursor;
        private PeptideSettings.IsotopeLabel _isotopeLabel;

        public PrecursorChromatogramsViewBean(String resultsUri)
        {
            super(resultsUri);
        }

        public Precursor getPrecursor()
        {
            return _precursor;
        }

        public void setPrecursor(Precursor precursor)
        {
            _precursor = precursor;
        }

        public String getModifiedPeptideHtml()
        {
           return new ModifiedPeptideHtmlMaker().getHtml(getPrecursor());
        }

        public PeptideSettings.IsotopeLabel getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(PeptideSettings.IsotopeLabel isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }
    }

    public static class PeptideChromatogramsViewBean
    {
        private ChromatogramForm _form;
        private Peptide _peptide;
        private PeptideGroup _peptideGroup;
        private List<Precursor> _precursorList;
        private int _lightIsotopeLableId;
        private List<PeptideSettings.IsotopeLabel> labels;
        private TargetedMSRun _run;
        protected String _resultsUri;

        public PeptideChromatogramsViewBean(String resultsUri)
        {
            _resultsUri = resultsUri;
        }
        public String getResultsUri()
        {
            return _resultsUri;
        }

        public ChromatogramForm getForm()
        {
            return _form;
        }

        public void setForm(ChromatogramForm form)
        {
            _form = form;
        }

        public Peptide getPeptide()
        {
            return _peptide;
        }

        public void setPeptide(Peptide peptide)
        {
            _peptide = peptide;
        }

        public TargetedMSRun getRun()
        {
            return _run;
        }

        public void setRun(TargetedMSRun run)
        {
            _run = run;
        }

        public PeptideGroup getPeptideGroup()
        {
            return _peptideGroup;
        }

        public void setPeptideGroup(PeptideGroup peptideGroup)
        {
            _peptideGroup = peptideGroup;
        }

        public List<Precursor> getPrecursorList()
        {
            return _precursorList;
        }

        public void setPrecursorList(List<Precursor> precursorList)
        {
            _precursorList = precursorList;
        }

        public int getLightIsotopeLableId()
        {
            return _lightIsotopeLableId;
        }

        public void setLightIsotopeLableId(int lightIsotopeLableId)
        {
            _lightIsotopeLableId = lightIsotopeLableId;
        }

        public List<PeptideSettings.IsotopeLabel> getLabels()
        {
            return labels;
        }

        public void setLabels(List<PeptideSettings.IsotopeLabel> labels)
        {
            this.labels = labels;
        }
    }

    public static class ChromatogramForm extends AbstractChartForm
    {
        private int _id;
        private boolean _syncY = false;
        private boolean _syncX = false;

        public ChromatogramForm()
        {
            setChartWidth(400);
        }

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }

        public boolean isSyncY()
        {
            return _syncY;
        }

        public void setSyncY(boolean syncY)
        {
            _syncY = syncY;
        }

        public boolean isSyncX()
        {
            return _syncX;
        }

        public void setSyncX(boolean syncX)
        {
            _syncX = syncX;
        }
    }


    // ------------------------------------------------------------------------
    // Action to display peptide details page
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptideAction extends SimpleViewAction<ChromatogramForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private String _sequence;

        @Override
        public ModelAndView getView(ChromatogramForm form, BindException errors) throws Exception
        {
            int peptideId = form.getId();  // peptide Id

            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if(peptide == null)
            {
                throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", peptideId));
            }
            _sequence = peptide.getSequence();

            VBox vbox = new VBox();

            _run = TargetedMSManager.getRunForPeptide(peptideId);

            PeptideGroup pepGroup = PeptideGroupManager.get(peptide.getPeptideGroupId());

            List<Precursor> precursorList = PrecursorManager.getPrecursorsForPeptide(peptide.getId());

            List<PeptideSettings.IsotopeLabel> labels = IsotopeLabelManager.getIsotopeLabels(_run.getId());

            PeptideChromatogramsViewBean bean = new PeptideChromatogramsViewBean(
                new ActionURL(ShowPeptideAction.class, getContainer()).getLocalURIString());
            bean.setForm(form);
            bean.setPeptide(peptide);
            bean.setPeptideGroup(pepGroup);
            bean.setPrecursorList(precursorList);
            bean.setLightIsotopeLableId(labels.get(0).getId());
            bean.setLabels(labels);
            bean.setRun(_run);

            // summary for this peptide
            JspView<PeptideChromatogramsViewBean> peptideInfo = new JspView<PeptideChromatogramsViewBean>("/org/labkey/targetedms/view/peptideSummaryView.jsp", bean);
            peptideInfo.setFrame(WebPartView.FrameType.PORTAL);
            peptideInfo.setTitle("Peptide Summary");
            vbox.addView(peptideInfo);

            // precursor and transition chromatograms. One row per replicate
            JspView<PeptideChromatogramsViewBean> chartForm = new JspView<PeptideChromatogramsViewBean>("/org/labkey/targetedms/view/chromatogramsForm.jsp", bean);
            PeptidePrecursorChromatogramsView chromView = new PeptidePrecursorChromatogramsView(peptide, new TargetedMSSchema(getUser(), getContainer()),
                                                                                                form, errors);
            chromView.enableExpandCollapse(PeptidePrecursorChromatogramsView.TITLE, false);
            vbox.addView(chartForm);
            vbox.addView(chromView);

             // Peak area graph for the peptide
            PeakAreaGraphBean peakAreasBean = new PeakAreaGraphBean();
            peakAreasBean.setPeptideId(peptideId);
            peakAreasBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(_run.getId()));


            JspView<PeakAreaGraphBean> peakAreaView = new JspView<PeakAreaGraphBean>("/org/labkey/targetedms/view/peptidePeakAreaView.jsp",
                                                                                                   peakAreasBean);
            peakAreaView.setTitle("Peak Areas");
            peakAreaView.enableExpandCollapse("PeakAreasView", false);

            vbox.addView(peakAreaView);


            // library spectrum
            List<LibrarySpectrumMatch> libSpectraMatchList = LibrarySpectrumMatchGetter.getMatches(peptide);
            int idx = 0;
            for(LibrarySpectrumMatch libSpecMatch: libSpectraMatchList)
            {
                libSpecMatch.setLorikeetId(idx++);
                PeptideSpectrumView spectrumView = new PeptideSpectrumView(libSpecMatch, errors);
                spectrumView.enableExpandCollapse("PeptideSpectrumView", false);
                vbox.addView(spectrumView);
            }

            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (null != _run)
            {
                root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
                root.addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()));
                root.addChild(_sequence);
            }
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a library spectrum
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowSpectrumAction extends SimpleViewAction<ShowSpectrumForm>
    {
        @Override
        public ModelAndView getView(ShowSpectrumForm form, BindException errors) throws Exception
        {
            int peptideId = form.getId();  // peptide Id

            Peptide peptide = PeptideManager.getPeptide(getContainer(), peptideId);
            if(peptide == null)
            {
                throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", peptideId));
            }

            VBox vbox = new VBox();
            List<LibrarySpectrumMatch> libSpectraMatchList = LibrarySpectrumMatchGetter.getMatches(peptide);
            int idx = 0;
            for(LibrarySpectrumMatch libSpecMatch: libSpectraMatchList)
            {
                libSpecMatch.setLorikeetId(idx++);
                PeptideSpectrumView spectrumView = new PeptideSpectrumView(libSpecMatch, errors);
                spectrumView.enableExpandCollapse("PeptideSpectrumView", false);
                vbox.addView(spectrumView);
            }
            return vbox;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;  //TODO: link back to peptides details page
        }
    }

    public static class ShowSpectrumForm
    {
        private int _id;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a peak areas for peptides of a protein
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPeptidePeakAreasAction extends ExportAction<ShowPeakAreaForm>
    {
        @Override
        public void export(ShowPeakAreaForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            PeptideGroup peptideGrp = null;
            if(form.getPeptideGroupId() != 0)
            {
                peptideGrp = PeptideGroupManager.getPeptideGroup(getContainer(), form.getPeptideGroupId());
                if(peptideGrp == null)
                {
                    throw new NotFoundException(String.format("No peptide group found in this folder for peptideGroupId: %d", form.getPeptideGroupId()));
                }
            }

            Peptide peptide = null;
            if(form.getPeptideId() != 0)
            {
                peptide = PeptideManager.getPeptide(getContainer(), form.getPeptideId());
                if(peptide == null)
                {
                    throw new NotFoundException(String.format("No peptide found in this folder for peptideId: %d", form.getPeptideId()));
                }

                if(peptideGrp == null)
                {
                    peptideGrp = PeptideGroupManager.getPeptideGroup(getContainer(), peptide.getPeptideGroupId());
                }
            }

            JFreeChart chart = new PrecursorPeakAreaChartMaker().make(peptideGrp,
                                                                 form.getReplicateId(),
                                                                 peptide,
                                                                 form.getGroupByReplicateAnnotName(),
                                                                 form.isCvValues());
            if(chart != null)
            {
                response.setContentType("image/png");
                writePNG(form, response, chart);
            }
            else
            {
                chart = createEmptyChart();
                form.setChartHeight(20);
                form.setChartWidth(300);
                response.setContentType("image/png");
                writePNG(form, response, chart);
            }
        }

        private JFreeChart createEmptyChart()
        {
            JFreeChart chart = ChartFactory.createBarChart("", "", "", null, PlotOrientation.VERTICAL, false, false, false);
            chart.setTitle(new TextTitle("No chromatogram data found.", new java.awt.Font("SansSerif", Font.PLAIN, 12)));
            return chart;
        }
    }

    public abstract static class AbstractChartForm
    {
        private int _chartWidth = 600;
        private int _chartHeight = 400;

        public int getChartWidth()
        {
            return _chartWidth;
        }

        public void setChartWidth(int chartWidth)
        {
            _chartWidth = chartWidth;
        }

        public int getChartHeight()
        {
            return _chartHeight;
        }

        public void setChartHeight(int chartHeight)
        {
            _chartHeight = chartHeight;
        }
    }

    public static class ShowPeakAreaForm extends AbstractChartForm
    {
        private int _peptideGroupId;
        private int _replicateId = 0; // A value of 0 means all replicates should be included in the plot.
        private int _peptideId = 0;
        private String _groupByReplicateAnnotName;
        private boolean _cvValues;

        public int getPeptideGroupId()
        {
            return _peptideGroupId;
        }

        public void setPeptideGroupId(int peptideGroupId)
        {
            _peptideGroupId = peptideGroupId;
        }

        public int getReplicateId()
        {
            return _replicateId;
        }

        public void setReplicateId(int replicateId)
        {
            _replicateId = replicateId;
        }

        public String getGroupByReplicateAnnotName()
        {
            return _groupByReplicateAnnotName;
        }

        public void setGroupByReplicateAnnotName(String groupByReplicateAnnotName)
        {
            _groupByReplicateAnnotName = groupByReplicateAnnotName;
        }

        public int getPeptideId()
        {
            return _peptideId;
        }

        public void setPeptideId(int peptideId)
        {
            _peptideId = peptideId;
        }

        public boolean isCvValues()
        {
            return _cvValues;
        }

        public void setCvValues(boolean cvValues)
        {
            _cvValues = cvValues;
        }
    }

    public static class SkylinePipelinePathForm extends PipelinePathForm
    {
        private String[] _proteinRepresentative = new String[0];
        private String[] _peptideRepresentative = new String[0];

        public String[] getProteinRepresentative()
        {
            return _proteinRepresentative;
        }

        public void setProteinRepresentative(String[] representative)
        {
            _proteinRepresentative = representative;
        }

        public String[] getPeptideRepresentative()
        {
            return _peptideRepresentative;
        }

        public void setPeptideRepresentative(String[] peptideRepresentative)
        {
            _peptideRepresentative = peptideRepresentative;
        }

        @Override
        public List<File> getValidatedFiles(Container c)
        {
            List<File> files = super.getValidatedFiles(c);
            List<File> resolvedFiles = new ArrayList<File>(files.size());
            for(File file: files)
            {
                resolvedFiles.add(FileUtil.resolveFile(file));  // Strips out ".." and "." from the path
            }
            return resolvedFiles;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class SkylineDocUploadOptionsAction extends FormViewAction<SkylinePipelinePathForm>
    {
        @Override
        public void validateCommand(SkylinePipelinePathForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(SkylinePipelinePathForm form, boolean reshow, BindException errors) throws Exception
        {
            form.getValidatedFiles(getContainer());
            return new JspView<SkylinePipelinePathForm>("/org/labkey/targetedms/view/confirmImport.jsp", form, errors);
        }

        @Override
        public boolean handlePost(SkylinePipelinePathForm form, BindException errors) throws Exception
        {
            return false;
        }

        @Override
        public URLHelper getSuccessURL(SkylinePipelinePathForm form)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Confirm TargetedMS Data Import");
        }
    }

    // ------------------------------------------------------------------------
    // Document upload action
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(InsertPermission.class)
    public class SkylineDocUploadAction extends RedirectAction<SkylinePipelinePathForm>
    {
        public ActionURL getSuccessURL(SkylinePipelinePathForm form)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        public void validateCommand(SkylinePipelinePathForm form, Errors errors)
        {
        }

        public boolean doAction(SkylinePipelinePathForm form, BindException errors) throws Exception
        {
            for (File file : form.getValidatedFiles(getContainer()))
            {
                if (!file.isFile())
                {
                    throw new NotFoundException("Expected a file but found a directory: " + file.getName());
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    TargetedMSManager.addRunToQueue(info, file, form.getPipeRoot(getContainer()));
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                catch (SQLException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class SkylineDocUploadApiAction extends ApiAction<PipelinePathForm>
    {
        public ApiResponse execute(PipelinePathForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            List<Map<String, Object>> jobDetailsList = new ArrayList<Map<String, Object>>();

            for (File file : form.getValidatedFiles(getContainer()))
            {
                if (!file.isFile())
                {
                    throw new NotFoundException("Expected a file but found a directory: " + file.getName());
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    int jobId = TargetedMSManager.addRunToQueue(info, file, form.getPipeRoot(getContainer()));
                    Map<String, Object> detailsMap = new HashMap<String, Object>(4);
                    detailsMap.put("Path", form.getPath());
                    detailsMap.put("File",file.getName());
                    detailsMap.put("RowId", jobId);
                    jobDetailsList.add(detailsMap);
                }
                catch (IOException e)
                {
                    throw new ApiUsageException(e);
                }
                catch (SQLException e)
                {
                    throw new ApiUsageException(e);
                }
            }
            response.put("UploadedJobDetails", jobDetailsList);
            return response;
        }
    }

    // ------------------------------------------------------------------------
    // Action to display a document's transition or precursor list
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public abstract class ShowRunDetailsAction <VIEWTYPE extends NestableQueryView> extends QueryViewAction<RunDetailsForm, VIEWTYPE>
    {
        protected TargetedMSRun _run;  // save for use in appendNavTrail

        public ShowRunDetailsAction()
        {
            super(RunDetailsForm.class);
        }

        public ModelAndView getHtmlView(final RunDetailsForm form, BindException errors) throws Exception
        {
            //this action requires that a specific experiment run has been specified
            if(!form.hasRunId())
                throw new RedirectException(new ActionURL(ShowListAction.class, getViewContext().getContainer()));

            //ensure that the experiment run is valid and exists within the current container
            _run = validateRun(form.getId());

            VBox vBox = new VBox();

            RunDetailsBean bean = new RunDetailsBean();
            bean.setForm(form);
            bean.setRun(_run);

            JspView<RunDetailsBean> runSummaryView = new JspView<RunDetailsBean>("/org/labkey/targetedms/view/runSummaryView.jsp", bean);
            runSummaryView.setFrame(WebPartView.FrameType.PORTAL);
            runSummaryView.setTitle("Document Summary");

            vBox.addView(runSummaryView);

            VIEWTYPE view = createInitializedQueryView(form, errors, false, getDataRegionName());
            vBox.addView(view);

            NavTree menu = getViewSwitcherMenu();
            view.setNavMenu(menu);
            return vBox;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            NavTree navTree = appendNavTrail(root, _run);
            if (_run != null)
            {
                navTree = navTree.addChild(_run.getDescription());
            }
            return navTree;
        }

        public NavTree appendNavTrail(NavTree root, TargetedMSRun run)
        {
            return root.addChild("Targeted MS Runs", getShowListURL(getContainer()));
        }

        public abstract String getDataRegionName();

        public abstract NavTree getViewSwitcherMenu();

    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowTransitionListAction extends ShowRunDetailsAction<DocumentTransitionsView>
    {
        @Override
        protected DocumentTransitionsView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            DocumentTransitionsView view = new DocumentTransitionsView(getViewContext(),
                                                                       new TargetedMSSchema(getUser(), getContainer()),
                                                                       form.getId(),
                                                                       forExport);
            view.setShowExportButtons(true);
            return view;
        }

        @Override
        public String getDataRegionName()
        {
            return DocumentTransitionsView.DATAREGION_NAME;
        }

        @Override
        public NavTree getViewSwitcherMenu()
        {
            NavTree menu = new NavTree();
            ActionURL url = new ActionURL(ShowPrecursorListAction.class, getContainer());
            url.addParameter("id", _run.getId());

            menu.addChild(DocumentPrecursorsView.TITLE, url);
            return menu;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ShowPrecursorListAction extends ShowRunDetailsAction<DocumentPrecursorsView>
    {
        @Override
        protected DocumentPrecursorsView createQueryView(RunDetailsForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            DocumentPrecursorsView view = new DocumentPrecursorsView(getViewContext(),
                                                                   new TargetedMSSchema(getUser(), getContainer()),
                                                                   form.getId(),
                                                                   forExport);
            view.setShowExportButtons(true);
            view.setShowDetailsColumn(false);
            return view;
        }

        @Override
        public String getDataRegionName()
        {
            return DocumentPrecursorsView.DATAREGION_NAME;
        }

        @Override
        public NavTree getViewSwitcherMenu()
        {
            NavTree menu = new NavTree();
            ActionURL url = new ActionURL(ShowTransitionListAction.class, getContainer());
            url.addParameter("id", _run.getId());

            menu.addChild(DocumentTransitionsView.TITLE, url);
            return menu;
        }
    }

    public static class RunDetailsForm extends QueryViewAction.QueryExportForm
    {
        private int _id = 0;
        private String _view;

        public void setId(int id)
        {
            _id = id;
        }

        public int getId()
        {
            return _id;
        }

        public boolean hasRunId() {
            return _id > 0;
        }

        public String getView()
        {
            return _view;
        }

        public void setView(String view)
        {
            _view = view;
        }
    }

    public static class RunDetailsBean
    {
        private RunDetailsForm _form;
        private TargetedMSRun _run;

        public RunDetailsForm getForm()
        {
            return _form;
        }

        public void setForm(RunDetailsForm form)
        {
            _form = form;
        }

        public TargetedMSRun getRun()
        {
            return _run;
        }

        public void setRun(TargetedMSRun run)
        {
            _run = run;
        }
    }

    @NotNull
    private TargetedMSRun validateRun(int runId)
    {
        Container c = getContainer();
        TargetedMSRun run = TargetedMSManager.getRun(runId);

        if (null == run)
            throw new NotFoundException("Run " + runId + " not found");
        if (run.isDeleted())
            throw new NotFoundException("Run has been deleted.");
        if (run.getStatusId() == SkylineDocImporter.STATUS_RUNNING)
            throw new NotFoundException("Run is still loading.  Current status: " + run.getStatus());
        if (run.getStatusId() == SkylineDocImporter.STATUS_FAILED)
            throw new NotFoundException("Run failed loading.  Status: " + run.getStatus());

        Container container = run.getContainer();

        if (null == container || !container.equals(c))
        {
            ActionURL url = getViewContext().getActionURL().clone();
            url.setContainer(run.getContainer());
            throw new RedirectException(url);
        }

        return run;
    }

    // ------------------------------------------------------------------------
    // Action to show a protein detail page
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinAction extends SimpleViewAction<ProteinDetailForm>
    {
        private TargetedMSRun _run; // save for use in appendNavTrail
        private String _proteinLabel;

        @Override
        public ModelAndView getView(final ProteinDetailForm form, BindException errors) throws Exception
        {
            PeptideGroup group = PeptideGroupManager.getPeptideGroup(getContainer(), form.getId());
            if (group == null)
            {
                throw new NotFoundException("Could not find peptide group #" + form.getId());
            }

            _run = TargetedMSManager.getRun(group.getRunId());
            _proteinLabel = group.getLabel();

            // Peptide group details
            DataRegion groupDetails = new DataRegion();
            TargetedMSSchema schema = new TargetedMSSchema(getUser(), getContainer());
            TableInfo tableInfo = schema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
            groupDetails.setColumns(tableInfo.getColumns("Label", "Description", "Decoy", "Note", "RunId"));
            groupDetails.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY);
            DetailsView groupDetailsView = new DetailsView(groupDetails, form.getId());
            groupDetailsView.setTitle("Peptide Group");

            VBox result = new VBox(groupDetailsView);


            // Protein sequence coverage
            if (group.getSequenceId() != null)
            {
                int seqId = group.getSequenceId().intValue();
                List<String> peptideSequences = new ArrayList<String>();
                for (Peptide peptide : PeptideManager.getPeptidesForGroup(group.getId()))
                {
                    peptideSequences.add(peptide.getSequence());
                }

                ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
                WebPartView sequenceView = proteinService.getProteinCoverageView(seqId, peptideSequences.toArray(new String[peptideSequences.size()]), 100, true);

                sequenceView.setTitle("Sequence Coverage");
                sequenceView.enableExpandCollapse("SequenceCoverage", false);
                result.addView(sequenceView);

                result.addView(proteinService.getAnnotationsView(seqId));
            }

            // List of peptides
            QuerySettings settings = new QuerySettings(getViewContext(), "Peptides", "Peptide");
            QueryView peptidesView = new QueryView(schema, settings, errors)
            {
                @Override
                protected TableInfo createTable()
                {
                    TargetedMSTable result = (TargetedMSTable) super.createTable();
                    result.addCondition(new SimpleFilter(FieldKey.fromParts("PeptideGroupId"), form.getId()));
                    List<FieldKey> visibleColumns = new ArrayList<FieldKey>();
                    visibleColumns.add(FieldKey.fromParts("Sequence"));
                    visibleColumns.add(FieldKey.fromParts("CalcNeutralMass"));
                    visibleColumns.add(FieldKey.fromParts("NumMissedCleavages"));
                    visibleColumns.add(FieldKey.fromParts("Rank"));
                    visibleColumns.add(FieldKey.fromParts("AvgMeasuredRetentionTime"));
                    visibleColumns.add(FieldKey.fromParts("PredictedRetentionTime"));
                    result.setDefaultVisibleColumns(visibleColumns);
                    return result;
                }
            };
            peptidesView.setTitle("Peptides");
            peptidesView.enableExpandCollapse("TargetedMSPeptides", false);
            peptidesView.setUseQueryViewActionExportURLs(true);
            result.addView(peptidesView);


            // Peptide peak areas
            PeakAreaGraphBean peakAreasBean = new PeakAreaGraphBean();
            peakAreasBean.setPeptideGroupId(form.getId());
            peakAreasBean.setReplicateList(ReplicateManager.getReplicatesForRun(group.getRunId()));
            peakAreasBean.setReplicateAnnotationNameList(ReplicateManager.getReplicateAnnotationNamesForRun(group.getRunId()));
            peakAreasBean.setPeptideList(new ArrayList<Peptide>(PeptideManager.getPeptidesForGroup(group.getId())));


            //peakAreaUrl.addParameter("sampleFileId", sampleFiles.get(0).getId());
            JspView<PeakAreaGraphBean> peakAreaView = new JspView<PeakAreaGraphBean>("/org/labkey/targetedms/view/peptidePeakAreaView.jsp",
                                                                                      peakAreasBean);
            peakAreaView.setTitle("Peak Areas");
            peakAreaView.enableExpandCollapse("PeakAreasView", false);

            result.addView(peakAreaView);

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return new ShowPrecursorListAction().appendNavTrail(root, _run)
                                                .addChild(_run.getFileName(), getShowRunURL(getContainer(), _run.getId()))
                                                .addChild(_proteinLabel);
        }
    }

    public static class ProteinDetailForm
    {
        private int _id;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }
    }

    public static class PeakAreaGraphBean
    {
        private int _peptideGroupId;
        private int _peptideId;
        private List<Replicate> _replicateList;
        private List<String> _replicateAnnotationNameList;
        private List<Peptide> _peptideList;

        public int getPeptideGroupId()
        {
            return _peptideGroupId;
        }

        public void setPeptideGroupId(int peptideGroupId)
        {
            _peptideGroupId = peptideGroupId;
        }

        public int getPeptideId()
        {
            return _peptideId;
        }

        public void setPeptideId(int peptideId)
        {
            _peptideId = peptideId;
        }

        public List<Replicate> getReplicateList()
        {
            return _replicateList != null ? _replicateList : Collections.<Replicate>emptyList();
        }

        public void setReplicateList(List<Replicate> replicateList)
        {
            _replicateList = replicateList;
        }

        public List<String> getReplicateAnnotationNameList()
        {
            return _replicateAnnotationNameList != null ? _replicateAnnotationNameList : Collections.<String>emptyList();
        }

        public void setReplicateAnnotationNameList(List<String> replicateAnnotationNameList)
        {
            _replicateAnnotationNameList = replicateAnnotationNameList;
        }

        public List<Peptide> getPeptideList()
        {
            return _peptideList != null ? _peptideList : Collections.<Peptide>emptyList();
        }

        public void setPeptideList(List<Peptide> peptideList)
        {
            _peptideList = peptideList;
        }
    }

    // ------------------------------------------------------------------------
    // Action to show a protein detail page
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class ShowProteinAJAXAction extends SimpleViewAction<ProteinDetailForm>
    {
        @Override
        public ModelAndView getView(ProteinDetailForm form, BindException errors) throws Exception
        {
            PeptideGroup group = PeptideGroupManager.getPeptideGroup(getContainer(), form.getId());
            if (group == null)
            {
                throw new NotFoundException("Could not find peptide group #" + form.getId());
            }

            if (group.getSequenceId()!= null)
            {
                int seqId = group.getSequenceId().intValue();
                List<String> peptideSequences = new ArrayList<String>();
                for (Peptide peptide : PeptideManager.getPeptidesForGroup(group.getId()))
                {
                    peptideSequences.add(peptide.getSequence());
                }
                ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
                ActionURL searchURL = PageFlowUtil.urlProvider(MS2Urls.class).getProteinSearchUrl(getContainer());
                searchURL.addParameter("seqId", group.getSequenceId().intValue());
                searchURL.addParameter("identifier", group.getLabel());
                getViewContext().getResponse().getWriter().write("<a href=\"" + searchURL + "\">Search for other references to this protein</a><br/>");
                WebPartView sequenceView = proteinService.getProteinCoverageView(seqId, peptideSequences.toArray(new String[peptideSequences.size()]), 40, true);
                sequenceView.render(getViewContext().getRequest(), getViewContext().getResponse());
            }

            getPageConfig().setTemplate(PageConfig.Template.None);
            return null;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // Action to show representative data conflicts, if any, in a container
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(InsertPermission.class)
    public class ShowProteinConflictUiAction extends SimpleViewAction<ConflictUIForm>
    {
        @Override
        public ModelAndView getView(ConflictUIForm form, BindException errors) throws Exception
        {
            List<ConflictProtein> conflictProteinList = ConflictResultsManager.getConflictedProteins(getContainer());
            // If the list contains the same conflicted proteins from multiple files return the ones from the
            // oldest run first.  Or, use the runId from the form if we are given one.
            int conflictRunId = form.getConflictedRunId();
            boolean useMin = false;
            if(conflictRunId == 0)
            {
                conflictRunId = Integer.MAX_VALUE;
                useMin = true;
            }
            String conflictRunFileName = null;
            Map<String, Integer> conflictRunFiles = new HashMap<String, Integer>();
            for(ConflictProtein cProtein: conflictProteinList)
            {
                if(useMin && (cProtein.getNewProteinRunId() < conflictRunId))
                {
                    conflictRunId = cProtein.getNewProteinRunId();
                    conflictRunFileName = cProtein.getNewRunFile();
                }
                else if(!useMin && (conflictRunId == cProtein.getNewProteinRunId()))
                {
                    conflictRunFileName = cProtein.getNewRunFile();
                }
                conflictRunFiles.put(cProtein.getNewRunFile(), cProtein.getNewProteinRunId());
            }

            //ensure that the run is valid and exists within the current container
            validateRun(conflictRunId);

            if(conflictRunFileName == null)
            {
                throw new NotFoundException("Run with ID "+conflictRunId+" does not have any protein conflicts.");
            }

            List<ConflictProtein> singleRunConflictProteins = new ArrayList<ConflictProtein>();
            for(ConflictProtein cProtein: conflictProteinList)
            {
                if(cProtein.getNewProteinRunId() != conflictRunId)
                    continue;
                singleRunConflictProteins.add(cProtein);
            }

            ProteinConflictBean bean = new ProteinConflictBean();
            bean.setCurrentConflictRunFile(conflictRunFileName);
            bean.setConflictProteinList(singleRunConflictProteins);
            if(conflictRunFiles.size() > 1)
            {
                bean.setAllConflictRunFiles(conflictRunFiles);
            }

            JspView<ProteinConflictBean> conflictInfo = new JspView<ProteinConflictBean>("/org/labkey/targetedms/view/proteinConflictResolutionView.jsp", bean);
            conflictInfo.setFrame(WebPartView.FrameType.PORTAL);
            conflictInfo.setTitle("Representative Protein Data Conflicts");

            return conflictInfo;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class ConflictUIForm
    {
        private int _conflictedRunId;

        public int getConflictedRunId()
        {
            return _conflictedRunId;
        }

        public void setConflictedRunId(int conflictedRunId)
        {
            _conflictedRunId = conflictedRunId;
        }
    }

    public static class ProteinConflictBean
    {
        private List<ConflictProtein> _conflictProteinList;
        private Map<String, Integer> _allConflictRunFiles;
        private String _conflictRunFileName;

        public List<ConflictProtein> getConflictProteinList()
        {
            return _conflictProteinList;
        }

        public void setConflictProteinList(List<ConflictProtein> conflictProteinList)
        {
            _conflictProteinList = conflictProteinList;
        }

        public Map<String, Integer> getAllConflictRunFiles()
        {
            return _allConflictRunFiles;
        }

        public void setAllConflictRunFiles(Map<String, Integer> allConflictRunFiles)
        {
            _allConflictRunFiles = allConflictRunFiles;
        }

        public void setCurrentConflictRunFile(String conflictRunFileName)
        {
            _conflictRunFileName = conflictRunFileName;
        }

        public String getConflictRunFileName()
        {
            return _conflictRunFileName;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ProteinConflictPeptidesAjaxAction extends ApiAction<ProteinPeptidesForm>
    {
        @Override
        public ApiResponse execute(ProteinPeptidesForm proteinPeptidesForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            int newProteinId = proteinPeptidesForm.getNewProteinId();
            if(PeptideGroupManager.getPeptideGroup(getContainer(), newProteinId) == null)
            {
                throw new NotFoundException("PeptideGroup with ID "+newProteinId+" was not found in the container.");
            }
            int oldProteinId = proteinPeptidesForm.getOldProteinId();
            if(PeptideGroupManager.getPeptideGroup(getContainer(), oldProteinId) == null)
            {
                throw new NotFoundException("PeptideGroup with ID "+oldProteinId+" was not found in the container.");
            }

            List<ConflictPeptide> conflictPeptides = ConflictResultsManager.getConflictPeptidesForProteins(newProteinId, oldProteinId);
            // Sort them by ascending peptide ranks in the new protein
            Collections.sort(conflictPeptides, new Comparator<ConflictPeptide>()
            {
                @Override
                public int compare(ConflictPeptide o1, ConflictPeptide o2)
                {
                    return Integer.valueOf(o1.getNewPeptideRank()).compareTo(o2.getNewPeptideRank());
                }
            });
            List<Map<String, Object>> conflictPeptidesMap = new ArrayList<Map<String, Object>>();
            for(ConflictPeptide peptide: conflictPeptides)
            {
                Map<String, Object> map = new HashMap<String, Object>();
                // PrecursorHtmlMaker.getHtml(peptide.getNewPeptide(), peptide.getNewPeptidePrecursor(), )
                String newPepSequence = peptide.getNewPeptide() != null ? peptide.getNewPeptide().getSequence() : "-";
                map.put("newPeptide", newPepSequence);
                String newPepRank = peptide.getNewPeptide() != null ? String.valueOf(peptide.getNewPeptideRank()) : "-";
                map.put("newPeptideRank", newPepRank);
                String oldPepSequence = peptide.getOldPeptide() != null ? peptide.getOldPeptide().getSequence() : "-";
                map.put("oldPeptide", oldPepSequence);
                String oldPepRank = peptide.getOldPeptide() != null ? String.valueOf(peptide.getOldPeptideRank()) : "-";
                map.put("oldPeptideRank",oldPepRank);
                conflictPeptidesMap.add(map);
            }

            response.put("conflictPeptides", conflictPeptidesMap);
            return response;
        }
    }

    public static class ProteinPeptidesForm
    {
        private int _newProteinId;
        private int _oldProteinId;

        public int getNewProteinId()
        {
            return _newProteinId;
        }

        public void setNewProteinId(int newProteinId)
        {
            _newProteinId = newProteinId;
        }

        public int getOldProteinId()
        {
            return _oldProteinId;
        }

        public void setOldProteinId(int oldProteinId)
        {
            _oldProteinId = oldProteinId;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ShowPrecursorConflictUiAction extends SimpleViewAction<ConflictUIForm>
    {
        @Override
        public ModelAndView getView(ConflictUIForm form, BindException errors) throws Exception
        {
            List<ConflictPrecursor> conflictPrecursorList = ConflictResultsManager.getConflictedPrecursors(getContainer());
            // If the list contains the same conflicted precursors from multiple files return the ones from the
            // oldest run first.  Or, use the runId from the form if we are given one.
            int conflictRunId = form.getConflictedRunId();
            boolean useMin = false;
            if(conflictRunId == 0)
            {
                conflictRunId = Integer.MAX_VALUE;
                useMin = true;
            }

            String conflictRunFileName = null;
            Map<String, Integer> conflictRunFiles = new HashMap<String, Integer>();
            for(ConflictPrecursor cPrecursor: conflictPrecursorList)
            {
                if(useMin && cPrecursor.getNewPrecursorRunId() < conflictRunId)
                {
                    conflictRunId = cPrecursor.getNewPrecursorRunId();
                    conflictRunFileName = cPrecursor.getNewRunFile();
                }
                else if(!useMin && cPrecursor.getNewPrecursorRunId() == conflictRunId)
                {
                    conflictRunFileName = cPrecursor.getNewRunFile();
                }
                conflictRunFiles.put(cPrecursor.getNewRunFile(), cPrecursor.getNewPrecursorRunId());
            }

            //ensure that the run is valid and exists within the current container
            validateRun(conflictRunId);

            if(conflictRunFileName == null)
            {
                throw new NotFoundException("Run with ID "+conflictRunId+" does not have any peptide conflicts.");
            }

            List<ConflictPrecursor> singleRunConflictPrecursors = new ArrayList<ConflictPrecursor>();
            for(ConflictPrecursor cPrecursor: conflictPrecursorList)
            {
                if(cPrecursor.getNewPrecursorRunId() != conflictRunId)
                    continue;
                singleRunConflictPrecursors.add(cPrecursor);
            }

            PrecursorConflictBean bean = new PrecursorConflictBean();
            bean.setConflictRunFileName(conflictRunFileName);
            bean.setConflictPrecursorList(singleRunConflictPrecursors);
            if(conflictRunFiles.size() > 1)
            {
                bean.setAllConflictRunFiles(conflictRunFiles);
            }

            JspView<PrecursorConflictBean> conflictInfo = new JspView<PrecursorConflictBean>("/org/labkey/targetedms/view/precursorConflictResolutionView.jsp", bean);
            conflictInfo.setFrame(WebPartView.FrameType.PORTAL);
            conflictInfo.setTitle("Representative Peptide Data Conflicts");

            return conflictInfo;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class PrecursorConflictBean
    {
        private List<ConflictPrecursor> _conflictPrecursorList;
        private Map<String, Integer> _allConflictRunFiles;
        private String _conflictRunFileName;

        public List<ConflictPrecursor> getConflictPrecursorList()
        {
            return _conflictPrecursorList;
        }

        public void setConflictPrecursorList(List<ConflictPrecursor> conflictPrecursorList)
        {
            _conflictPrecursorList = conflictPrecursorList;
        }

        public Map<String, Integer> getAllConflictRunFiles()
        {
            return _allConflictRunFiles;
        }

        public void setAllConflictRunFiles(Map<String, Integer> allConflictRunFiles)
        {
            _allConflictRunFiles = allConflictRunFiles;
        }

        public String getConflictRunFileName()
        {
            return _conflictRunFileName;
        }

        public void setConflictRunFileName(String conflictRunFileName)
        {
            _conflictRunFileName = conflictRunFileName;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class PrecursorConflictTransitionsAjaxAction extends ApiAction<ConflictPrecursorsForm>
    {
        @Override
        public ApiResponse execute(ConflictPrecursorsForm conflictPrecursorsForm, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            int newPrecursorId = conflictPrecursorsForm.getNewPrecursorId();
            if(PrecursorManager.getPrecursor(getContainer(), newPrecursorId) == null)
            {
                throw new NotFoundException("Precursor with ID "+newPrecursorId+" was not found in the container.");
            }
            int oldPrecursorId = conflictPrecursorsForm.getOldPrecursorId();
            if(PrecursorManager.getPrecursor(getContainer(), oldPrecursorId) == null)
            {
                throw new NotFoundException("Precursor with ID "+oldPrecursorId+" was not found in the container.");
            }

            List<ConflictTransition> conflictTransitions = ConflictResultsManager.getConflictTransitionsForPrecursors(newPrecursorId, oldPrecursorId);
            // Sort them by ascending transitions ranks in the new precursor
            Collections.sort(conflictTransitions, new Comparator<ConflictTransition>()
            {
                @Override
                public int compare(ConflictTransition o1, ConflictTransition o2)
                {
                    return Integer.valueOf(o1.getNewTransitionRank()).compareTo(o2.getNewTransitionRank());
                }
            });
            List<Map<String, Object>> conflictTransitionsMap = new ArrayList<Map<String, Object>>();
            for(ConflictTransition transition: conflictTransitions)
            {
                Map<String, Object> map = new HashMap<String, Object>();
                String newTransitionLabel = transition.getNewTransition() != null ? transition.getNewTransition().getLabel() : "-";
                map.put("newTransition", newTransitionLabel);
                String newTransRank = transition.getNewTransition() != null ? String.valueOf(transition.getNewTransitionRank()) : "-";
                map.put("newTransitionRank", newTransRank);
                String oldTransLabel = transition.getOldTransition() != null ? transition.getOldTransition().getLabel() : "-";
                map.put("oldTransition", oldTransLabel);
                String oldPepRank = transition.getOldTransition() != null ? String.valueOf(transition.getOldTransitionRank()) : "-";
                map.put("oldTransitionRank",oldPepRank);
                conflictTransitionsMap.add(map);
            }

            response.put("conflictTransitions", conflictTransitionsMap);
            return response;
        }
    }

    public static class ConflictPrecursorsForm
    {
        private int _newPrecursorId;
        private int _oldPrecursorId;

        public int getNewPrecursorId()
        {
            return _newPrecursorId;
        }

        public void setNewPrecursorId(int newPrecursorId)
        {
            _newPrecursorId = newPrecursorId;
        }

        public int getOldPrecursorId()
        {
            return _oldPrecursorId;
        }

        public void setOldPrecursorId(int oldPrecursorId)
        {
            _oldPrecursorId = oldPrecursorId;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ResolveConflictAction extends RedirectAction<ResolveConflictForm>
    {
        @Override
        public URLHelper getSuccessURL(ResolveConflictForm resolveConflictForm)
        {
            return TargetedMSController.getShowListURL(getContainer());
        }

        @Override
        public void validateCommand(ResolveConflictForm form, Errors errors)
        {
        }

        @Override
        public boolean doAction(ResolveConflictForm resolveConflictForm, BindException errors) throws Exception
        {

            if(resolveConflictForm.getConflictLevel() == null)
            {
                errors.reject(ERROR_MSG, "Missing 'conflictLevel' parameter.");
                return false;
            }
            boolean resolveProtein = resolveConflictForm.getConflictLevel().equalsIgnoreCase("protein");
            boolean resolvePrecursor = resolveConflictForm.getConflictLevel().equalsIgnoreCase("peptide");
            if(!resolveProtein && !resolvePrecursor)
            {
                errors.reject(ERROR_MSG, resolveConflictForm.getConflictLevel() + " is an invalid value for 'conflictLevel' parameter."+
                              " Valid values are 'peptide' or 'protein'.");

                return false;
            }

            int[] selectedIds = resolveConflictForm.getSelectedIds();
            int[] deselectIds = resolveConflictForm.getDeselectedIds();
            if(selectedIds == null || selectedIds.length == 0)
            {
                errors.reject(ERROR_MSG, "No IDs were found to be marked as representative.");
                return false;
            }
            if(deselectIds == null || deselectIds.length == 0)
            {
                errors.reject(ERROR_MSG, "No IDs were found to be marked as deprecated.");
                return false;
            }

            // ensure that the peptide-group or precursor Ids belong to a run in the container
            if(resolveProtein)
            {
                if(!PeptideGroupManager.ensureContainerMembership(selectedIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the selected peptideGroupIds were not found in the container.");
                }
                if(!PeptideGroupManager.ensureContainerMembership(deselectIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the deselected peptideGroupIds were not found in the container.");
                }
            }
            if(resolvePrecursor)
            {
                if(!PrecursorManager.ensureContainerMembership(selectedIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the selected precursorIds were not found in the container.");
                }
                if(!PrecursorManager.ensureContainerMembership(deselectIds, getContainer()))
                {
                    throw new NotFoundException("One or more of the deselected precursorIds were not found in the container.");
                }
            }

            TargetedMSManager.getSchema().getScope().ensureTransaction();
            try {
                if(resolveProtein)
                {
                    // Set RepresentativeDataState to Representative.
                    PeptideGroupManager.updateRepresentativeStatus(selectedIds, RepresentativeDataState.Representative);

                    // Set to either NotRepresentative or Representative_Deprecated.
                    // If the original status was Representative it will be updated to Representative_Deprecated.
                    // If the original status was Conflicted it will be update to NotRepresentative.
                    PeptideGroupManager.updateStatusToDeprecatedOrNotRepresentative(deselectIds);

                    // If there are runs in the container that no longer have any representative data mark
                    // them as being not representative.
                    TargetedMSManager.markRunsNotRepresentative(getContainer(), TargetedMSRun.RepresentativeDataState.Representative_Protein);
                }
                else
                {
                    // Set RepresentativeDataState to Representative.
                    PrecursorManager.updateRepresentativeStatus(selectedIds, RepresentativeDataState.Representative);

                    // Set to either NotRepresentative or Representative_Deprecated.
                    // If the original status was Representative it will be updated to Representative_Deprecated.
                    // If the original status was Conflicted it will be update to NotRepresentative.
                    PrecursorManager.updateStatusToDeprecatedOrNotRepresentative(deselectIds);

                    // If there are runs in the container that no longer have any representative data mark
                    // them as being not representative.
                    TargetedMSManager.markRunsNotRepresentative(getContainer(), TargetedMSRun.RepresentativeDataState.Representative_Peptide);
                }

                TargetedMSManager.getSchema().getScope().commitTransaction();

                // Increment the chromatogram library revision number for this container.
                ChromatogramLibraryUtils.incrementLibraryRevision(getContainer());

                // Add event to audit log.
                TargetedMsRepresentativeStateAuditViewFactory.addAuditEntry(getContainer(), getUser(), "Conflict resolved.");
            }
            finally {

                TargetedMSManager.getSchema().getScope().closeConnection();
            }
            return true;
        }
    }

    public static class ResolveConflictForm
    {
        public String _conflictLevel; // Either 'peptide' or 'protein'
        public String[] _selectedInputValues;
        private int[] _selectedIds;
        private int[] _deselectedIds;

        public String getConflictLevel()
        {
            return _conflictLevel;
        }

        public void setConflictLevel(String conflictLevel)
        {
            _conflictLevel = conflictLevel;
        }

        public String[] getSelectedInputValues()
        {
            return _selectedInputValues;
        }

        public void setSelectedInputValues(String[] selectedInputValues)
        {
            _selectedInputValues = selectedInputValues;
            if(selectedInputValues != null)
            {
                _selectedIds = new int[_selectedInputValues.length];
                _deselectedIds = new int[_selectedInputValues.length];

                int count = 0;
                for(String value: _selectedInputValues)
                {
                    int idx = value.indexOf('_');
                    if(idx != -1)
                    {
                        int selected = Integer.parseInt(value.substring(0, idx));
                        int deselected = Integer.parseInt(value.substring(idx+1));
                        _selectedIds[count] = selected;
                        _deselectedIds[count] = deselected;
                        count++;
                    }
                }
            }
        }

        public int[] getSelectedIds()
        {
            return _selectedIds;
        }

        public int[] getDeselectedIds()
        {
            return _deselectedIds;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class ChangeRepresentativeStateAction extends RedirectAction<ChangeRepresentativeStateForm>
    {
        @Override
        public URLHelper getSuccessURL(ChangeRepresentativeStateForm changeStateForm)
        {
            ActionURL url = new ActionURL(ShowPrecursorListAction.class, getContainer());
            url.addParameter("id", changeStateForm.getRunId());
            return url;
        }

        @Override
        public void validateCommand(ChangeRepresentativeStateForm target, Errors errors)
        {
        }

        @Override
        public boolean doAction(ChangeRepresentativeStateForm changeStateForm, BindException errors) throws Exception
        {
            //ensure that the run is valid and exists within the current container
            TargetedMSRun run = validateRun(changeStateForm.getRunId());

            TargetedMSRun.RepresentativeDataState state = TargetedMSRun.RepresentativeDataState.valueOf(changeStateForm.getState());

            RepresentativeStateManager.setRepresentativeState(getUser(), getContainer(), run, state);
            return true;
        }
    }

    public static class ChangeRepresentativeStateForm
    {
        private int _runId;
        private String _state;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getState()
        {
            return _state;
        }

        public void setState(String state)
        {
            _state = state;
        }
    }

    // ------------------------------------------------------------------------
    // Action to download a Skyline zip file.
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadDocumentAction extends SimpleViewAction<DownloadDocumentForm>
    {
        public ModelAndView getView(DownloadDocumentForm form, BindException errors) throws Exception
        {
            if (form.getRunId() < 0)
            {
                throw new NotFoundException("No run ID specified.");
            }
            TargetedMSRun run = validateRun(form.getRunId());
            ExpRun expRun = ExperimentService.get().getExpRun(run.getExperimentRunLSID());
            if (expRun == null)
            {
                throw new NotFoundException("Run " + run.getExperimentRunLSID() + " does not exist.");
            }

            ExpData[] inputDatas = expRun.getAllDataUsedByRun();
            if(inputDatas == null || inputDatas.length == 0)
            {
                throw new NotFoundException("No input data found for run "+expRun.getRowId());
            }
            // The first file will be the .zip file since we only use one file as input data.
            File file = expRun.getAllDataUsedByRun()[0].getFile();
            if (file == null)
            {
                throw new NotFoundException("Data file for run " + run.getFileName() + " was not found.");
            }
            if(!NetworkDrive.exists(file))
            {
                throw new NotFoundException("File " + file + " does not exist.");
            }

            PageFlowUtil.streamFile(getViewContext().getResponse(), file, true);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class DownloadDocumentForm
    {
        private int _runId;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }
    }

    // ------------------------------------------------------------------------
    // Actions to export chromatogram libraries
    // ------------------------------------------------------------------------
    public static class DownloadForm
    {
        int revision;

        public int getRevision()
        {
            return revision;
        }

        public void setRevision(int revision)
        {
            this.revision = revision;
        }
    }
    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadChromLibraryAction extends SimpleViewAction<DownloadForm>
    {
        public ModelAndView getView(DownloadForm form, BindException errors) throws Exception
        {
            // Check if the folder has any representative data
            List<Integer> representativeRunIds = TargetedMSManager.getCurrentRepresentativeRunIds(getContainer());
            if(representativeRunIds.size() == 0)
            {
                //errors.reject(ERROR_MSG, "Folder "+getContainer().getPath()+" does not contain any representative data.");
                //return new SimpleErrorView(errors, true);
                throw new NotFoundException("Folder "+getContainer().getPath()+" does not contain any representative data.");
            }

            // Get the latest library revision.
            int currentRevision = ChromatogramLibraryUtils.getCurrentRevision(getContainer());
            int libraryRevision = ( form.getRevision() != 0) ? form.getRevision() : currentRevision;

            Container container = getContainer();
            File chromLibFile = ChromatogramLibraryUtils.getChromLibFile(container, libraryRevision);

            // If the library is not found (i.e. was deleted),
            if(!chromLibFile.exists())
            {
                // create a new library file if the version numbers match
                if (libraryRevision == currentRevision)
                    ChromatogramLibraryUtils.writeLibrary(container, libraryRevision);
                else
                    throw new NotFoundException("Unable to find archived library for revision " + libraryRevision);
            }


            // construct new filename
            String fileName = getViewContext().getContainer().getName() + "_rev" + libraryRevision + ".clib";
            PageFlowUtil.streamFile(getViewContext().getResponse(), Collections.<String, String>emptyMap(), fileName, new FileInputStream(chromLibFile), true);

            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermissionClass(InsertPermission.class)
    public class IsLibraryCurrentAction extends ApiAction<LibraryDetailsForm>
    {
        public ApiResponse execute(LibraryDetailsForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            Container container = getContainer();

            // Required parameters in the request.
            if(form.getPanoramaServer() == null)
            {
                throw new ApiUsageException("Missing required parameter 'panoramaServer'");
            }
            if(form.getContainer() == null)
            {
                throw new ApiUsageException("Missing required parameter 'container'");
            }
            if(form.getSchemaVersion() == null)
            {
                throw new ApiUsageException("Missing required parameter 'schemaVersion'");
            }
            if(form.getLibraryRevision() == null)
            {
                throw new ApiUsageException("Missing required parameter 'libraryRevision'");
            }


            // Check panorama server.
            URLHelper requestParamServerUrl = new URLHelper(form.getPanoramaServer());
            URLHelper requestServerUrl = new URLHelper(getViewContext().getActionURL().getBaseServerURI());
            if(!URLHelper.queryEqual(requestParamServerUrl, requestServerUrl))
            {
                response.put("errorMessage", "Incorrect Panorama server: "+form.getPanoramaServer());
                return response;
            }

            // Check container path.
            if(!container.getPath().equals(form.getContainer()))
            {
                response.put("errorMessage", "Mismatch in container path. Expected "+container.getPath()+", found "+form.getContainer());
                return response;
            }

            // Check the schema version and library revision.
            if(!ChromatogramLibraryUtils.isRevisionCurrent(getContainer(), form.getSchemaVersion(), form.getLibraryRevision()))
            {
                response.put("isUptoDate", Boolean.FALSE);
                return response;
            }

            response.put("isUptoDate", Boolean.TRUE);
            return response;
        }
    }

    public static class LibraryDetailsForm
    {
        private String _panoramaServer;
        private String _container;
        private String _schemaVersion;
        private Integer _libraryRevision;

        public String getPanoramaServer()
        {
            return _panoramaServer;
        }

        public void setPanoramaServer(String panoramaServer)
        {
            _panoramaServer = panoramaServer;
        }

        public String getContainer()
        {
            return _container;
        }

        public void setContainer(String container)
        {
            _container = container;
        }

        public String getSchemaVersion()
        {
            return _schemaVersion;
        }

        public void setSchemaVersion(String schemaVersion)
        {
            _schemaVersion = schemaVersion;
        }

        public Integer getLibraryRevision()
        {
            return _libraryRevision;
        }

        public void setLibraryRevision(Integer libraryRevision)
        {
            _libraryRevision = libraryRevision;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ModificationSearchAction extends QueryViewAction<ModificationSearchForm, QueryView>
    {
        public ModificationSearchAction()
        {
            super(ModificationSearchForm.class);
        }

        @Override
        protected QueryView createQueryView(ModificationSearchForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            return createModificationSearchView(form, errors);
        }

        @Override
        protected ModelAndView getHtmlView(final ModificationSearchForm form, BindException errors) throws Exception
        {
            VBox result = new VBox(new ModificationSearchWebPart(form));

            if (form.isNtermSearch() || form.isCtermSearch() || form.getModificationSearchStr() != null)
                result.addView(createModificationSearchView(form, errors));

            return result;
        }

        private QueryView createModificationSearchView(final ModificationSearchForm form, BindException errors)
        {
            if (! getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(TargetedMSModule.class)))
                return null;

            ViewContext viewContext = getViewContext();
            QuerySettings settings = new QuerySettings(viewContext, "TargetedMSMatches", "Precursor");

            if (form.isIncludeSubfolders())
                settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

            QueryView result = new QueryView(new TargetedMSSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
            {
                @Override
                protected TableInfo createTable()
                {
                    TargetedMSTable result = (TargetedMSTable) super.createTable();

                    DetailsURL detailsURLs = new DetailsURL(new ActionURL(TargetedMSController.ShowPeptideAction.class, getContainer()), Collections.singletonMap("id", "PeptideId"));
                    detailsURLs.setContainerContext(new ContainerContext.FieldKeyContext(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "Folder")));
                    result.setDetailsURL(detailsURLs);

                    if (form.isNtermSearch())
                    {
                        result.addCondition(new SQLFragment("ModifiedSequence LIKE '_" + form.getDeltaMassSearchStr(true) + "%' ESCAPE '!'"));
                    }
                    else if (form.isCtermSearch())
                    {
                        result.addCondition(new SQLFragment("ModifiedSequence LIKE '%" + form.getDeltaMassSearchStr(true) + "' ESCAPE '!'"));
                    }
                    else
                    {
                        String modStr = form.getModificationSearchStr();
                        result.addCondition(new SimpleFilter(FieldKey.fromParts("ModifiedSequence"), modStr, modStr != null ? CompareType.CONTAINS_ONE_OF : CompareType.ISBLANK));
                    }

                    List<FieldKey> visibleColumns = new ArrayList<FieldKey>();
                    visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "Label"));
                    visibleColumns.add(FieldKey.fromParts("PeptideId", "Sequence"));
                    visibleColumns.add(FieldKey.fromParts("ModifiedSequence"));
                    if (form.isIncludeSubfolders())
                    {
                        visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "Folder", "Path"));
                    }
                    visibleColumns.add(FieldKey.fromParts("PeptideId", "PeptideGroupId", "RunId", "File"));
                    result.setDefaultVisibleColumns(visibleColumns);

                    return result;
                }
            };
            result.setTitle("Targeted MS Peptides");
            result.setUseQueryViewActionExportURLs(true);
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Modification Search Results");
        }
    }

    public static class ModificationSearchForm extends QueryViewAction.QueryExportForm implements HasViewContext
    {
        private ViewContext _context;
        private String _searchType;
        private String _modificationNameType;
        private Boolean _structural;
        private Boolean _isotopeLabel;
        private String _customName;
        private String _unimodName;
        private String _aminoAcids;
        private char[] _aminoAcidArr;
        private Double _deltaMass;
        private boolean _includeSubfolders;
        private String _modSearchPairsStr;

        public static ModificationSearchForm createDefault()
        {
            ModificationSearchForm result = new ModificationSearchForm();
            result.setSearchType("deltaMass");
            return result;
        }

        public String getModificationSearchStr()
        {
            String modStr = null;
            String delim = "";

            if (_modSearchPairsStr != null)
            {
                // Issue 17596: allow for a set of AA / DeltaMass pairs
                modStr = "";
                for (Pair<String, Double> entry : getModSearchPairs())
                {
                    for (char aa : splitAminoAcidString(entry.getKey()))
                    {
                        modStr += delim + aa + getDeltaMassSearchStr(entry.getValue(), false);
                        delim = ";";
                    }
                }
            }
            else if (_aminoAcidArr != null && _aminoAcidArr.length > 0)
            {
                modStr = "";
                for (char aa : _aminoAcidArr)
                {
                    modStr += delim + aa + getDeltaMassSearchStr(false);
                    delim = ";";
                }
            }

            return modStr;
        }

        public String getDeltaMassSearchStr(boolean withEscapeChar)
        {
            return getDeltaMassSearchStr(_deltaMass, withEscapeChar);
        }

        public String getDeltaMassSearchStr(Double deltaMass, boolean withEscapeChar)
        {
            // use ! as the escape character in the SQL LIKE clause with brackets (i.e. ModifiedSequence LIKE '%![+8!]' ESCAPE '!' )
            DecimalFormat df = new DecimalFormat("0.#");
            return (withEscapeChar ? "!" : "") + "[" + (deltaMass != null && deltaMass > 0 ? "+" : "") + df.format(deltaMass) + (withEscapeChar ? "!" : "") + "]";
        }

        public char[] splitAminoAcidString(String aminoAcids)
        {
            return aminoAcids.replaceAll("[^A-Za-z]","").toUpperCase().toCharArray();
        }

        public boolean isCtermSearch()
        {
            return _aminoAcids != null && _aminoAcids.equals("]");
        }

        public boolean isNtermSearch()
        {
            return _aminoAcids != null && _aminoAcids.equals("[");
        }

        public void setViewContext(ViewContext context)
        {
            _context = context;
        }

        public ViewContext getViewContext()
        {
            return _context;
        }

        public Double getDeltaMass()
        {
            return _deltaMass;
        }

        public void setDeltaMass(Double deltaMass)
        {
            _deltaMass = deltaMass;
        }

        public String getAminoAcids()
        {
            return _aminoAcids;
        }

        public void setAminoAcids(String aminoAcids)
        {
            _aminoAcids = aminoAcids;

            if (_aminoAcids != null)
                _aminoAcidArr = splitAminoAcidString(_aminoAcids);
        }

        public char[] getAminoAcidArr()
        {
            return _aminoAcidArr;
        }

        public String getSearchType()
        {
            return _searchType;
        }

        public void setSearchType(String searchType)
        {
            _searchType = searchType;
        }

        public String getModificationNameType()
        {
            return _modificationNameType;
        }

        public void setModificationNameType(String modificationNameType)
        {
            _modificationNameType = modificationNameType;
        }

        public Boolean isStructural()
        {
            return _structural;
        }

        public void setStructural(Boolean structural)
        {
            _structural = structural;
        }

        public Boolean isIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(Boolean isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }

        public String getCustomName()
        {
            return _customName;
        }

        public void setCustomName(String customName)
        {
            _customName = customName;
        }

        public String getUnimodName()
        {
            return _unimodName;
        }

        public void setUnimodName(String unimodName)
        {
            _unimodName = unimodName;
        }

        public boolean isIncludeSubfolders()
        {
            return _includeSubfolders;
        }

        public void setIncludeSubfolders(boolean includeSubfolders)
        {
            _includeSubfolders = includeSubfolders;
        }

        public List<Pair<String, Double>> getModSearchPairs()
        {
            List<Pair<String, Double>> pairs = new ArrayList<Pair<String, Double>>();
            if (_modSearchPairsStr != null)
            {
                String[] pairStrs = _modSearchPairsStr.split(";");
                for (String pairStr : pairStrs)
                {
                    String[] pair = pairStr.split(",");
                    if (pair.length == 2)
                    {
                        try {
                            pairs.add(new Pair<String, Double>(pair[0], Double.parseDouble(pair[1])));
                        }
                        catch (NumberFormatException e)
                        {
                            // skip any pairs that don't conform to the expected format
                        }
                    }
                }
            }
            return pairs;
        }

        public String getModSearchPairsStr()
        {
            return _modSearchPairsStr;
        }

        public void setModSearchPairsStr(String modSearchPairsStr)
        {
            _modSearchPairsStr = modSearchPairsStr;
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testModificationSearch() throws Exception
        {
            // test amino acid parsing and modificaation search string generation
            ModificationSearchForm form = ModificationSearchForm.createDefault();

            form.setDeltaMass(10.0);
            form.setAminoAcids("R");
            assertEquals("Unexpected number of parsed amino acids", 1, form.getAminoAcidArr().length);
            assertTrue(form.getAminoAcidArr()[0] == 'R');
            assertEquals("Unexpected modification search string", "R[+10]", form.getModificationSearchStr());

            form.setDeltaMass(8.0);
            form.setAminoAcids("RK");
            assertEquals("Unexpected number of parsed amino acids", 2, form.getAminoAcidArr().length);
            assertTrue(form.getAminoAcidArr()[0] == 'R');
            assertTrue(form.getAminoAcidArr()[1] == 'K');
            assertEquals("Unexpected modification search string", "R[+8];K[+8]", form.getModificationSearchStr());

            form.setDeltaMass(8.01);
            form.setAminoAcids("R K N");
            assertEquals("Unexpected number of parsed amino acids", 3, form.getAminoAcidArr().length);
            assertTrue(form.getAminoAcidArr()[0] == 'R');
            assertTrue(form.getAminoAcidArr()[1] == 'K');
            assertTrue(form.getAminoAcidArr()[2] == 'N');
            assertEquals("Unexpected modification search string", "R[+8];K[+8];N[+8]", form.getModificationSearchStr());

            form.setDeltaMass(-144.11);
            form.setAminoAcids("R,K;N S|T");
            assertEquals("Unexpected number of parsed amino acids", 5, form.getAminoAcidArr().length);
            assertTrue(form.getAminoAcidArr()[0] == 'R');
            assertTrue(form.getAminoAcidArr()[1] == 'K');
            assertTrue(form.getAminoAcidArr()[2] == 'N');
            assertTrue(form.getAminoAcidArr()[3] == 'S');
            assertTrue(form.getAminoAcidArr()[4] == 'T');
            assertEquals("Unexpected modification search string", "R[-144.1];K[-144.1];N[-144.1];S[-144.1];T[-144.1]", form.getModificationSearchStr());

            form.setAminoAcids("[");
            assertTrue(form.isNtermSearch());
            assertFalse(form.isCtermSearch());
            form.setAminoAcids("]");
            assertTrue(form.isCtermSearch());
            assertFalse(form.isNtermSearch());

            form.setModSearchPairsStr("GT,6;VG,5");
            assertEquals("Unexpected modification search string", "G[+6];T[+6];V[+5];G[+5]", form.getModificationSearchStr());

            form.setDeltaMass(10.0);
            assertEquals("Unexpected delta mass search string", "[+10]", form.getDeltaMassSearchStr(false));
            assertEquals("Unexpected delta mass search string", "![+10!]", form.getDeltaMassSearchStr(true));
        }
    }

    public static class FolderSetupForm
    {
        private String _folderType;
        private boolean _precursorNormalized;

        public String getFolderType()
        {
            return _folderType;
        }

        public void setFolderType(String folderType)
        {
            _folderType = folderType;
        }

        public boolean isPrecursorNormalized()
        {
            return _precursorNormalized;
        }

        public void setPrecursorNormalized(boolean precursorNormalized)
        {
            _precursorNormalized = precursorNormalized;
        }
    }

    // ------------------------------------------------------------------------
    // Actions to render a graph of library statistics
    // - viewable from the chromatogramLibraryDownload.jsp webpart
    // ------------------------------------------------------------------------
    @RequiresPermissionClass(ReadPermission.class)
    public class GraphLibraryStatisticsAction extends ExportAction
    {
        @Override
        public void export(Object o, HttpServletResponse response, BindException errors) throws Exception
        {
            int width;
            int height = 250;

            DefaultCategoryDataset dataset = getNumProteinsNumPeptidesByDate();
            width = dataset.getColumnCount() * 50 + 50;

            JFreeChart chart = ChartFactory.createBarChart(
                        null,                     // chart title
                        null,                     // domain axis label
                        "# added",                // range axis label
                        dataset,                  // data
                        PlotOrientation.VERTICAL, // orientation
                        true,                     // include legend
                        false,                     // tooltips?
                        false                     // URLs?
                    );
            chart.setBackgroundPaint(new Color(1,1,1,1));

            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
        }

        // ------------------------------------------------------------------------
        // Helper method to return representative proteins and peptides grouped by date
        // - returns a dataset for use with JFreeChart
        // ------------------------------------------------------------------------
        private DefaultCategoryDataset getNumProteinsNumPeptidesByDate() {
            final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            // determine the folder type
            final FolderType folderType = TargetedMSManager.getFolderType(getViewContext().getContainer());

            final String proteinLabel = "Proteins";
            final String peptideLabel = "Peptides";

            SQLFragment sqlFragment = new SQLFragment();
            sqlFragment.append("SELECT COALESCE(x.RunDate,y.RunDate) AS RunDate, ProteinCount, PeptideCount FROM ");
            sqlFragment.append("(SELECT pepCount.RunDate, COUNT(DISTINCT pepCount.Id) AS PeptideCount ");
            sqlFragment.append("FROM   ( SELECT ");
            sqlFragment.append("r.Created as RunDate, ");
            sqlFragment.append("p.Id ");
            sqlFragment.append("FROM ");
            sqlFragment.append("targetedms.peptide AS p, ");
            sqlFragment.append("targetedms.Runs AS r, ");
            sqlFragment.append("targetedms.PeptideGroup AS pg, ");
            sqlFragment.append("targetedms.Precursor AS pc ");
            sqlFragment.append("WHERE ");
            sqlFragment.append("p.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND pc.PeptideId = p.Id AND pc.RepresentativeDataState = 1 AND r.Deleted = ? AND r.Container = ? ");
            sqlFragment.append(") AS pepCount ");
            sqlFragment.append("GROUP BY pepCount.RunDate) AS x FULL OUTER JOIN ");
            sqlFragment.append("(SELECT protCount.RunDate, COUNT(DISTINCT protCount.Id) AS ProteinCount ");
            sqlFragment.append("FROM   ( SELECT ");
            sqlFragment.append("r.Created as RunDate, ");
            sqlFragment.append("pg.Id ");
            sqlFragment.append("FROM ");
            sqlFragment.append("targetedms.Runs AS r, ");
            sqlFragment.append("targetedms.PeptideGroup AS pg ");
            sqlFragment.append("WHERE ");
            sqlFragment.append("pg.RunId = r.Id AND pg.RepresentativeDataState = 1  AND r.Deleted = ? AND r.Container = ? ");
            sqlFragment.append(") AS protCount ");
            sqlFragment.append("GROUP BY protCount.RunDate) AS y ");
            sqlFragment.append("ON x.RunDate = y.RunDate ORDER BY COALESCE(x.RunDate,y.RunDate); ");

            sqlFragment.add(false);
            sqlFragment.add(getContainer().getId());
            sqlFragment.add(false);
            sqlFragment.add(getContainer().getId());

            // grab data from database
            SqlSelector sqlSelector = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment);

            // build HashMap of values for binning purposes
            final LinkedHashMap<Date, Integer> protMap = new LinkedHashMap<>();
            final LinkedHashMap<Date, Integer> pepMap = new LinkedHashMap<>();

            // add data to maps - binning by the date specified in simpleDateFormat
            sqlSelector.forEach(new Selector.ForEachBlock<ResultSet>() {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    Date runDate = rs.getDate("runDate");
                    int protCount = protMap.containsKey(runDate) ? protMap.get(runDate) : 0;
                    protMap.put(runDate, protCount + rs.getInt("ProteinCount"));
                    int pepCount = pepMap.containsKey(runDate) ? pepMap.get(runDate) : 0;
                    pepMap.put(runDate, pepCount + rs.getInt("PeptideCount"));
                }
            });

            LinkedHashMap<Date, Integer> binnedProtMap = binDateHashMap(protMap, 0);
            LinkedHashMap<Date, Integer> binnedPepMap = binDateHashMap(pepMap, 0);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d");

            if (protMap.size() > 10) // if more than 2 weeks, bin by week
            {
                binnedProtMap = binDateHashMap(protMap, Calendar.DAY_OF_WEEK);
                binnedPepMap = binDateHashMap(pepMap, Calendar.DAY_OF_WEEK);
            }
            if (binnedProtMap.size() > 10 )
            {
                binnedProtMap = binDateHashMap(protMap, Calendar.DAY_OF_MONTH);
                binnedPepMap = binDateHashMap(pepMap, Calendar.DAY_OF_MONTH);
                simpleDateFormat = new SimpleDateFormat("MMM yy");
            }
            // put all data from maps into dataset
            for (Map.Entry<Date, Integer> entry : binnedProtMap.entrySet())
            {
                Date key = entry.getKey();
                if (folderType == FolderType.LibraryProtein)
                    dataset.addValue(entry.getValue(), proteinLabel, simpleDateFormat.format(key));
                dataset.addValue( binnedPepMap.get(key), peptideLabel, simpleDateFormat.format(key));
            }

            return dataset;
        }
    }

    // binDateHashMap - function to bin an existing hashmap of <date, count> into different date increments
    // useful values for mode include
    //   0 - do not perform any additional binning
    //   Calendar.DAY_OF_WEEK - bin by week
    //   Calendar.DAY_OF_MONTH - bin by month
    public static LinkedHashMap<Date, Integer> binDateHashMap(LinkedHashMap<Date, Integer> hashMap, int mode )
    {
        LinkedHashMap<Date, Integer> newMap = new LinkedHashMap<>();

        // put all data from maps into dataset
        for (Map.Entry<Date, Integer> entry : hashMap.entrySet())
        {
            Date keyDate = entry.getKey();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(keyDate);
            calendar.clear(Calendar.HOUR);
            calendar.clear(Calendar.MINUTE);
            calendar.clear(Calendar.MILLISECOND);
            if ( mode != 0) // bin by week or month, passed as an argument
                calendar.set(mode, 1);
            Date newDate = calendar.getTime();

            int count = newMap.containsKey(keyDate) ? newMap.get(keyDate) : 0;
            newMap.put(newDate, count + hashMap.get(keyDate));
        }

        return newMap;
    }

    public static final long getNumRepresentativeProteins(User user, Container container) {
        long peptideGroupCount = 0;
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo peptideGroup = schema.getTable(TargetedMSSchema.TABLE_PEPTIDE_GROUP);
        if (peptideGroup != null)
        {
            SimpleFilter peptideGroupFilter = new SimpleFilter(FieldKey.fromParts("RepresentativeDataState", "Value"), "Representative", CompareType.EQUAL);
            peptideGroupCount = new TableSelector(peptideGroup, peptideGroupFilter, null).getRowCount();
        }
        return peptideGroupCount;
    }

    public static final long getNumRepresentativePeptides(Container container) {
        SQLFragment sqlFragment = new SQLFragment();
        sqlFragment.append("SELECT DISTINCT(p.Id) FROM ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptide(), "p");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoRuns(), "r");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoPeptideGroup(), "pg");
        sqlFragment.append(", ");
        sqlFragment.append(TargetedMSManager.getTableInfoPrecursor(), "pc");
        sqlFragment.append(" WHERE ");
        sqlFragment.append("p.PeptideGroupId = pg.Id AND pg.RunId = r.Id AND pc.PeptideId = p.Id AND pc.RepresentativeDataState = 1 AND r.Deleted = ? AND r.Container = ? ");

        // add variables
        sqlFragment.add(false);
        sqlFragment.add(container.getId());

        // run the query on the database and count rows
        SqlSelector sqlSelector = new SqlSelector(TargetedMSSchema.getSchema(), sqlFragment);
        long peptideCount = sqlSelector.getRowCount();

        return peptideCount;
    }

    public static final long getNumRankedTransitions(User user, Container container) {
        long transitionCount = 0;
        TargetedMSSchema schema = new TargetedMSSchema(user, container);
        TableInfo transition = schema.getTable(TargetedMSSchema.TABLE_TRANSITION);
        if (transition != null)
        {
            transitionCount = new TableSelector(transition).getRowCount();
        }
        return transitionCount;
    }

    /*
     * BEGIN RENAME CODE BLOCK
     */
    public static class RunForm extends ReturnUrlForm
    {
        public enum PARAMS
        {
            run, expanded, grouping
        }

        int run = 0;
        String columns;

        public void setRun(int run)
        {
            this.run = run;
        }

        public int getRun()
        {
            return run;
        }

        public String getColumns()
        {
            return columns;
        }

        public void setColumns(String columns)
        {
            this.columns = columns;
        }

        public ActionURL getReturnActionURL()
        {
            ActionURL result;
            try
            {
                result = super.getReturnActionURL();
                if (result != null)
                {
                    return result;
                }
            }
            catch (Exception e)
            {
                // Bad URL -- fall through
            }

            // Bad or missing returnUrl -- go to showRun or showList
            Container c = HttpView.currentContext().getContainer();

            if (0 != run)
                return getShowRunURL(c, run);
            else
                return getShowListURL(c);
        }
    }

    public static ActionURL getRenameRunURL(Container c, TargetedMSRun run, ActionURL returnURL)
    {
        ActionURL url = new ActionURL(RenameRunAction.class, c);
        url.addParameter("run", run.getRunId() );
        url.addReturnURL(returnURL);
        return url;
    }

    public static class RenameForm extends RunForm
    {
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class RenameRunAction extends FormViewAction<RenameForm>
    {
        private TargetedMSRun _run;
        private URLHelper _returnURL;

        public void validateCommand(RenameForm target, Errors errors)
        {
        }

        public ModelAndView getView(RenameForm form, boolean reshow, BindException errors) throws Exception
        {
            _run = validateRun(form.getRun());
            _returnURL = form.getReturnURLHelper(getShowRunURL(getContainer(), form.getRun()));

            String description = form.getDescription();
            if (description == null || description.length() == 0)
                description = _run.getDescription();

            RenameBean bean = new RenameBean();
            bean.run = _run;
            bean.description = description;
            bean.returnURL = _returnURL;

            getPageConfig().setFocusId("description");

            JspView<RenameBean> jview = new JspView<>("/org/labkey/targetedms/view/renameRun.jsp", bean);
            jview.setFrame(WebPartView.FrameType.PORTAL);
            jview.setTitle("Rename TargetedMS Run");
            return jview;
        }

        public boolean handlePost(RenameForm form, BindException errors) throws Exception
        {
            _run = validateRun(form.getRun());
            TargetedMSManager.renameRun(form.getRun(), form.getDescription());
            return true;
        }

        public URLHelper getSuccessURL(RenameForm form)
        {
            return form.getReturnURLHelper();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendRunNavTrail(root, _run, _returnURL, "Rename Run", getPageConfig(), null);
        }
    }


    public class RenameBean
    {
        public TargetedMSRun run;
        public String description;
        public URLHelper returnURL;
    }

    private NavTree appendRunNavTrail(NavTree root, TargetedMSRun run, URLHelper runURL, String title, PageConfig page, String helpTopic)
    {
        appendRootNavTrail(root, null, page, helpTopic);

        if (null != runURL)
            root.addChild(run.getDescription(), runURL);
        else
            root.addChild(run.getDescription());

        if (null != title)
            root.addChild(title);
        return root;
    }

    private NavTree appendRootNavTrail(NavTree root, String title, PageConfig page, String helpTopic)
    {
        page.setHelpTopic(new HelpTopic(null == helpTopic ? "targetedms" : helpTopic));
        root.addChild("TargetedMS Runs", getShowListURL(getContainer()));
        if (null != title)
            root.addChild(title);
        return root;
    }

    /*
     * END RENAME CODE BLOCK
     */
}