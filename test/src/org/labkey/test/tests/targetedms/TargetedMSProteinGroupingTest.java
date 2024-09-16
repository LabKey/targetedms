package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.util.DataRegionTable;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class TargetedMSProteinGroupingTest extends TargetedMSTest
{
    private static final String SKY_FILE = "ProteinGroup.sky";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSProteinGroupingTest init = (TargetedMSProteinGroupingTest) getCurrentTest();
        init.doInit();
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doInit()
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE);
    }

    @Test
    public void testProteinGrouping()
    {
        String group = "sp|O60814|H2B1K_HUMAN / sp|P06899|H2B1J_HUMAN / sp|P23527|H2B1O_HUMAN / sp|P33778|H2B1B_HUMAN /" +
                " sp|P57053|H2BFS_HUMAN / sp|P58876|H2B1D_HUMAN / sp|P62807|H2B1C_HUMAN / sp|Q16778|H2B2E_HUMAN / " +
                "sp|Q5QNW6|H2B2F_HUMAN / sp|Q8N257|H2B3B_HUMAN / sp|Q93079|H2B1H_HUMAN / sp|Q99877|H2B1N_HUMAN / " +
                "sp|Q99879|H2B1M_HUMAN / sp|Q99880|H2B1L_HUMAN";

        goToProjectHome();
        clickAndWait(Locator.linkWithText(SKY_FILE));
        clickAndWait(Locator.linkWithText(group));

        log("Verifying protein matches for peptide");
        DataRegionTable proteinTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Proteins").waitFor();
        String protein1 = "sp|O60814|H2B1K_HUMAN";
        checker().verifyEquals("Incorrect number of protein matched", 14, proteinTable.getDataRowCount());
        checker().verifyTrue("Incorrect title for Sequence coverage webpart for " + protein1,
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Sequence Coverage for " + protein1)));
        checker().verifyEquals("Incorrect Sequence for " + protein1,
                "MPEPAKSAPAPKKGSKKAVTKAQKKDGKKRKRSRKESYSVYVYKVLKQVHPDTGISSKAMGIMNSFVNDIFERIAGEASRLAHYNKRSTITSREIQTAVRLLLPGELAKHAVSEGTKAVTKYTSAK",
                getCurrentSequence());

        String protein2 = "sp|P33778|H2B1B_HUMAN";
        clickAndWait(Locator.linkWithText(protein2));
        checker().verifyTrue("Incorrect title for Sequence coverage webpart for " + protein2,
                isElementPresent(Locator.tagWithAttribute("h3", "title", "Sequence Coverage for " + protein2)));
        checker().verifyEquals("Incorrect Sequence for " + protein2,
                "MPEPSKSAPAPKKGSKKAITKAQKKDGKKRKRSRKESYSIYVYKVLKQVHPDTGISSKAMGIMNSFVNDIFERIAGEASRLAHYNKRSTITSREIQTAVRLLLPGELAKHAVSEGTKAVTKYTSSK",
                getCurrentSequence());

        DataRegionTable peptideTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Peptides").waitFor();
        checker().verifyEquals("Incorrect number of peptides imported", 1, peptideTable.getDataRowCount());
        CustomizeView customizeView = peptideTable.openCustomizeGrid();
        customizeView.addColumn("PeptideGroupId");
        customizeView.applyCustomView();
        peptideTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Peptides").waitFor();
        checker().verifyEquals("Incorrect group for the protein", group, peptideTable.getDataAsText(0, "PeptideGroupId"));
    }

    private String getCurrentSequence()
    {
        String unformattedString = (String) executeScript("return document.getElementById(\"peptideMap\").textContent;");
        /*
            The textContent is getting the whole table text content along with this "The number displayed on each peptide box is the rank" text.
            When we replace everything but capital letters T stays back along with sequence so substring to get the sequence string.
         */
        return unformattedString.replaceAll("[^A-Z]", "").trim().substring(1);
    }

}
