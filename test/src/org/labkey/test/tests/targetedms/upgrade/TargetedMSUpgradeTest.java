package org.labkey.test.tests.targetedms.upgrade;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.tests.targetedms.TargetedMSTest.FolderType;
import org.labkey.test.tests.upgrade.BaseUpgradeTest;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.targetedms.TargetedMSHelper;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Verifies that the targetedms-26.006-26.007 upgrade script correctly populates PeptideGroupCount,
 * MoleculeGroupCount, and ProteinCount on existing runs after the schema migration.
 */
@Category({})
public class TargetedMSUpgradeTest extends BaseUpgradeTest
{
    private static final String SKY_FILE = "smallmol_plus_peptides.sky.zip";

    public TargetedMSUpgradeTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected String getProjectName()
    {
        return "TargetedMS Upgrade Test";
    }

    @Override
    protected void doSetup() throws Exception
    {
        TargetedMSHelper helper = new TargetedMSHelper(this);
        helper.setupFolder(getProjectName(), FolderType.Experiment);
        helper.importData(SKY_FILE);
    }

    @Test
    public void testRunCounts() throws Exception
    {
        SelectRowsCommand cmd = new SelectRowsCommand("targetedms", "Runs");
        cmd.setColumns(List.of("PeptideGroups", "MoleculeLists", "Proteins"));
        SelectRowsResponse response = cmd.execute(createDefaultConnection(), getProjectName());

        List<Map<String, Object>> rows = response.getRows();
        assertEquals("Expected exactly one run", 1, rows.size());
        Map<String, Object> run = rows.get(0);
        assertEquals("PeptideGroupCount", 24, ((Number) run.get("PeptideGroups")).intValue());
        assertEquals("MoleculeGroupCount", 3, ((Number) run.get("MoleculeLists")).intValue());
        assertEquals("ProteinCount", 24, ((Number) run.get("Proteins")).intValue());
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("targetedms");
    }
}
