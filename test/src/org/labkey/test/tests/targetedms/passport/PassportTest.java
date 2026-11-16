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
package org.labkey.test.tests.targetedms.passport;


import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.util.APIUserHelper;
import org.labkey.test.util.LogMethod;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests Panorama Passport integration end-to-end, covering user management, permission setup, and project
 * configuration for both normal and admin users.
 */
@Category({})
public class PassportTest extends PassportTestPart
{
    // Before/After ratio of Normalization-standard area for Haptoglobin in data1.sky.zip; TotalArea is float4, so allow its rounding
    private static final double EXPECTED_NORMALIZATION_RATIO = 1.00232153;
    private static final double NORMALIZATION_RATIO_TOLERANCE = 1e-6;

    @Test
    public void testSteps()
    {
        APIUserHelper h = new APIUserHelper(this);
        h.createUser(NORMAL_USER);

        setupProject();
        testAsSuperAdmin();
        testAsNormalUser();
    }

    @LogMethod
    protected void testAsSuperAdmin()
    {
        signIn();
        clickProject(getProjectName());
        testNormalStuff();
    }

    private void testNormalStuff()
    {
        // assert side bar and pipeline tab not visible... only passport tab should be seen by anproteiny user
        assertElementNotPresent(Locator.xpath("//a[@id='PipelineTab']"));
        assertTextNotPresent("Pages");
        // enter Haptoglobin

        clickAndWait(Locator.xpath("//tr[contains(@class,'labkey-alternate-row')]//a[@class='labkey-text-link'][contains(text(),'PASSPORT VIEW')]"));
        assertTextPresent("Haptoglobin", "data1.sky.zip");
        verifyNormalizedIntensities();
        assertElementPresent(Locator.xpath("//div[@id='rangesliderdeg']"));
        assertElementPresent(Locator.xpath("//div[@id='rangesliderlength']"));
        assertElementPresent(Locator.xpath("//button[@id='formreset']"));
        assertElementPresent(Locator.xpath("//a[contains(text(),'P00738')]"));
        assertElementPresent(Locator.xpath("//div[@id='chart']"));
        String[] peptidesOrderIntensity = {"VGYVSGWGR", "GSFPWQAK","YVMLPVADQDQCIR","QLVEIEK","SCAVAEYGVYVK","TEGDGVYTLNNEK",
                "VMPICLPSK","VTSIQDWVQK","TEGDGVYTLNDK","DYAEVGR","ILGGHLDAK","HYEGSTVPEK","DIAPTLTLYVGK","SPVGVQPILNEHTFCAGMSK",
                "LPECEAVCGKPK","VVLHPNYSQVDIGLIK","NPANPVQR","NLFLNHSENATAK","MVSHHNLTTGATLINEQWLLTTAK"};
        for(int i = 0; i < peptidesOrderIntensity.length; i++) {
            int index = i+1;
            assertElementContains(Locator.xpath("//ul[@id='livepeptidelist']//li["+index+"]"), peptidesOrderIntensity[i]);
        }
        selectOptionByValue(Locator.xpath("//select[@id='peptideSort']"), "sequencelocation");
        String[] peptidesOrderLocation = {"TEGDGVYTLNDK","TEGDGVYTLNNEK","LPECEAVCGKPK","NPANPVQR","ILGGHLDAK",
                "GSFPWQAK","MVSHHNLTTGATLINEQWLLTTAK","NLFLNHSENATAK","DIAPTLTLYVGK","QLVEIEK","VVLHPNYSQVDIGLIK","VMPICLPSK",
                "DYAEVGR","VGYVSGWGR","YVMLPVADQDQCIR","HYEGSTVPEK","SPVGVQPILNEHTFCAGMSK","SCAVAEYGVYVK","VTSIQDWVQK"};
        for(int i = 0; i < peptidesOrderLocation.length; i++) {
            int index = i+1;
            assertElementContains(Locator.xpath("//ul[@id='livepeptidelist']//li["+index+"]"), peptidesOrderLocation[i]);
        }
        click(Locator.xpath("//button[@id='formreset']")); // reset form check reset works
        waitForElement(Locator.tagWithId("ul", "livepeptidelist"));
        for(int i = 0; i < peptidesOrderIntensity.length; i++) {
            int index = i+1;
            assertElementContains(Locator.xpath("//ul[@id='livepeptidelist']//li["+index+"]"), peptidesOrderIntensity[i]);
        }
        assertElementContains(Locator.xpath("//span[@id='filteredPeptideCount']//green"), "19");

        dragAndDrop(Locator.xpath("//div[@id='rangesliderdeg']//span[1]"), 50, 0);
        assertElementContains(Locator.xpath("//span[@id='filteredPeptideCount']//green"), "15");
        click(Locator.xpath("//button[@id='formreset']")); // reset form check reset works

        waitForElement(Locator.tagWithId("span", "filteredPeptideCount").childTag("green"));
        assertElementContains(Locator.xpath("//span[@id='filteredPeptideCount']//green"), "19");

        // Features come from a live query against Uniprot, so be tolerant of any number above our baseline
        waitForElementsAtLeast(Locator.tagWithClass("td", "feature-sequencevariant"), 3);
        waitForElementsAtLeast(Locator.tagWithClass("td", "feature-glycosylationsite"), 3);
        waitForElementsAtLeast(Locator.tagWithClass("td", "feature-helix"), 3);
        waitForElementsAtLeast(Locator.tagWithClass("td", "feature-turn"), 3);
    }

