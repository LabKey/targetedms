package org.labkey.test.tests.panoramapremium;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.components.targetedms.QCSummaryWebPart;
import org.labkey.test.pages.admin.ExportFolderPage;
import org.labkey.test.pages.panoramapremium.ConfigureMetricsUIPage;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class TargetedMSQCFolderImportExport extends TargetedMSPremiumTest
{
    private final static String IMPORT_FOLDER = "TargetedMS QC Import Folder";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSQCFolderImportExport init = (TargetedMSQCFolderImportExport) getCurrentTest();
        init.doInit();
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    private void doInit()
    {
        setupFolder(FolderType.QC);
        setUpFolder(IMPORT_FOLDER, FolderType.QC);

        goToProjectHome();
        importData(SProCoP_FILE);

        goToProjectHome(IMPORT_FOLDER);
        importData(SProCoP_FILE);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        // these tests use the UIContainerHelper for project creation, but we can use the APIContainerHelper for deletion
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(getProjectName(), afterTest);
        apiContainerHelper.deleteProject(IMPORT_FOLDER, afterTest);
    }

    @Test
    public void testQCFolderImport()
    {
        String customMetricName = "Custom Metric - Exporting";
        String annotationType = "Test QC Annotation type";
        log("Updating the folder to be exported with data points");
        createGuideSet(getProjectName());
        addCustomMetric(getProjectName(), customMetricName, "targetedms", "AQCTest_Metric");
        addAnnotationType(getProjectName(), annotationType);
        excludePrecursors(getProjectName(), 0);

        goToProjectHome();
        ExportFolderPage exportFolderPage = goToFolderManagement().goToExportTab();
        File exportedFile = exportFolderPage.exportToBrowserAsZipFile();

        goToProjectHome(IMPORT_FOLDER);
        goToFolderManagement().goToImportTab()
                .selectLocalZipArchive()
                .chooseFile(exportedFile)
                .setValidateAllQueries(false)
                .clickImportFolder();
        waitForPipelineJobsToFinish(2);

        goToProjectHome(IMPORT_FOLDER);
        log("After import : Verifying the custom metric");
        goToConfigureMetricsUI();
        waitForElement(Locator.linkWithText(customMetricName));
        assertTextPresent(customMetricName);

        log("After import : Verifying the guide set");
        clickTab("Guide Sets");
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).waitFor();
        Assert.assertEquals("Incorrect number of guideSets imported", 1, table.getDataRowCount());
        Assert.assertEquals("Incorrect Training start date", "2013-08-16 00:51", table.getDataAsText(0, "TrainingStart"));
        Assert.assertEquals("Incorrect Training end date", "2013-08-17 06:10", table.getDataAsText(0, "TrainingEnd"));

        log("After import : Verifying the Annotation");
        clickTab("Annotations");
        assertTextPresent(annotationType);

        log("After import : Verifying excluded precursor");
        goToSchemaBrowser();
        table = viewQueryData("targetedms", "ExcludedPrecursors");
        Assert.assertEquals("Incorrect excluded precursor imported", 1, table.getDataRowCount());
        Assert.assertEquals("Incorrect sequence excluded", "VYVEELKPTPEGDLEILLQK", table.getDataAsText(0, "ModifiedSequence"));
    }

    private void createGuideSet(String projectName)
    {
        goToProjectHome(projectName);
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", "GuideSet");
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_TrainingStart"), "2013-08-16 00:51:16");
        setFormElement(Locator.name("quf_TrainingEnd"), "2013-08-17 06:10:02");
        clickButton("Submit");

        table = new DataRegionTable("query", getDriver());
        Assert.assertEquals("Guide Set was not added correctly", 1, table.getDataRowCount());
    }

    private void addCustomMetric(String projectName, String metricName, String schema1Name, String series1Query)
    {
        goToProjectHome(projectName);
        Map<ConfigureMetricsUIPage.CustomMetricProperties, String> metricProperties = new LinkedHashMap<>();
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricName, metricName);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.series1Schema, schema1Name);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.series1Query, series1Query);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.series1AxisLabel, metricName);
        metricProperties.put(ConfigureMetricsUIPage.CustomMetricProperties.metricType, ConfigureMetricsUIPage.MetricType.Precursor.name());

        ConfigureMetricsUIPage configureUI = goToConfigureMetricsUI();
        configureUI.addNewCustomMetric(metricProperties);
    }

    private void addAnnotationType(String projectName, String name)
    {
        log("Adding QC Annotation type");
        goToProjectHome(projectName);
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("targetedms", "QCAnnotationType");
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), name);
        Locator.tagWithClassContaining("a", "color-3366FF").findElement(getDriver()).click();
        clickButton("Submit");

        table = new DataRegionTable("query", getDriver());
        Assert.assertEquals("QC annotation type was not added correctly", 5, table.getDataRowCount());
    }

    private void excludePrecursors(String projectName, int rowNum)
    {
        goToProjectHome(projectName);
        QCSummaryWebPart qcSummaryWebPart = goToDashboard().getQcSummaryWebPart();
        qcSummaryWebPart.clickMenuItem("Configure Included and Excluded Precursors");
        DataRegionTable table = new DataRegionTable.DataRegionFinder(getDriver()).waitFor();
        table.checkCheckbox(rowNum);
        table.clickHeaderButton("Mark As Excluded");
    }
}
