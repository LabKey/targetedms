package org.labkey.test.tests.targetedms;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.targetedms.QCPlotsWebPart;
import org.labkey.test.pages.targetedms.PanoramaDashboard;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@Category({})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class TargetedMSSmallMoleculeLightHeavyRatioTest extends TargetedMSTest
{
    private static final String SKY_FILE = "Acylcarnitines_Template_Plasma.sky.zip";

    @BeforeClass
    public static void initProject()
    {
        TargetedMSSmallMoleculeLightHeavyRatioTest init = getCurrentTest();
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
        importData(SKY_FILE);
    }

    @Test
    public void testLightHeavyRatio() throws IOException, CommandException
    {
        goToProjectHome();
        PanoramaDashboard qcDashboard = new PanoramaDashboard(this);
        QCPlotsWebPart qcPlotsWebPart = qcDashboard.getQcPlotsWebPart();
        // Be sure that we get a light/heavy ratio metric shown
        qcPlotsWebPart.setMetric1Type(QCPlotsWebPart.MetricType.LHRATIO);
        assertTextPresent("Acetylcarnitine (C2)", "Tetradecanoylcarnitine (C14)");

        // Spot check calculated values. Ordering should be stable based on the Skyline document's ordering
        Connection connection = createDefaultConnection();
        SelectRowsCommand transitionCommand = new SelectRowsCommand("targetedms", "transitionarearatio");
        SelectRowsResponse transitionResponse = transitionCommand.execute(connection, getProjectName());
        assertEquals("Wrong number of rows", 48, transitionResponse.getRows().size());
        assertEquals("Wrong ratio for first row", 11.234128, (Double)transitionResponse.getRows().get(0).get("arearatio"), 0.0001);

        SelectRowsCommand precursorCommand = new SelectRowsCommand("targetedms", "precursorarearatio");
        SelectRowsResponse precursorResponse = precursorCommand.execute(connection, getProjectName());
        assertEquals("Wrong number of rows", 24, precursorResponse.getRows().size());
        assertEquals("Wrong ratio for first row", 12.301311, (Double)precursorResponse.getRows().get(0).get("arearatio"), 0.0001);
    }
}
