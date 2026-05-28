/*
 * Copyright (c) 2020-2026 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

import static org.junit.Assert.assertTrue;

/**
 * Tests MAM (Multi-Attribute Monitoring) experiment folders, including cross-linked peptides and iRT data.
 */
@Category({})
public class TargetedMSMAMTest extends TargetedMSTest
{
    protected static final String SKY_FILE = "iRT Human+Standard Calibrate.zip";
    protected static final String CROSS_LINKED_SKY_FILE = "CrosslinkPeptideMapTest.sky.zip";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSMAMTest init = getCurrentTest();
        init.setupFolder(FolderType.ExperimentMAM);
        init.importData(SKY_FILE, 1);
        init.importData(CROSS_LINKED_SKY_FILE, 2);
    }

    @Test
    public void testSteps()
    {
        goToProjectHome();

        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));

        verifyRunSummaryCountsPep(125, 124, 158, 0, 160, 628, 1, 0, 0);

        clickAndWait(Locator.linkContainingText("PTM Report"));

        assertElementPresent("Wrong modification count", Locator.xpath("//td[contains(text(), 'Carbamidomethyl Cysteine')]"), 9);
        assertTextPresentInThisOrder("(K)HDLDLICR(A)", "(K)YLECSALTQR(G)", "(R)YVDIAIPCNNK(G)");
        assertTextPresentInThisOrder("C245", "C157", "C163");

        assertTextPresent("Chromatograms");

        clickAndWait(Locator.linkContainingText("Peptide Map"));
        DataRegionTable table = new DataRegionTable("PeptideIds", getDriver());
        table.setPageSize(250);
        assertTextPresentInThisOrder("11.3", "14.1", "14.8");
        assertTextPresentInThisOrder("1501.75", "1078.50", "1547.71");
        assertTextPresentInThisOrder("NU205", "1433Z", "RL35", "HSP72; HSP7C");
        assertTextNotPresent("UCRI; RL35"); // Ensure we don't have non-tryptic matches anymore
        assertTextPresentInThisOrder("70-84", "325-333", "28-41", "305-314; 302-311");
        assertTextPresentInThisOrder("(K)ASTEGVAIQGQQGTR(L)", "(K)AQYEDIANR(S)", "(K)SVTEQGAELSNEER(N)");
        assertTextPresentInThisOrder("Carbamidomethyl Cysteine @ C157", "Carbamidomethyl Cysteine @ C245", "Carbamidomethyl Cysteine @ C94");

        // Ensure that the Cystine isn't highlighted, as it's a fixed modification and that report doesn't want to call it out
        assertTrue(getHtmlSource().contains("(K)YLECSALTQR(G)"));
    }

    @Test
    public void testCrossLinkedPeptideMap()
    {
        goToProjectHome();

        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(CROSS_LINKED_SKY_FILE));

        verifyRunSummaryCountsPep(2, 2, 2, 0, 2, 2, 1, 0, 0);

        clickAndWait(Locator.linkContainingText("Peptide Map"));
        assertTextPresentInThisOrder("121-124", "342-345", "142-145");
        // Disulfide bonds
        assertTextPresentInThisOrder("V121-S345-Q142/\nQ124-S345-Q142", "L11-A137-Y271/\nL11-A137-Y271/\nV17-A137-Y271/");
        assertTextPresentInThisOrder("(K)LKPLALV(D)", "(K)AVVQDPA(Y)", "(R)YGEATSR(E)");

        // Ensure that the highlighting is as expected for both crosslinking and modification
        assertTrue(getHtmlSource().contains("(R)<span style=\"font-weight:bold;color:green;text-decoration:underline;\">V</span>SS<span style=\"font-weight:bold;color:green;\">Q</span>(Q)"));
    }
}
