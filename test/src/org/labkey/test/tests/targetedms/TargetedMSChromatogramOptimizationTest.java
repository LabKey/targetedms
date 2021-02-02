package org.labkey.test.tests.targetedms;


import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.targetedms.ConnectionSource;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSChromatogramOptimizationTest extends TargetedMSTest
{
    private static final String SKY_FILE = "SmallMoleculeLibrary3.sky.zip";

    @BeforeClass
    public static void setupProject()
    {
        TargetedMSChromatogramOptimizationTest init = (TargetedMSChromatogramOptimizationTest) getCurrentTest();
        init.setupFolder(FolderType.Library);
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS Chromatogram Optimization Test";
    }

    @Test
    public void testUpload() throws Exception
    {
        goToProjectHome();
        importData(SKY_FILE);
        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        File downloadedClibFile = doAndWaitForDownload(() -> clickButton("Download", 0));

        goToProjectHome();
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", "TransitionOptimization");
        table.rowSelector().showAll();

        log("Verifying the new tables added");
        checker().verifyTrue("TransitionOptimization table is not present in the SQLITE file",
                tableExists(downloadedClibFile, "TransitionOptimization"));
        checker().verifyTrue("MoleculeTransitionOptimization table is not present in SQLITE file",
                tableExists(downloadedClibFile, "MoleculeTransitionOptimization"));

        log("Verifying the SampleFile modifications");
        checker().verifyTrue("Sample File does not have CePredictorId",
                columnExists(downloadedClibFile, "SampleFile", "CePredictorId"));
        checker().verifyTrue("Sample File does not have DpPredictorId",
                columnExists(downloadedClibFile, "SampleFile", "DpPredictorId"));

        checker().verifyEquals("Invalid number of rows in transition optimization", table.getDataRowCount(),
                sizeOfTable(downloadedClibFile, "MoleculeTransitionOptimization"));

    }

    private int sizeOfTable(File clibFile, String name) throws SQLException
    {
        int cnt = 0;
        @SuppressWarnings("SqlResolve")
        String sql = "SELECT * FROM " + name;
        Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath());
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next())
            cnt++;
        return cnt;
    }

    private boolean tableExists(File clibFile, String tableName) throws SQLException
    {
        Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath());
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getTables(null, null, tableName, null);
        if (rs.next())
            return true; //Table exists
        else
            return false;
    }

    private boolean columnExists(File clibFile, String tableName, String columnName) throws SQLException
    {
        try (Connection conn = ConnectionSource.getConnection(clibFile.getAbsolutePath()))
        {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, tableName, columnName))
            {
                if (rs.next())
                    return true; //Table exists
                else
                    return false;
            }
        }
    }
}