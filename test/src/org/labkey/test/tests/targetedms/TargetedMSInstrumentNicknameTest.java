/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.tests.targetedms;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({})
public class TargetedMSInstrumentNicknameTest extends TargetedMSTest
{
    private static final String QC_SUB_FOLDER = "QC Subfolder";
    private static final String QC_SUB_SUB_FOLDER = "QC SubSubfolder";
    private static final String NON_QC_SUB_FOLDER = "NonQC Subfolder 3";
    public static final String Q_EXACTIVE = "Q Exactive";
    public static final String Q_EXACTIVE_SERIAL_ONLY = "Exactive Series slot #2384";
    public static final String Q_EXACTIVE_WITH_SERIAL = Q_EXACTIVE + " - " + Q_EXACTIVE_SERIAL_ONLY;
    public static final String QTRAP = "4000 QTRAP - U02630409";
    public static final String AUTOMATED_TEST_NICKNAME_PREFIX = "AutomatedTestNickname";
    public static final String NICKNAME_1 = AUTOMATED_TEST_NICKNAME_PREFIX + "1" + TRICKY_CHARACTERS;
    public static final String NICKNAME_2 = AUTOMATED_TEST_NICKNAME_PREFIX + "2" + TRICKY_CHARACTERS;
    public static final String NICKNAME_3 = AUTOMATED_TEST_NICKNAME_PREFIX + "3" + TRICKY_CHARACTERS;
    public static final String REPLICATE_NAME_WITHOUT_SERIAL = "QEHF_7x5_TRAP_PRM_30minG_ES800_200fmolOC_25Oct19_R2";
    public static final String FILE_PATH_WITHOUT_SERIAL = "C:\\Xcalibur\\data\\Bhavin\\2018\\October\\QEHF_QC_24Oct2018\\QEHF_7x5_TRAP_PRM_30minG_ES800_TRAP_200fmolOC_25Oct2018_R2.raw";
    public static final String REPLICATE_NAME_WITH_SERIAL = "QEHF_7x5_TRAP_PRM_30minG_ES800_200fmolOC_21Apr18_R2";
    public static final String FILE_PATH_WITH_SERIAL = "Z:\\QEHF_RawData\\Bhavin\\2018\\April\\SystemSuitability_7x5_Validation_16Apr2018\\QEHF_SysSuitStd_35AQUA_7x5_TRAP_PRM_30minG_ES800_400fmolOC_3axAdj17A_21Apr18_R2.raw";

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @BeforeClass
    public static void initProject()
    {
        TargetedMSInstrumentNicknameTest init = getCurrentTest();
        init.setupProjectWithSubfolders();
        init.importInitialData();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        // Clean out the nicknames set in /Shared
        var command = new SelectRowsCommand("targetedms", "InstrumentNickname");
        command.setFilters(List.of(new Filter("Nickname", AUTOMATED_TEST_NICKNAME_PREFIX, Filter.Operator.STARTS_WITH)));
        try
        {
            Connection connection = createDefaultConnection();
            var response = command.execute(connection, "/Shared");
            if (!response.getRows().isEmpty())
            {
                var deleteCommand = new DeleteRowsCommand("targetedms", "InstrumentNickname");
                deleteCommand.setRows(response.getRows());
                deleteCommand.execute(connection, "/Shared");
            }
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void setupProjectWithSubfolders()
    {
        setupFolder(FolderType.QC);

        setupSubfolder(getProjectName(), QC_SUB_FOLDER, FolderType.QC);
        setupSubfolder(getProjectName(), NON_QC_SUB_FOLDER, FolderType.Experiment);

        clickFolder(QC_SUB_FOLDER);
        setupSubfolder(getProjectName(), QC_SUB_FOLDER, QC_SUB_SUB_FOLDER, FolderType.QC);

        _userHelper.createUser(USER);

        // give user reader permissions to all but FOLDER_1
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.addMemberToRole(USER, "Reader", PermissionsHelper.MemberType.user, getProjectName());
        permissionsHelper.addMemberToRole(USER, "Editor", PermissionsHelper.MemberType.user, getProjectName() + "/" + NON_QC_SUB_FOLDER);
    }

    private void importInitialData()
    {
        goToProjectHome();
        importData(ISOTOPOLOGUE_FILE_ANNOTATED, 1, false, false);

        // Import the same file into a subfolders to test scoping
        clickFolder(QC_SUB_FOLDER);
        importData(ISOTOPOLOGUE_FILE_ANNOTATED, 1, false, false);

        clickFolder(QC_SUB_SUB_FOLDER);
        importData(ISOTOPOLOGUE_FILE_ANNOTATED, 1, false, false);

        clickFolder(NON_QC_SUB_FOLDER);
        importData(ISOTOPOLOGUE_FILE_ANNOTATED, 1, false, false);
        // Only do DB maintenance on the last import in the sequence
        importData(SAMPLE_FILE_CHROM_INFO, 2, false, true);
    }

    @Test
    public void testSubfolders()
    {
        goToProjectHome();
        Locator qExactiveLinkLocator = Locator.linkWithText(Q_EXACTIVE);
        Locator qExactiveWithSerialLinkLocator = Locator.linkWithText(Q_EXACTIVE_WITH_SERIAL);
        Locator nickname1LinkLocator = Locator.linkWithText(NICKNAME_1);
        Locator nickname2LinkLocator = Locator.linkWithText(NICKNAME_2);

        // Default display should show both variants of the model/serial number
        waitForElement(qExactiveLinkLocator);
        assertElementPresent(qExactiveWithSerialLinkLocator);

        // Give a nickname that collapses them, saving in the default scope (server-side)
        clickAndWait(qExactiveLinkLocator);
        setFormElement(Locator.input("name"), NICKNAME_1);
        clickButton("Save");
        goToProjectHome();
        waitAndClickAndWait(qExactiveWithSerialLinkLocator);
        setFormElement(Locator.input("name"), NICKNAME_1);
        clickButton("Save");

        // Be sure that the new nickname is shown and the model/serial number aren't
        waitForElement(nickname1LinkLocator);
        assertElementNotPresent(qExactiveLinkLocator);
        assertElementNotPresent(qExactiveWithSerialLinkLocator);

        clickFolder(QC_SUB_FOLDER);
        waitForElement(nickname1LinkLocator);
        assertElementNotPresent(qExactiveLinkLocator);
        assertElementNotPresent(qExactiveWithSerialLinkLocator);

        clickAndWait(nickname1LinkLocator);
        // We should see both model/serial numbers on the same page
        assertTextPresent(Q_EXACTIVE, 4);  // Twice on the page itself, twice in hidden form elements
        assertTextPresent(Q_EXACTIVE_SERIAL_ONLY, 2); // Once on the page itself, once in hidden form elements
        assertTextPresent(
                getProjectName() + "/" + QC_SUB_FOLDER + "/" + QC_SUB_SUB_FOLDER,
                getProjectName() + "/" + NON_QC_SUB_FOLDER,

                // Check for a sample without a serial number
                REPLICATE_NAME_WITHOUT_SERIAL, FILE_PATH_WITHOUT_SERIAL,

                // And one with a serial number
                REPLICATE_NAME_WITH_SERIAL, FILE_PATH_WITH_SERIAL
        );

        // Rename the nickname for the one with the serial number to make sure they split
        setFormElement(Locator.input("name"), NICKNAME_2);
        clickButton("Save");

        assertTextPresent(Q_EXACTIVE_SERIAL_ONLY, 2);  // Once visible on the page, once in a hidden form element
        assertTextPresent(REPLICATE_NAME_WITH_SERIAL, FILE_PATH_WITH_SERIAL);

        String postImpersonationUrl = getDriver().getCurrentUrl();
        impersonateRole("Reader");
        assertTextPresent(Q_EXACTIVE_SERIAL_ONLY, 1);  // Just the visible element, no form and hidden inputs for readers
        stopImpersonating();
        beginAt(postImpersonationUrl);

        // Now resave, scoped to the folder
        Locator.XPathLocator targetContainerLocator = Locator.name("targetContainerId");
        selectOptionByTextContaining(targetContainerLocator.findElement(getDriver()), "In this folder");
        clickButton("Save");

        goToDashboard();
        // We should see the current folder showing both nicknames
        waitForElement(nickname1LinkLocator);
        waitForElement(nickname2LinkLocator);
        // We should also see the subfolder showing the nickname for one and the serial number for the other
        waitForElement(qExactiveWithSerialLinkLocator);

        // The subfolder should only see one of the nicknames
        clickFolder(QC_SUB_SUB_FOLDER);
        waitForElement(nickname1LinkLocator);
        assertElementPresent(qExactiveWithSerialLinkLocator);
        assertElementNotPresent(nickname2LinkLocator);

        // Now try a non-QC folder, which should be inheriting the first nickname but not the second
        clickFolder(NON_QC_SUB_FOLDER);
        clickAndWait(Locator.linkWithText(ISOTOPOLOGUE_FILE_ANNOTATED));
        clickAndWait(Locator.linkWithText("6 replicates"));
        assertElementPresent(nickname1LinkLocator);
        assertElementPresent(qExactiveWithSerialLinkLocator);
        // We should also get links to the QC folders with data from the same instrument
        assertElementPresent(Locator.linkWithText(getProjectName()));
        assertElementPresent(Locator.linkWithText(QC_SUB_FOLDER));
        assertElementPresent(Locator.linkWithText(QC_SUB_SUB_FOLDER));
    }

    @Test
    public void testNonSiteAdmin()
    {
        goToProjectHome();
        clickFolder(NON_QC_SUB_FOLDER);
        clickAndWait(Locator.linkWithText(SAMPLE_FILE_CHROM_INFO));
        clickAndWait(Locator.linkWithText("2 replicates"));
        clickAndWait(Locator.linkWithText(QTRAP));

        String postImpersonationUrl = getDriver().getCurrentUrl();
        // Check we don't let readers save
        impersonateRole("Reader");
        assertTextPresent("Currently saved in");
        assertElementNotPresent(Locator.lkButton("Save"));
        stopImpersonating();
        beginAt(postImpersonationUrl);

        Locator.XPathLocator targetContainerLocator = Locator.name("targetContainerId");
        // Impersonate a user who can only edit in subfolder
        impersonate(USER);
        // Ensure they can't save in /Shared or project
        assertEquals("Wrong number of places to save the nickname", 1, targetContainerLocator.findElement(getDriver()).findElements(Locator.tag("option")).size());
        selectOptionByTextContaining(targetContainerLocator.findElement(getDriver()), "In this folder");
        setFormElement(Locator.input("name"), NICKNAME_3);
        clickButton("Save");

        clickAndWait(Locator.linkWithText(SAMPLE_FILE_CHROM_INFO));
        clickAndWait(Locator.linkWithText("2 replicates"));
        assertElementPresent(Locator.linkWithText(NICKNAME_3));
        stopImpersonating();
    }
}
