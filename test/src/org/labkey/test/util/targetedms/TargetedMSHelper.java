package org.labkey.test.util.targetedms;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.tests.targetedms.TargetedMSTest.FolderType;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebElement;

import java.nio.file.Paths;

/** Setup and import utilities to share between standard and upgrade tests for TargetedMS */
public class TargetedMSHelper
{
    private final BaseWebDriverTest _test;

    public TargetedMSHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void setupFolder(String projectName, FolderType folderType)
    {
        _test._containerHelper.createProject(projectName, "Panorama");
        _test.waitForElement(Locator.linkContainingText("Save"));
        _test.clickAndWait(Locator.linkContainingText("Next"));
        selectFolderType(folderType);
    }

    @LogMethod
    public void selectFolderType(@LoggedParam FolderType folderType)
    {
        _test.log("Select Folder Type: " + folderType);
        folderType.chooseFolderType(_test);
        _test.clickButton("Finish");
    }

    public void importData(String file)
    {
        importData(file, 1);
    }

    @LogMethod
    public void importData(@LoggedParam String file, int jobCount)
    {
        importData(file, jobCount, false);
    }

    @LogMethod
    public void importData(@LoggedParam String file, int jobCount, boolean expectError)
    {
        Locator.XPathLocator importButtonLoc = Locator.lkButton("Process and Import Data");
        WebElement importButton = importButtonLoc.findElementOrNull(_test.getDriver());
        if (null == importButton)
        {
            _test.goToModule("Pipeline");
            importButton = importButtonLoc.findElement(_test.getDriver());
        }
        _test.clickAndWait(importButton);
        String fileName = Paths.get(file).getFileName().toString();
        if (!_test._fileBrowserHelper.fileIsPresent(fileName))
            _test._fileBrowserHelper.uploadFile(TestFileUtils.getSampleData("TargetedMS/" + file));
        _test._fileBrowserHelper.importFile(fileName, "Import Skyline Results");
        _test.waitForText("Skyline document import");
        _test.waitForPipelineJobsToComplete(jobCount, file, expectError);
    }
}
