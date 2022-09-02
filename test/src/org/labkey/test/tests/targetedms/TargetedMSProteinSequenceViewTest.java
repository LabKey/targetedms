package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.targetedms.SequenceCoverageWebPart;

import java.util.Arrays;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 12)
public class TargetedMSProteinSequenceViewTest extends TargetedMSTest
{
    private static final String CONFIDENCE_SCORE_FOLDER = "Confidence Score Folder";
    private static final String MODIFIED_PEPTIDES = "Modified Peptides Folder";

    private static final String CS_SKY_FILE = "ConfidenceScore.sky.zip";
    private static final String MP_SKY_FILE = "ModifiedPeptides.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSProteinSequenceViewTest init = (TargetedMSProteinSequenceViewTest) getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        setupFolder(FolderType.Experiment);

        setupSubfolder(getProjectName(), CONFIDENCE_SCORE_FOLDER, FolderType.Experiment);
        importData(CS_SKY_FILE);

        goToProjectHome();
        setupSubfolder(getProjectName(), MODIFIED_PEPTIDES, FolderType.Experiment);
        importData(MP_SKY_FILE);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Test
    public void testIntensityAndConfidenceScale()
    {
        log("Navigate to sequence coverage map");
        navigateToFolder(getProjectName(), CONFIDENCE_SCORE_FOLDER);
        waitAndClickAndWait(Locator.linkWithText(CS_SKY_FILE));
        clickAndWait(Locator.linkWithText("sp|O13527|YA11B_YEAST"));
        clickAndWait(Locator.linkWithText("sp|O13527|YA11B_YEAST"));

        SequenceCoverageWebPart sequenceCoverage = new SequenceCoverageWebPart(getDriver());
        checker().verifyEquals("Incorrect default selection of display By", "Intensity", sequenceCoverage.getDisplayBy());

        checker().verifyEquals("Incorrect heatMap legend values", Arrays.asList("6.85", "5.27", "3.68"), sequenceCoverage.getHeatMapLegendValues());

    }

    @Test
    public void testPeptideFormSelection()
    {

    }
}
