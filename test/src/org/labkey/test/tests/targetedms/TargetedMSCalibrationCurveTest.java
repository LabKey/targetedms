/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.TabLoader;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.testng.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tests uploading Skyline documents that contain calibration curve settings. Makes sure that the calculated results
 * match the values that are in the CSV files in /SampleData/TargetedMS/Quantification/CalibrationScenariosTest.
 * Those data were generated from the Skyline unit test "CalibrationScenariosTest".
 */
@Category({DailyB.class, MS2.class})
public class TargetedMSCalibrationCurveTest extends TargetedMSTest
{
    private static final String SAMPLEDATA_FOLDER = "Quantification/CalibrationScenariosTest/";
    public static final List<String> scenarioNames = Collections.unmodifiableList(Arrays.asList(
            "MergedDocuments", "CalibrationTest", "p180test_calibration_DukeApril2016"));

    @Test
    public void testCalibrationScenarios() throws Exception
    {
        setupFolder(FolderType.Experiment);
        for (String scenario : scenarioNames)
        {
            runScenario(scenario);
        }
    }

    private void runScenario(String scenario) throws Exception
    {
        setupSubfolder(getProjectName(), scenario, FolderType.Experiment);
        importData(SAMPLEDATA_FOLDER + scenario + ".sky.zip");
        List<Map<String, Object>> allCalibrationCurves = readScenarioCsv(scenario, "CalibrationCurves");

        for (boolean smallMolecule : Arrays.asList(true, false))
        {
            clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
            click(Locator.linkContainingText(scenario + ".sky.zip"));
            List<Map<String, Object>> expected;
            if (smallMolecule)
            {
                expected = allCalibrationCurves.stream()
                        .filter(row -> "#N/A".equals(row.get("PeptideModifiedSequence")))
                        .collect(Collectors.toList());
                if (expected.isEmpty())
                {
                    continue;
                }
                click(Locator.xpath("//th[span[text() = 'Small Molecule Precursor List']]/span/a/span[contains(@class, 'fa-caret-down')]"));
            }
            else
            {
                expected = allCalibrationCurves.stream()
                        .filter(row -> !"#N/A".equals(row.get("PeptideModifiedSequence")))
                        .collect(Collectors.toList());
                if (expected.isEmpty())
                {
                    continue;
                }
                click(Locator.xpath("//th[span[text() = 'Precursor List']]/span/a/span[contains(@class, 'fa-caret-down')]"));
            }
            Locator calibrationCurveMenuItem = Locator.tagContainingText("span", "Calibration Curves");
            clickAndWait(calibrationCurveMenuItem);
            waitForText("Calibration Curves");
            DataRegionTable calibrationCurvesTable = new DataRegionTable("calibration_curves" + (smallMolecule ? "_sm_mol" : ""), this);
            for (Map<String, Object> expectedRow : expected)
            {
                String peptide = expectedRow.get("Peptide").toString();
                String msg = scenario + "_" + peptide;
                int rowIndex = calibrationCurvesTable.getRowIndex(smallMolecule ? "Molecule" : "Peptide", peptide);
                Assert.assertNotEquals(rowIndex, -1, msg);
                String actualErrorMessage = calibrationCurvesTable.getDataAsText(rowIndex, "Error Message");
                String expectedErrorMessage = (String) expectedRow.get("ErrorMessage");
                if (expectedErrorMessage != null && expectedErrorMessage.length() > 0)
                {
                    Assert.assertNotEquals("", actualErrorMessage);
                }
                else
                {
                    double delta = 1E-4;
                    double actualSlope = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "Slope"));
                    double expectedSlope = Double.parseDouble(expectedRow.get("Slope").toString());
                    Assert.assertEquals(actualSlope, expectedSlope, delta);
                    double actualIntercept = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "Intercept"));
                    double expectedIntercept = Double.parseDouble(expectedRow.get("Intercept").toString());
                    Assert.assertEquals(actualIntercept, expectedIntercept, delta);
                    double actualRSquared = Double.parseDouble(calibrationCurvesTable.getDataAsText(rowIndex, "RSquared"));
                    double expectedRSquared = Double.parseDouble(expectedRow.get("RSquared").toString());
                    if (Math.abs(actualRSquared - expectedRSquared) > delta)
                    {
                        Assert.assertEquals(actualRSquared, expectedRSquared, delta);
                    }
                }
            }
        }
    }

    private List<Map<String, Object>> readScenarioCsv(String scenarioName, String reportName) throws Exception
    {
        File file = TestFileUtils.getSampleData("TargetedMS/" + SAMPLEDATA_FOLDER + scenarioName + "_" + reportName + ".csv");
        try (TabLoader tabLoader = new TabLoader(file, true))
        {
            tabLoader.parseAsCSV();
            tabLoader.setInferTypes(false);
            return tabLoader.load();
        }
    }
}
