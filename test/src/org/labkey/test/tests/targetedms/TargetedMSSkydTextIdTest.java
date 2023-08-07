/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.targetedms;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSSkydTextIdTest extends TargetedMSTest
{
    @BeforeClass
    public static void setupProject()
    {
        TargetedMSTest init = (TargetedMSTest) getCurrentTest();
        init.setupFolder(FolderType.Experiment);
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMSSkydTextIdTest";
    }
    @Test
    public void testUploadSmMolSkydFile() throws Exception
    {
        verifyUploadSmMolSkydFile("SmMolSkydTest.sky.zip");
    }

    @Test
    public void testUploadSmMolSkydFile23_1() throws Exception
    {
        verifyUploadSmMolSkydFile("SmMolSkydTest23_1.sky.zip");
    }

    /**
     * Tests uploading a small molecule document and verifies that chromatograms exists for all of the molecules.
     * Some of the molecules in the Skyline document have a name and a formula, some have a name and a m/z,
     * and some have just a formula, and some have just an m/z.
     * The test verifies that each molecule has chromatograms in both of the replicates, and that the number
     * of chromatograms in each replicate is the same.
     */
    public void verifyUploadSmMolSkydFile(String skydFilename) throws Exception
    {
        goToProjectHome();
        setupSubfolder(getProjectName(), "Small molecules " + skydFilename, FolderType.Experiment);
        importData(skydFilename);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.xpath("//a[text()='" + skydFilename + "']"));

        // Get a list of the details hrefs for all eight molecules.
        Locator molDetailsLocator = Locator.xpath("//a[nobr/img[@title='Molecule Details']]");
        List<String> detailsHrefs = molDetailsLocator.findElements(getDriver()).stream()
                .map(element->element.getAttribute("href"))
                .distinct()
                .collect(Collectors.toList());
        Assert.assertEquals(8, detailsHrefs.size());

        verifyChromatogramReplicates(detailsHrefs,
                "FU2_2017_0915_RJ_05_1ab_30",
                "FU2_2017_0915_RJ_06_1ab_50");
    }

    @Test
    public void testUploadHighPrecModSkydFile() throws Exception
    {
        verifyUploadHighPrecModSkydFile("HighPrecModSkydTest.sky.zip");
    }
    @Test
    public void testUploadHighPrecModSkydFile23_1() throws Exception
    {
        verifyUploadHighPrecModSkydFile("HighPrecModSkydTest23_1.sky.zip");
    }
    /**
     * Tests uploading a Skyline document where one of the first replicate was imported before Skyline started
     * using high precision modifications, and the second replicate uses high precision modifications.
     * The text id's in the .skyd file only match the modified sequences in the .sky file for the second replicate,
     * but the logic in "Peptide.textIdMatches" enables Panorama to find all of the chromatograms.
     */
    public void verifyUploadHighPrecModSkydFile(String skydFilename) throws Exception
    {
        setupSubfolder(getProjectName(), "High Precision Peptide Modifications " + skydFilename, FolderType.Experiment);
        importData(skydFilename);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.xpath("//a[text()='" + skydFilename + "']"));

        // Get a list of the details hrefs for all eight molecules.
        Locator molDetailsLocator = Locator.xpath("//a[nobr/img[@title='Peptide Details']]");
        List<String> detailsHrefs = molDetailsLocator.findElements(getDriver()).stream()
                .map(element->element.getAttribute("href"))
                .distinct()
                .collect(Collectors.toList());
        Assert.assertEquals(10, detailsHrefs.size());
        verifyChromatogramReplicates(detailsHrefs,
                "C20130725_MaierGeneKD_ES_3p1a_EZH2_01",
                "C20130725_MaierGeneKD_ES_3p2a_EZH2_01");
    }

    private void verifyChromatogramReplicates(List<String> detailHrefs, String...expectedReplicates) throws Exception
    {
        List<Locator> locators = Arrays.stream(expectedReplicates)
                .map(rep->Locator.xpath("//div[@alt='Chromatogram " + rep + "']"))
                .collect(Collectors.toList());
        for (String href : detailHrefs)
        {
            goToURL(new URL(href), 10000);
            waitForElement(locators.get(0));
            List<WebElement> firstReplicateChromatograms = locators.get(0).findElements(getDriver());
            Assert.assertNotEquals(0, firstReplicateChromatograms.size());
            for (int iReplicate = 1; iReplicate < locators.size(); iReplicate++)
            {
                List<WebElement> replicateChromatograms = locators.get(iReplicate).findElements(getDriver());
                Assert.assertEquals(firstReplicateChromatograms.size(), replicateChromatograms.size());
            }
        }
    }
}
