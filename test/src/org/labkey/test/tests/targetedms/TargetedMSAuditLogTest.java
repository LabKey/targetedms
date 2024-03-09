package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.DataRegionTable;

import static org.junit.Assert.assertEquals;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class TargetedMSAuditLogTest extends TargetedMSTest
{
    protected static final String AuditTrail_FILE = "AuditTrail.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSAuditLogTest init = (TargetedMSAuditLogTest) getCurrentTest();
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
        _userHelper.createUser(USER);
        importData(AuditTrail_FILE);
        startSystemMaintenance("Database");
        waitForSystemMaintenanceCompletion();
    }

    @Test
    public void testAuditLogImported()
    {
        log("Start of test");
        DataRegionTable auditLog = getAuditLogs(getProjectName());

        log("Verifying the imported logs");
        assertEquals("Invalid number of audit logs", 9, auditLog.getDataRowCount());
        assertEquals("Start message is incorrect", "Start of audit log for already existing document",
                auditLog.getDataAsText(0, "MessageText"));
        assertEquals("End message is incorrect", "Managed results",
                auditLog.getDataAsText(8, "MessageText"));
    }

    /*
        Test coverage for Issue 44507: Importing the same Skyline document in multiple containers associates the audit log solely with the last import
     */
    @Test
    public void testAuditLogInTwoDifferentFolders()
    {
        log("Creating folder and importing same SKY file");
        goToProjectHome();
        String FOLDER_2 = "Folder 2 with same audit logs";
        setUpFolder(FOLDER_2, FolderType.QC);
        importData(AuditTrail_FILE);

        log("Verifying logs are imported correctly in " + FOLDER_2);
        DataRegionTable auditLog = getAuditLogs(FOLDER_2);
        assertEquals("Invalid number of audit logs in " + FOLDER_2, 9, auditLog.getDataRowCount());

        log("Verifying logs are not affected in first folder after importing in second folder");
        auditLog = getAuditLogs(getProjectName());
        assertEquals("Invalid number of audit logs in " + getProjectName(), 9, auditLog.getDataRowCount());


        log("Deleting " + FOLDER_2);
        APIContainerHelper apiContainerHelper = new APIContainerHelper(this);
        apiContainerHelper.deleteProject(FOLDER_2, false);

        log("Verifying the logs are present in first folder");
        auditLog = getAuditLogs(getProjectName());
        assertEquals("Invalid number of audit logs", 9, auditLog.getDataRowCount());
    }

    private DataRegionTable getAuditLogs(String projectName)
    {
        goToProjectHome(projectName);
        clickTab("Runs");
        clickAndWait(Locator.linkContainingText(AuditTrail_FILE));

        log("Navigating to the audit log");
        clickAndWait(Locator.tagWithAttribute("a", "data-original-title", "Skyline Audit Log"));
        return new DataRegionTable("SkylineAuditLog", getDriver());
    }
}
