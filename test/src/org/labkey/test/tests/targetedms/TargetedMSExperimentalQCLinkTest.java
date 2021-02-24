package org.labkey.test.tests.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.targetedms.GuideSet;
import org.labkey.test.components.targetedms.GuideSetWebPart;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.GuideSetPage;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.util.DataRegionTable;

public class TargetedMSExperimentalQCLinkTest extends TargetedMSTest
{
    private static final String SKY_FILE_EXPERIMENT = "SProCoPTutorial-ExperimentalFolderData.zip";
    private static final String SKY_FILE_QC = "SProCoPTutorial-QCFolderData.zip";
    private static final String QC_FOLDER_1 = "Test Project QC Folder 1";
    private static final String QC_FOLDER_2 = "Test Project QC Folder 2";


    @BeforeClass
    public static void initProject()
    {
        TargetedMSExperimentalQCLinkTest init = (TargetedMSExperimentalQCLinkTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE_EXPERIMENT);

        log("Creating one test QC folder with same data");
        setUpFolder(QC_FOLDER_1, FolderType.QC);
        importData(SKY_FILE_QC);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(QC_FOLDER_1, afterTest);
        _containerHelper.deleteProject(QC_FOLDER_2, afterTest);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "TargetedMS Experimental QC Link Test";
    }

    @Test
    public void testInstrumentSummaryPage()
    {
        goToProjectHome();
        DataRegionTable table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));

        checker().verifyTrue("Instruments Summary webpart is missing",
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Instruments Summary")));
        table = new DataRegionTable("InstrumentSummary", getDriver());
        checker().verifyEquals("Invalid QC Folder Name ", QC_FOLDER_1,
                table.getDataAsText(0, "QCFolders"));

        setUpFolder(QC_FOLDER_2, FolderType.QC);
        importData(SKY_FILE_QC);
        goToProjectHome();
        table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));

        checker().verifyTrue("Instruments Summary webpart is missing",
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Instruments Summary")));
        table = new DataRegionTable("InstrumentSummary", getDriver());
        checker().verifyEquals("Invalid Instrument serial number", "Exactive Series slot #2384",
                table.getDataAsText(0, "SerialNumber"));
        checker().verifyEquals("Invalid QC Folder Name ", QC_FOLDER_1 + "\n" + QC_FOLDER_2,
                table.getDataAsText(0, "QCFolders"));

        clickAndWait(Locator.linkWithText(QC_FOLDER_1));
        checker().verifyEquals("Did not navigate to QC folder ", QC_FOLDER_1, getCurrentContainer());
        goBack();

        clickAndWait(Locator.linkWithText(QC_FOLDER_2));
        checker().verifyEquals("Did not navigate to QC folder ", QC_FOLDER_2, getCurrentContainer());

    }

    @Test
    public void testLinkExperimentalQC()
    {
        goToProjectHome(QC_FOLDER_1);
        createGuideSetFromTable(new GuideSet("2013/08/03", "2013/08/09", null));
        createGuideSetFromTable(new GuideSet("2013/08/19", "2013/08/21", null));

        goToProjectHome();
        DataRegionTable table = new DataRegionTable("TargetedMSRuns", getDriver());
        clickAndWait(table.link(0, "Replicates"));

        clickAndWait(Locator.linkWithText(QC_FOLDER_1));
        checker().verifyTrue("Experiment date range toolbar is not present",
                isElementPresent(Locator.linkContainingText(SKY_FILE_EXPERIMENT)));
        clickAndWait(Locator.linkContainingText(SKY_FILE_EXPERIMENT));
        checker().verifyEquals("Did not navigate to experimental folder", getProjectName(),getCurrentContainer());
        goBack();

        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        String testStartDate = "2013-08-19";
        String testEndDate = "2013-08-27";
        qcPlotsWebPart.filterQCPlots(testStartDate, testEndDate, 7);

    }

    protected void createGuideSetFromTable(GuideSet guideSet)
    {
        if (!"Guide Sets".equals(getUrlParam("pageId", true)))
            clickTab("Guide Sets");

        GuideSetWebPart guideSetWebPart = new GuideSetWebPart(this, getProjectName());
        GuideSetPage guideSetPage = guideSetWebPart.startInsert();
        guideSetPage.insert(guideSet, null);
    }

}
