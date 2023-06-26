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
package org.labkey.test.tests.targetedms.passport;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.nio.file.Paths;


public abstract class PassportTestPart extends BaseWebDriverTest
{
    protected static final String NORMAL_USER = "normaluser@passport.test";

    @Override
    protected String getProjectName()
    {
        return "PassportTest";
    }

    @LogMethod
    protected void setupProject()
    {
        _containerHelper.createProject(getProjectName(), "Collaboration");
        ApiPermissionsHelper h = new ApiPermissionsHelper(this);
        h.addMemberToRole(NORMAL_USER, "Reader", PermissionsHelper.MemberType.user);

        goToFolderManagement().
                goToFolderTypeTab().
                enableModule("Pipeline").
                enableModule("Query").
                enableModule("TargetedMS").
                save();

        goToProjectHome();
        PortalHelper ph = new PortalHelper(this);
        ph.enterAdminMode();
        ph.removeWebPart("Messages");
        ph.removeWebPart("Wiki");
        ph.removeWebPart("Pages");
        ph.addWebPart("Passport");
        ph.addTab("Pipeline");
        ph.addWebPart("Pipeline Files");
        ph.renameTab("Start Page", "Passport");
        importData("data1.sky.zip", 1, true);
        importData("data2.sky.zip", 2, true);
        ph.hideTab("Pipeline");
        ph.exitAdminMode();
    }

    @LogMethod
    protected void importData(@LoggedParam String file, int jobCount, boolean failOnError)
    {
        Locator.XPathLocator importButtonLoc = Locator.lkButton("Process and Import Data");
        WebElement importButton = importButtonLoc.findElementOrNull(getDriver());
        if (null == importButton)
        {
            goToModule("Pipeline");
            importButton = importButtonLoc.findElement(getDriver());
        }
        clickAndWait(importButton);
        _fileBrowserHelper.waitForFileGridReady();
        String fileName = Paths.get(file).getFileName().toString();
        if (!isElementPresent(FileBrowserHelper.Locators.gridRow(fileName)))
            _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData("TargetedMS/Passport/" + file));
        _fileBrowserHelper.importFile(fileName, "Import Skyline Results");
        waitForText("Skyline document import");
        if (failOnError)
            waitForPipelineJobsToComplete(jobCount, file, false);
        else
            waitForPipelineJobsToFinish(jobCount);
    }

    public void deleteProject(String project, boolean failIfFail) throws TestTimeoutException
    {
        _containerHelper.deleteProject(project, failIfFail, 120000); // Wait 2 minutes for project deletion
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, NORMAL_USER);
    }

}
