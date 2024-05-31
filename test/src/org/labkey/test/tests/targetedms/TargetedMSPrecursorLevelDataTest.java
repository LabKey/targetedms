/*
 * Copyright (c) 2021 LabKey Corporation
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

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.ModulePropertyValue;

import java.util.Arrays;

@Category({})
public class TargetedMSPrecursorLevelDataTest extends AbstractQuantificationTest
{
    /**
     * The method {@link #setUsePrecursorLevelData} cannot handle special characters in the project name,
     * so we use a simple project name for these tests.
     */
    @Override
    protected String getProjectName()
    {
        return "TargetedMSProject";
    }
    public void setUsePrecursorLevelData() {
        setStorageLimitModuleProperties("0", "0");
    }

    @Test
    public void testUsingPrecursorLevelData() throws Exception
    {
        setUsePrecursorLevelData();
        FiguresOfMerit fom = new FiguresOfMerit("VIFDANAPVAVR");
        fom.setLoq("1.0");
        fom.setUloq("10.0");
        fom.setBiasLimit("20.0%");
        fom.setCvLimit("20.0%");
        fom.setLod("0.11");
        fom.setCalc("Blank plus 2 * SD");

        runScenario("MergedDocuments", "none", "linear", fom);
    }

    @After
    public void setDefaultStorageLimits()
    {
        setStorageLimitModuleProperties("", "");
    }
}
