package org.labkey.test.tests.panoramapremium;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;
import org.labkey.test.tests.targetedms.TargetedMSTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSHidePeptidesAndMolecules extends TargetedMSTest
{
    protected static final String PeptidesOnlySkyFile = "QC_1.sky.zip";
    protected static final String PeptidesOnlySkyFile_2 = "QC_2.sky.zip";
    protected static final String MoleculesOnlySkyFile = "SmMolLibA.sky.zip";
    protected static final String PeptidesAndMoleculesSkyFile = "smallmol_plus_peptides.sky.zip";

    private static final String PeptidesOnlySubfolder = "Peptides Only";
    private static final String MoleculesOnlySubFolder = "Molecules Only";
    private static final String PeptidesAndMoleculesSubfolder = "Peptides and Molecules";
    private static final String EmptyProject = "Empty QC Folder";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSHidePeptidesAndMolecules init = (TargetedMSHidePeptidesAndMolecules) getCurrentTest();
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

        setupSubfolder(getProjectName(), PeptidesOnlySubfolder, FolderType.QC);
        importData(PeptidesOnlySkyFile);

        setupSubfolder(getProjectName(), MoleculesOnlySubFolder, FolderType.QC);
        importData(MoleculesOnlySkyFile);

        setupSubfolder(getProjectName(), PeptidesAndMoleculesSubfolder, FolderType.QC);
        importData(PeptidesAndMoleculesSkyFile);
    }

    @Test
    public void testPeptideAndMoleculeExclusionAndInclusion()
    {
        log("Verifying the QC summary menu item");
        verifyQCSummaryMenuItem(PeptidesOnlySubfolder);
        verifyQCSummaryMenuItem(MoleculesOnlySubFolder);
        verifyQCSummaryMenuItem(PeptidesAndMoleculesSubfolder);

        log("Verifying exclusion");
        excludePeptideOrMolecule(PeptidesOnlySubfolder, 0, "Excluded");
        excludePeptideOrMolecule(MoleculesOnlySubFolder, 0, "Excluded");

        log("Verifying QC plots");
        navigateToFolder(getProjectName(), PeptidesOnlySubfolder);
        PanoramaDashboard qcDashboard = goToDashboard();
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        checker().verifyEquals("Incorrect number of plots displayed when excluded ", 1, qcPlotsWebPart.getPlots().size());

        qcPlotsWebPart.setShowExcludedPrecursors(true);
        qcPlotsWebPart.waitForPlots(2);
        checker().verifyEquals("Incorrect number of plots after show excluded precursor is checked ", 2, qcPlotsWebPart.getPlots().size());

        log("Verifying Inclusion");
        excludePeptideOrMolecule(PeptidesOnlySubfolder, 0, "Included");
        excludePeptideOrMolecule(MoleculesOnlySubFolder, 0, "Included");
    }

    /*
        Test coverage for : Issue 46048: Configure Included and Excluded Precursors - blank or duplicate precursors in some scenarios
     */
    @Test
    public void testMultipleSkyFilesImport()
    {
        setupSubfolder(getProjectName(), EmptyProject, FolderType.QC);
        new PortalHelper(this).clickWebpartMenuItem("Replicate Summary", true, "Configure Included and Excluded Precursors");
        checker().verifyTrue("Incorrect error message before importing SKY file",
                isTextPresent("No data loaded in this folder. Import a Skyline document into this folder to configure exclusions."));
        checker().screenShotIfNewError("EmptyProjectErrorMessage");

        navigateToFolder(getProjectName(), PeptidesOnlySubfolder);
        DataRegionTable table = gotoIncludeExcludeMenu();
        checker().verifyEquals("Incorrect peptides with single import file", Arrays.asList("AGGSSEPVTGLADK", "VEATFGVDESANK"), table.getColumnDataAsText("modifiedSequence"));

        log("Importing the second file folder");
        navigateToFolder(getProjectName(), PeptidesOnlySubfolder);
        importData(PeptidesOnlySkyFile_2, 2);
        table = gotoIncludeExcludeMenu();
        checker().verifyEquals("Incorrect peptides when two sky files are imported", Arrays.asList("AGGSSEPVTGLADK", "VEATFGVDESANK"), table.getColumnDataAsText("modifiedSequence"));

        log("Deleting the extra imported SKY file");
        clickTab("Runs");
        DataRegionTable runsTable = new DataRegionTable("TargetedMSRuns", getDriver());
        runsTable.checkCheckbox(0);
        runsTable.clickHeaderButton("Delete");
        clickButton("Confirm Delete");
    }

    private void verifyQCSummaryMenuItem(String folderName)
    {
        navigateToFolder(getProjectName(), folderName);

        log("Verifying Marked As column is defaulted to Included");
        DataRegionTable table = gotoIncludeExcludeMenu();
        table.setUpFilter("markedAs", "Does Not Equal", "Included");
        table.doAndWaitForUpdate(() -> table.getWrapper().clickButton("OK", 0));
        checker().verifyEquals("Should be marked as Included by default " + folderName, 0, table.getCheckedCount());
    }

    private void excludePeptideOrMolecule(String folderName, int rowIndex, String action)
    {
        navigateToFolder(getProjectName(), folderName);
        DataRegionTable table = gotoIncludeExcludeMenu();
        table.checkCheckbox(rowIndex);
        if (action.equals("Excluded"))
            table.doAndWaitForUpdate(() -> table.clickHeaderButton("Mark As Excluded"));
        else
            table.doAndWaitForUpdate(() -> table.clickHeaderButton("Mark As Included"));
        checker().withScreenshot("MarkedAsColumnUpdate_" + folderName).verifyEquals("Marked As column not updated with selection of action", action, table.getDataAsText(rowIndex, "markedAs"));

        log("Verifying targetedms.ExcludedPrecursors table gets updated with action");
        goToSchemaBrowser();
        DataRegionTable queryData = viewQueryData("targetedms", "ExcludedPrecursors");
        if (action.equals("Excluded"))
            checker().verifyEquals("Excluded element not present in targetedms.ExcludedPrecursors table", 1, queryData.getDataRowCount());
        else
            checker().verifyEquals("Excluded element not present in targetedms.ExcludedPrecursors table", 0, queryData.getDataRowCount());
    }

    private DataRegionTable gotoIncludeExcludeMenu()
    {
        PanoramaDashboard qcDashboard = goToDashboard();
        QCSummaryWebPart qcSummaryWebPart = qcDashboard.getQcSummaryWebPart();
        qcSummaryWebPart.clickMenuItem("Configure Included and Excluded Precursors");
        return new DataRegionTable.DataRegionFinder(getDriver()).waitFor();
    }
}
