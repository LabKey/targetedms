package org.labkey.test.tests.panoramapremium;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSSampleManagerIntegrationTest extends TargetedMSPremiumTest
{
    protected static final String TargetedMS_SubFolder = "TargetedMS Subfolder";
    protected static final String Sample_Manager_Subfolder = "Sample Manager Subfolder";
    private static final String sampleType = "TargetedMS_Linked_Sample_Type";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSSampleManagerIntegrationTest init = (TargetedMSSampleManagerIntegrationTest) getCurrentTest();
        init.doInit();
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        _containerHelper.enableModules(Arrays.asList("SampleManagement"));

        setupSubfolder(getProjectName(), TargetedMS_SubFolder, FolderType.QC);
        importData(SProCoP_FILE_ANNOTATED);

        goToProjectHome();
        new APIContainerHelper(this).createSubfolder(getProjectName(), Sample_Manager_Subfolder);
        new PortalHelper(getDriver()).addBodyWebPart("Sample Types");

        log("Creating Samples in SM subfolder");
        createSampleType(sampleType);
    }

    @Test
    public void testSampleTypeNavigation()
    {
        String s1 = "AnnotatedSample1";
        String s2 = "ExtractedSampleId4";
        String s3 = "Q_Exactive_08_09_2013_JGB_87";

        log("Verifying links does not navigate to SM application");
        navigateToFolder(getProjectName(), TargetedMS_SubFolder);
        waitAndClickAndWait(Locator.linkContainingText("replicates"));
        assertTextPresent(s1, s2, s3);
        assertElementNotPresent(Locator.linkWithText(s1));
        assertElementNotPresent(Locator.linkWithText(s2));

        log("Adding samples");
        List<Map<String, String>> samples = Arrays.asList(Map.of("Name", s1),
                Map.of("Name", s2), Map.of("Name", s3));
        addSamples(sampleType, samples);

        goToProjectHome();
        navigateToFolder(getProjectName(), TargetedMS_SubFolder);
        clickTab("Runs");
        waitAndClickAndWait(Locator.linkWithText(SProCoP_FILE_ANNOTATED));
        clickAndWait(Locator.linkWithText("6 replicates"));

        log("Clicking the replicate column link to verify the navigation");
        clickAndWait(Locator.linkWithText("Q_Exactive_08_09_2013_JGB_02").index(0));
        checker().verifyTrue("Sample link did not navigate to sample manager application",
                getCurrentRelativeURL(false).contains(WebTestHelper.buildRelativeUrl("targetedms", getProjectName() + "/" + TargetedMS_SubFolder, "showSampleFile")));
        goBack();

        log("Navigating to SM app");
        assertElementPresent(Locator.linkWithText(s2));
        assertElementPresent(Locator.linkWithText(s3));
        clickAndWait(Locator.linkWithText(s1));
        checker().verifyTrue("Sample link did not navigate to sample manager application",
                getCurrentRelativeURL(false).contains(WebTestHelper.buildRelativeUrl("sampleManager", getProjectName() + "/" + Sample_Manager_Subfolder, "app")));

        log("Navigating back to labkey server");
        waitForElementToBeVisible(Locator.linkWithText("Assays"));
        click(Locator.linkWithText("Assays"));
        waitAndClick(Locator.linkContainingText("Skyline Documents"));
        waitAndClickAndWait(Locator.linkWithText(SProCoP_FILE_ANNOTATED));
        checker().verifyTrue("Did not navigate back to labkey server",
                getCurrentRelativeURL(false).contains(WebTestHelper.buildRelativeUrl("targetedms", getProjectName() + "/" + TargetedMS_SubFolder, "showPrecursorList")));

        log("Disabling the SM to verify the navigation");
        goToProjectHome();
        navigateToFolder(getProjectName(), Sample_Manager_Subfolder);
        _containerHelper.disableModules("SampleManagement");

        navigateToFolder(getProjectName(), TargetedMS_SubFolder);
        waitAndClickAndWait(Locator.linkContainingText("replicates"));
        clickAndWait(Locator.linkWithText(s1));
        checker().verifyTrue("Sample link navigated to sample manager application when disabled at folder level",
                getCurrentRelativeURL(false).contains(WebTestHelper.buildRelativeUrl("experiment", getProjectName() + "/" + Sample_Manager_Subfolder, "showMaterial")));

    }

    private void createSampleType(String sampleName)
    {
        projectMenu().navigateToFolder(getProjectName(), Sample_Manager_Subfolder);
        SampleTypeAPIHelper.createEmptySampleType(getProjectName(), new SampleTypeDefinition(sampleName));
    }

    private void addSamples(String sampleName, List<Map<String, String>> samples)
    {
        projectMenu().navigateToFolder(getProjectName(), Sample_Manager_Subfolder);
        clickAndWait(Locator.linkWithText(sampleName));
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);
        for (Map<String, String> sample : samples)
            sampleTypeHelper.insertRow(sample);
    }
}
