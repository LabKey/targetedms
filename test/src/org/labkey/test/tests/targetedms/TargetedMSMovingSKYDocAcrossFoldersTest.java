package org.labkey.test.tests.targetedms;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class TargetedMSMovingSKYDocAcrossFoldersTest extends TargetedMSTest
{
    private static final String EXPERIMENT_FOLDER = "Panorama Experiment folder";
    private static final String MAM_FOLDER = "Panorama MAM folder";
    private static final String IMPORT_SKY_FILE = "SampleIdTest.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSMovingSKYDocAcrossFoldersTest init = (TargetedMSMovingSKYDocAcrossFoldersTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        log("Creating one Experiment folder");
        setUpFolder(EXPERIMENT_FOLDER, FolderType.Experiment);

        log("Creating one MAM folder");
        setUpFolder(MAM_FOLDER, FolderType.ExperimentMAM);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(EXPERIMENT_FOLDER, afterTest);
        apiContainerHelper.deleteProject(MAM_FOLDER, afterTest);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Test
    public void testMovingSkyDocAcrossFolders()
    {
        log("Uploading skyline document in experiment folder");
        goToProjectHome(EXPERIMENT_FOLDER);
        importData(IMPORT_SKY_FILE);

        log("Performing move from " + EXPERIMENT_FOLDER + " to " + MAM_FOLDER);
        goToProjectHome(EXPERIMENT_FOLDER);
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).withName("TargetedMSRuns").waitFor();
        table.checkCheckbox(0);
        table.clickHeaderButton("Move");
        waitAndClickAndWait(Locator.linkWithText(MAM_FOLDER));
        waitForPipelineJobsToComplete(1, false);

        log("Verifying document is moved from source folder");
        goToProjectHome(EXPERIMENT_FOLDER);
        table = new DataRegionTable.DataRegionFinder(getDriver()).withName("TargetedMSRuns").waitFor();
        Assert.assertEquals("Skyline document should have been moved from " + EXPERIMENT_FOLDER, 0, table.getDataRowCount());

        log("Verifying document is moved to target folder");
        goToProjectHome(MAM_FOLDER);
        table = new DataRegionTable.DataRegionFinder(getDriver()).withName("TargetedMSRuns").waitFor();
        Assert.assertEquals("Skyline document should have been moved to " + MAM_FOLDER, 1, table.getDataRowCount());
        Assert.assertEquals("Incorrect skyline document moved", IMPORT_SKY_FILE, table.getDataAsText(0, "File"));
    }
}
