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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.data.Container;
import org.labkey.api.data.EnumConverter;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.module.SpringModule;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.query.QueryView;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.JspView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.targetedms.parser.RepresentativeDataState;
import org.labkey.targetedms.pipeline.TargetedMSPipelineProvider;
import org.labkey.targetedms.search.ModificationSearchWebPart;
import org.labkey.targetedms.view.LibraryPrecursorViewWebPart;
import org.labkey.targetedms.view.PeptideGroupViewWebPart;
import org.labkey.targetedms.view.PeptideViewWebPart;
import org.labkey.targetedms.view.TransitionPeptideSearchViewProvider;
import org.labkey.targetedms.view.TransitionProteinSearchViewProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TargetedMSModule extends SpringModule implements ProteomicsModule
{
    public static final String NAME = "TargetedMS";

    // Protocol prefix for importing .sky documents from Skyline
    public static final String IMPORT_SKYDOC_PROTOCOL_OBJECT_PREFIX = "TargetedMS.ImportSky";
    // Protocol prefix for importing .zip archives from Skyline
    public static final String IMPORT_SKYZIP_PROTOCOL_OBJECT_PREFIX = "TargetedMS.ImportSkyZip";

    public static final ExperimentRunType EXP_RUN_TYPE = new TargetedMSExperimentRunType();
    public static final String TARGETED_MS_SETUP = "Targeted MS Setup";
    public static final String TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD = "Chromatogram Library Download";
    public static final String TARGETED_MS_PRECURSOR_VIEW = "Targeted MS Precursor View";
    public static final String TARGETED_MS_PEPTIDE_VIEW = "Targeted MS Peptide View";
    public static final String TARGETED_MS_PEPTIDE_GROUP_VIEW = "Targeted MS Protein View";
    public static final String TARGETED_MS_RUNS_WEBPART_NAME = "Targeted MS Runs";
    public static final String TARGETED_MS_PROTEIN_SEARCH = "Targeted MS Protein Search";

    public static final String TARGETED_MS_FOLDER_TYPE = "TargetedMS Folder Type";
    public static ModuleProperty FOLDER_TYPE_PROPERTY;

    public enum FolderType
    {
        Experiment, Library, LibraryProtein, Undefined
    }

    public TargetedMSModule()
    {
        FOLDER_TYPE_PROPERTY = new ModuleProperty(this, TARGETED_MS_FOLDER_TYPE);
        // Set up the TargetedMS Folder Type property
        FOLDER_TYPE_PROPERTY.setDefaultValue(FolderType.Undefined.toString());
        FOLDER_TYPE_PROPERTY.setCanSetPerContainer(true);
        addModuleProperty(FOLDER_TYPE_PROPERTY);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public double getVersion()
    {
        return 13.21;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory setupFactory = new BaseWebPartFactory(TARGETED_MS_SETUP)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                JspView view = new JspView("/org/labkey/targetedms/view/folderSetup.jsp");
                view.setTitle("Configure Targeted MS Folder");
                return view;
            }
        };

        BaseWebPartFactory chromatogramLibraryDownload = new BaseWebPartFactory(TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                JspView view = new JspView("/org/labkey/targetedms/view/chromatogramLibraryDownload.jsp");
                view.setTitle(TARGETED_MS_CHROMATOGRAM_LIBRARY_DOWNLOAD);
                return view;
            }
        };

        BaseWebPartFactory precursorView = new BaseWebPartFactory(TARGETED_MS_PRECURSOR_VIEW)
        {
            @Override
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws Exception
            {
               return new LibraryPrecursorViewWebPart(portalCtx);
            }
        };

        BaseWebPartFactory peptideView  = new BaseWebPartFactory(TARGETED_MS_PEPTIDE_VIEW)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                QueryView view = new PeptideViewWebPart(portalCtx);
                return view;
            }
        };

        BaseWebPartFactory peptideGroupView  = new BaseWebPartFactory(TARGETED_MS_PEPTIDE_GROUP_VIEW)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                QueryView view = new PeptideGroupViewWebPart(portalCtx);
                return view;
            }
        };

        BaseWebPartFactory runsFactory = new BaseWebPartFactory(TARGETED_MS_RUNS_WEBPART_NAME)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                QueryView gridView = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), EXP_RUN_TYPE);
                gridView.setFrame(WebPartView.FrameType.NONE);
                VBox vbox = new VBox();
                vbox.addView(new JspView("/org/labkey/targetedms/view/conflictSummary.jsp"));
                vbox.addView(gridView);
                vbox.setFrame(WebPartView.FrameType.PORTAL);
                vbox.setTitle(TargetedMSModule.TARGETED_MS_RUNS_WEBPART_NAME);
                vbox.setTitleHref(new ActionURL(TargetedMSController.ShowListAction.class, portalCtx.getContainer()));
                return vbox;
            }
        };

        BaseWebPartFactory proteinSearchFactory = new BaseWebPartFactory(TARGETED_MS_PROTEIN_SEARCH)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                JspView view = new JspView("/org/labkey/targetedms/view/proteinSearch.jsp");
                view.setTitle("Protein Search");
                return view;
            }
        };

        BaseWebPartFactory modificationSearchFactory = new BaseWebPartFactory(ModificationSearchWebPart.NAME)
        {
            public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
            {
                return new ModificationSearchWebPart(TargetedMSController.ModificationSearchForm.createDefault());
            }
        };

        List<WebPartFactory> webpartFactoryList = new ArrayList<>();
        webpartFactoryList.add(setupFactory);
        webpartFactoryList.add(chromatogramLibraryDownload);
        webpartFactoryList.add(precursorView);
        webpartFactoryList.add(peptideView);
        webpartFactoryList.add(peptideGroupView);
        webpartFactoryList.add(runsFactory);
        webpartFactoryList.add(proteinSearchFactory);
        webpartFactoryList.add(modificationSearchFactory);
        return webpartFactoryList;
    }

    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(TargetedMSManager.get().getSchemaName());
    }

    @Override
    protected void init()
    {
        addController("targetedms", TargetedMSController.class);
        TargetedMSSchema.register();
        EnumConverter.registerEnum(TargetedMSRun.RepresentativeDataState.class);
        EnumConverter.registerEnum(RepresentativeDataState.class);
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new TargetedMSPipelineProvider(this));

        ExperimentService.get().registerExperimentDataHandler(new TargetedMSDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new SkylineBinaryDataHandler());

        ExperimentService.get().registerExperimentRunTypeSource(new ExperimentRunTypeSource()
        {
            public Set<ExperimentRunType> getExperimentRunTypes(Container container)
            {
                if (container.getActiveModules().contains(TargetedMSModule.this))
                {
                    return Collections.singleton(EXP_RUN_TYPE);
                }
                return Collections.emptySet();
            }
        });

        //register the Targeted MS folder type
        ModuleLoader.getInstance().registerFolderType(this, new TargetedMSFolderType(this));

        ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
        proteinService.registerProteinSearchView(new TransitionProteinSearchViewProvider());
        proteinService.registerPeptideSearchView(new TransitionPeptideSearchViewProvider());

        AuditLogService.get().addAuditViewFactory(TargetedMsRepresentativeStateAuditViewFactory.getInstance());
        AuditLogService.registerAuditType(new TargetedMsRepresentativeStateAuditProvider());
    }

    @NotNull
    @Override
    public Set<Class> getUnitTests()
    {
        return PageFlowUtil.<Class>set(TargetedMSController.TestCase.class);
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new TargetedMSUpgradeCode();
    }
}
