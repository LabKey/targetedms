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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, MS2.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSSmallMoleculeLibraryTest extends TargetedMSTest
{
    private static final String SKY_FILE1 = "SmMolLibA.sky.zip";
    private static final String SKY_FILE2 = "SmMolLibB.sky.zip";

    @Test
    public void testSteps()
    {
        setupFolder(FolderType.Library); // Library folder
        importData(SKY_FILE1);
        verifyRevision1();
        importData(SKY_FILE2, 2);
        verifyRevision2();
        verifyAndResolveConflicts();
        verifyRevision3();
    }

    @Override
    protected void selectFolderType(FolderType folderType)
    {
        // Make sure that we're still in the wizard UI
        assertTextPresent("Create Project", "Users / Permissions");
        super.selectFolderType(folderType);
    }

    @LogMethod
    protected void verifyRevision1()
    {
        log("Verifying expected counts in library revision 1 after uploading " + SKY_FILE1);

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(2, 8, 1, false);

        verifyLibraryMoleculeCount(2);

        // Verify one precursor from some of the molecules in the library.  All are from SKY_FILE1 at this point.
        Map<String, List<String>> precursorMap = new HashMap<>();
        precursorMap.put("C4H9NO3[M+H]", Arrays.asList("C4H9NO3", SKY_FILE1, "120.0655"));
        precursorMap.put("C4H9NO3[M4C13+H]", Arrays.asList("C4H9NO3", SKY_FILE1, "124.0789"));
        precursorMap.put("[M+]", Arrays.asList("NICOTINATE", SKY_FILE1, "124.0393"));

        verifyLibraryPrecursors(precursorMap, 4);
    }

    private void verifyLibraryMoleculeCount(int count)
    {
        clickTab("Molecules");
        log("Verify molecule count in the library");
        DataRegionTable moleculeTable = new DataRegionTable("Molecule" ,getDriver());
        assertEquals("Unexpected number of rows in molecules table", count, moleculeTable.getDataRowCount());
    }

    private void verifyChromatogramLibraryDownloadWebPart(int moleculeCount, int transitionCount, int revision, boolean hasConflict)
    {
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        if(!hasConflict)
        {
            assertElementPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
        }
        else
        {
            // The library stats graph is not displayed if the folder has conflicts.
            assertElementNotPresent(Locator.xpath("//img[contains(@src, 'graphLibraryStatistics.view')]"));
            assertTextPresent("The library cannot be extended until the conflicts are resolved", "The download link below is for the last stable version of the library");
        }
        assertTextPresent(
                moleculeCount + " molecules",
                transitionCount + " ranked transitions");
        assertElementPresent(Locator.lkButton("Download"));
        assertTextPresent("Revision " + revision);
    }


    @LogMethod
    protected void verifyRevision2()
    {
        log("Verifying expected counts in library revision 2 after uploading " + SKY_FILE2);

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        // The folder has conflicts so the download link and the numbers should be for the last stable library built
        // before conflicts, which is revision 1.
        verifyChromatogramLibraryDownloadWebPart(2,8, 1, true);

        verifyLibraryMoleculeCount(3);

        // Verify one precursor from some of the molecules in the library.
        // From SKY_FILE1
        Map<String,List<String>> precursorMap = new HashMap<>();
        // Conflicted
        precursorMap.put("C4H9NO3[M+H]", Arrays.asList("C4H9NO3", SKY_FILE1, "120.0655"));
        precursorMap.put("C4H9NO3[M4C13+H]", Arrays.asList("C4H9NO3", SKY_FILE1, "124.0789"));
        precursorMap.put("[M+]", Arrays.asList("NICOTINATE", SKY_FILE1, "124.0393"));

        // The precursors below are only in SKY_FILE2 so will not result in a conflict.
        precursorMap.put("[M3.01007+]", Arrays.asList("CYSTEINE", SKY_FILE2, "125.0371"));

        verifyLibraryPrecursors(precursorMap, 5);
    }

    private void verifyLibraryPrecursors(Map<String, List<String>> precursorMap, int totalPrecursorCount)
    {
        log("Verify precursors in the library");

        DataRegionTable precursorTable = new DataRegionTable("LibraryMoleculePrecursor" ,getDriver());
        if(totalPrecursorCount > 100)
        {
            precursorTable.getPagingWidget().setPageSize(250, true);
        }

        assertEquals("Unexpected number of rows in precursors table", totalPrecursorCount, precursorTable.getDataRowCount());

        for(Map.Entry<String, List<String>> entry: precursorMap.entrySet())
        {
            String precursor = entry.getKey();
            int idx = precursorTable.getRowIndex("Ion Formula", precursor);
            assertTrue("Expected precursor " + precursor + " not found in table", idx != -1);
            List<String> rowValues = precursorTable.getRowDataAsText(idx, "Molecule Name", "File", "Q1 m/z");
            assertEquals("Wrong data for row", entry.getValue(), rowValues);
        }
    }


    @LogMethod
    private void verifyAndResolveConflicts()
    {
        clickTab("Panorama Dashboard");
        log("Verifying that expected conflicts exist");
        String[] conflictText = new String[] {"The last Skyline document imported in this folder had 2 molecules that were already a part of the library",
                "Please click the link below to resolve conflicts and choose the version of each molecule that should be included in the library",
                "The library cannot be extended until the conflicts are resolved. The download link below is for the last stable version of the library"};
        assertTextPresent(conflictText);
        var resolveConflictsLink = Locator.tagWithClass("div", "labkey-download").descendant(Locator.linkWithText("RESOLVE CONFLICTS"));
        assertElementPresent(resolveConflictsLink);
        clickAndWait(resolveConflictsLink);
        assertTextPresent(
                "Newly Imported Data",
                "Current Library Data",
                "Resolve conflicts for " + SKY_FILE2 + ".");

        int expectedConflictCount = 3;
        assertEquals(expectedConflictCount + 2 /*add header rows*/, getTableRowCount("dataTable"));

        Set<String> expectedConflicts = new HashSet<>();
        expectedConflicts.add("C4H9NO3[M+H]");
        expectedConflicts.add("C4H9NO3[M4C13+H]");
        expectedConflicts.add("CYSTEINE");

        // Verify rows in the conflicts table
        Locator.XPathLocator table = Locator.id("dataTable");
        for(int i = 0; i < expectedConflictCount; i++)
        {
            String conflict = getTableCellText(table, i, 2);
            assertTrue("Unexpected row in conflicts table " + conflict, expectedConflicts.contains(conflict));
        }

        clickButton("Apply Changes");
    }

    protected void verifyRevision3()
    {
        log("Verifying expected counts in library revision 3 after resolving conflicts. ");

        // Download link, library statistics and revision in the ChromatogramLibraryDownloadWebpart
        verifyChromatogramLibraryDownloadWebPart(3, 10, 3, false);

        verifyLibraryMoleculeCount(3);

        Map<String, List<String>> precursorMap = new HashMap<>();
        precursorMap.put("C4H9NO3[M+H]", Arrays.asList("C4H9NO3", SKY_FILE2, "120.0655"));
        precursorMap.put("C4H9NO3[M4C13+H]", Arrays.asList("C4H9NO3", SKY_FILE2, "124.0789"));
        precursorMap.put("[M+]", Arrays.asList("CYSTEINE", SKY_FILE2, "122.0270"));
        precursorMap.put("[M3.01007+]", Arrays.asList("CYSTEINE", SKY_FILE2, "125.0371"));
        precursorMap.put("[M6.02013+]", Arrays.asList("NICOTINATE", SKY_FILE1, "130.0594"));

        verifyLibraryPrecursors(precursorMap, 5);
    }
}