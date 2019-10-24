/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.GetQueriesCommand;
import org.labkey.remoteapi.query.GetQueriesResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, MS2.class})
public class TargetedMSListTest extends TargetedMSTest
{
    private static final String SKY_FILE = "ListTest.sky.zip";

    @Test
    public void testSteps() throws IOException, CommandException
    {
        setupFolder(FolderType.Experiment);
        importData(SKY_FILE);

        clickAndWait(Locator.linkContainingText("Panorama Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        verifyRunSummaryCountsPep(2,4,0, 5,53, 1, 0, 6);
        clickAndWait(Locator.linkContainingText("6 lists"));
        assertTextPresent("DocumentProperties", "Lorem Ipsum", "Protein Descriptions");
        clickAndWait(Locator.linkContainingText("Protein Descriptions"));

        // Check that the document header remains
        verifyRunSummaryCountsPep(2,4,0, 5,53, 1, 0, 6);
        assertTextPresent("ALBU_BOVIN", "main protein of plasma");

        // Be sure data is exposed through query too
        GetQueriesCommand command = new GetQueriesCommand("targetedmslists");
        GetQueriesResponse queriesResponse = command.execute(createDefaultConnection(false), getCurrentContainerPath());
        assertEquals("Wrong number of queries", 6, queriesResponse.getQueryNames().size());
        Set<String> trimmedNames = queriesResponse.getQueryNames().stream().map((s) -> s.substring(s.indexOf("_") + 1)).collect(Collectors.toSet());
        assertTrue("Missing 'DocumentProperties'", trimmedNames.contains("DocumentProperties"));
        assertTrue("Missing 'Numbers'", trimmedNames.contains("Numbers"));

        Optional<String> samplesName = queriesResponse.getQueryNames().stream().filter((s) -> s.endsWith("_Samples")).findFirst();
        assertTrue("Missing '*_Samples' from: " + queriesResponse.getQueryNames(), samplesName.isPresent());

        SelectRowsCommand rowsCommand = new SelectRowsCommand("targetedmslists", samplesName.get());
        rowsCommand.setRequiredVersion(9.1);
        SelectRowsResponse rowsResponse = rowsCommand.execute(createDefaultConnection(false), getCurrentContainerPath());
        assertEquals("Wrong number of rows in 'Samples'", 5, rowsResponse.getRows().size());

        Set<String> sampleNames = new HashSet<>();
        rowsResponse.getRowset().forEach((r) -> sampleNames.add((String)r.getValue("SampleName")));
        assertEquals("Wrong sample names", Set.of("Mickey", "Minnie", "Mighty", "Jerry", "Speedy"), sampleNames);
    }
}
