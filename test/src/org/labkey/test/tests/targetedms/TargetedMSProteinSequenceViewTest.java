package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.junit.LabKeyAssert;
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
    private static final String MP_SKY_FILE = "ModifiedPeptidesWithCDRAnnotation.sky.zip";

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

        log("Verifying Intensity values");
        SequenceCoverageWebPart sequenceCoverage = new SequenceCoverageWebPart(getDriver());
        checker().verifyEquals("Incorrect default selection of display By", "Intensity", sequenceCoverage.getDisplayBy());
        checker().verifyEquals("Incorrect heatMap legend values for Intensity", Arrays.asList("6.85", "5.27", "3.68"), sequenceCoverage.getHeatMapLegendValues());
        checker().verifyEquals("Incorrect Peptide Details for Intensity 23(Blue)", "Mass: 2890.21\n" +
                "Start: 99\n" +
                "End: 122\n" +
                "Unmodified: 1\n" +
                "Intensity Rank: 23\n" +
                "Raw Intensity: 4.821E+03\n" +
                "Log 10 Base Intensity: 3.68", sequenceCoverage.getPopUpDetails("23"));
        checker().verifyEquals("Incorrect peptide details for Intensity 4(Red)", "Mass: 1675.81\n" +
                "Start: 123\n" +
                "End: 137\n" +
                "Unmodified: 1\n" +
                "Intensity Rank: 4\n" +
                "Raw Intensity: 4.753E+06\n" +
                "Log 10 Base Intensity: 6.68", sequenceCoverage.getPopUpDetails("4"));
        checker().screenShotIfNewError("Intensity_Errors");

        log("Verifying replicate selection with intensity");
        checker().verifyEquals("Incorrect default selection for Replicate", "All", sequenceCoverage.getReplicate());
        sequenceCoverage = sequenceCoverage.setReplicate("20170901_CalCurves_YeastCurve_A_1ug-ul_BY4741_166");
        checker().verifyEquals("Incorrect heatMap legend values for Intensity and single replicate", Arrays.asList("6.66", "5.17", "3.68"), sequenceCoverage.getHeatMapLegendValues());
        checker().verifyEquals("Incorrect Peptide Details for Intensity 15(Blue) and single replicate", "Mass: 2890.21\n" +
                "Start: 99\n" +
                "End: 122\n" +
                "Unmodified: 1\n" +
                "Intensity Rank: 15\n" +
                "Raw Intensity: 4.821E+03\n" +
                "Log 10 Base Intensity: 3.68", sequenceCoverage.getPopUpDetails("15"));
        checker().verifyEquals("Incorrect peptide details for Intensity 12(light red) and single replicate", "Mass: 1630.88\n" +
                "Start: 165\n" +
                "End: 178\n" +
                "Unmodified: 1\n" +
                "Intensity Rank: 12\n" +
                "Raw Intensity: 2.436E+05\n" +
                "Log 10 Base Intensity: 5.39", sequenceCoverage.getPopUpDetails("12"));
        checker().screenShotIfNewError("Intensity_And_SingleReplicate_Errors");

        log("Verifying Confidence score value");
        sequenceCoverage = sequenceCoverage.setReplicate("All");
        sequenceCoverage = sequenceCoverage.setDisplayBy("Confidence Score");
        checker().verifyEquals("Incorrect heatMap legend values for confidence score", Arrays.asList("5.04", "3.61", "2.17"), sequenceCoverage.getHeatMapLegendValues());
        checker().verifyEquals("Incorrect Peptide Details 23(Blue)", "Mass: 846.08\n" +
                "Start: 654\n" +
                "End: 661\n" +
                "Unmodified: 1\n" +
                "Confidence Score Rank: 23\n" +
                "Raw Confidence: 0.006768\n" +
                "Log 10 Base Confidence Score: 2.17", sequenceCoverage.getPopUpDetails("23"));
        checker().screenShotIfNewError("Confidence_Score_Errors");
    }

    @Test
    public void testPeptideFormSelection()
    {
        navigateToFolder(getProjectName(), MODIFIED_PEPTIDES);
        waitAndClickAndWait(Locator.linkWithText(MP_SKY_FILE));
        clickAndWait(Locator.linkWithText("NISTmAb_HC"));

        log("Verified the combined modified form");
        SequenceCoverageWebPart sequenceCoverage = new SequenceCoverageWebPart(getDriver());
        checker().verifyEquals("Incorrect value for heat map legend for modified peptides", Arrays.asList("9.73", "8.41", "7.08"), sequenceCoverage.getHeatMapLegendValues());
        String combinedExpected = "Mass: 597.72\n" +
                "Start: 1\n" +
                "End: 5\n" +
                "Unmodified: 1\n" +
                "Intensity Rank: 7\n" +
                "Raw Intensity: 2.242E+09\n" +
                "Log 10 Base Intensity: 9.35\n" +
                "Modified Forms Log Raw Intensity\n" +
                "Q[-17.026549]VTLR 9.35 2.232E+09\n" +
                "QVTLR 7.10 1.251E+07";
        LabKeyAssert.assertEqualsSorted("Incorrect Peptide Details for modified peptide QVTLR", Arrays.asList(combinedExpected.split("\n"))
                , Arrays.asList(sequenceCoverage.getPopUpDetails("7").split("\n")));
        log("Verifying the stacked modified form");
        sequenceCoverage = sequenceCoverage.setModifiedForm("stacked");
        String stackedExpected = "Mass: 1171.19\n" +
                "Start: 296\n" +
                "End: 304\n" +
                "Unmodified: 1\n" +
                "Intensity Rank: 20\n" +
                "Raw Intensity: 2.531E+07\n" +
                "Log 10 Base Intensity: 7.40\n" +
                "Modified Forms Log Raw Intensity\n" +
                "EEQYN[+1444.53387]STYR 7.40 2.531E+07\n" +
                "EEQYN[+1606.586693]STYR 7.34 2.209E+07\n" +
                "EEQYNSTYR 6.69 4.949E+06\n" +
                "EEQYN[+1768.639517]STYR 6.30 1.987E+06";
        LabKeyAssert.assertEqualsSorted("Incorrect values for Stacked", Arrays.asList(stackedExpected.split("\n")), Arrays.asList(sequenceCoverage.getPopUpDetails("20").split("\n")));
        checker().screenShotIfNewError("Modified_Forms_Error");
    }
}