    // normalizedAfter = after * beforeNormSum / afterNormSum, so every peptide shares one ratio to its raw After area
    private void verifyNormalizedIntensities()
    {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> peptides = (List<Map<String, Object>>) executeScript("return proteinJSON.peptides;");
        assertEquals("Peptide count", 19, peptides.size());
        Double firstRatio = null;
        for (Map<String, Object> peptide : peptides)
        {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> replicates = (List<Map<String, Object>>) peptide.get("replicateInfo");
            Double rawBefore = lastArea(replicates, "BeforeIncubation");
            Double rawAfter = lastArea(replicates, "AfterIncubation");
            Number normalizedAfter = (Number) peptide.get("normalizedafterintensity");
            if (rawBefore == null || rawAfter == null || rawBefore == 0)
            {
                assertNull("Normalized intensity without a measured Before area for " + peptide.get("sequence"), normalizedAfter);
                continue;
            }
            assertNotNull("Normalized intensity for " + peptide.get("sequence"), normalizedAfter);
            if (rawAfter == 0)
            {
                assertEquals("Normalized intensity for unmeasured After area of " + peptide.get("sequence"), 0, normalizedAfter.doubleValue(), 0);
                continue;
            }
            double ratio = normalizedAfter.doubleValue() / rawAfter;
            assertEquals("Normalization ratio for " + peptide.get("sequence"), EXPECTED_NORMALIZATION_RATIO, ratio, NORMALIZATION_RATIO_TOLERANCE);
            if (firstRatio == null)
                firstRatio = ratio;
            assertEquals("Normalization ratio differs between peptides for " + peptide.get("sequence"), firstRatio, ratio, 1e-12);
        }
    }

    private static Double lastArea(List<Map<String, Object>> replicates, String replicate)
    {
        return replicates.stream()
                .filter(r -> replicate.equals(r.get("replicate")))
                .reduce((_, b) -> b) // the page keeps the last precursor's area
                .map(r -> ((Number) r.get("intensity")).doubleValue())
                .orElse(null);
    }

    public void waitForElementsAtLeast(final Locator loc, final int count)
    {
        waitFor(() -> loc.findElements(getDriver()).size() >= count, WAIT_FOR_JAVASCRIPT);
        assertTrue("Element not present at least the expected number of times", loc.findElements(getDriver()).size() >= count);
    }


    @LogMethod
    protected void testAsNormalUser()
    {
        impersonate(NORMAL_USER);
        clickProject(getProjectName());
        testNormalStuff();

        // Log back in as a site admin
        stopImpersonating();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
