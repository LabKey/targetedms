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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;

@Category({})
public class TargetedMSMAMTest extends TargetedMSTest
{
    protected static final String SKY_FILE = "iRT Human+Standard Calibrate.zip";
    protected static final String CROSS_LINKED_SKY_FILE = "CrosslinkPeptideMapTest.sky.zip";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSMAMTest init = (TargetedMSMAMTest) getCurrentTest();
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

        verifyRunSummaryCountsPep(125,158,0, 160,628, 1, 0, 0);

        clickAndWait(Locator.linkContainingText("PTM Report"));

        assertElementPresent("Wrong modification count", Locator.xpath("//td[contains(text(), 'Carbamidomethyl Cysteine')]"), 9);
        assertTextPresentInThisOrder("(K)HDLDLICR(A)", "(K)YLECSALTQR(G)", "(R)YVDIAIPCNNK(G)");
        assertTextPresentInThisOrder("C245", "C157", "C163");
        assertTextPresent("A_D110907_SiRT_HELA_11_nsMRM_150selected_2_30min-5-35", "A_D110907_SiRT_HELA_11_nsMRM_150selected_1_30min-5-35");

        clickAndWait(Locator.linkContainingText("Peptide Map"));
        assertTextPresentInThisOrder("11.3", "14.1", "14.8");
        assertTextPresentInThisOrder("1501.75", "1078.50", "1547.71");
        assertTextPresentInThisOrder("NU205", "NU205", "1433Z");
        assertTextPresentInThisOrder("70-84", "325-333", "28-41");
        assertTextPresentInThisOrder("(K)ASTEGVAIQGQQGTR(L)", "(K)AQYEDIANR(S)", "(K)SVTEQGAELSNEER(N)");
        assertTextPresentInThisOrder("Carbamidomethyl Cysteine @ C157", "Carbamidomethyl Cysteine @ C245", "Carbamidomethyl Cysteine @ C94");
    }

    @Test
    public void testCrossLinkedPeptideMap()
    {
        goToProjectHome();

        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(CROSS_LINKED_SKY_FILE));

        verifyRunSummaryCountsPep(2,3,0, 3,3, 1, 0, 0);

        clickAndWait(Locator.linkContainingText("Peptide Map"));
        assertTextPresentInThisOrder("364-366", "367-369", "364-367");
        assertTextPresentInThisOrder("Q364, N366", "T369", "D364");
        assertTextPresentInThisOrder("(A)LKPLALV(D)", "(G)AVVQDPA(Y)", "(F)YGEATSR(E)");
    }
}
