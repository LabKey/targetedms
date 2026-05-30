/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;

import java.util.List;

/**
 * Validates behavior at chromatogram data storage limits for transitions and precursors, with custom configuration
 * overrides.
 */
@Category({})
public class TargetedMSTransitionChromInfoLimitTest extends TargetedMSTest
{
    private int _jobCount;
    private final String SKY_DOC = "QC_1.sky.zip";

    // QC_1-compact.sky.zip is the same as QC_1.sky.zip but with data for all transitions of a precursor contained
    // in a <transition_data> element in compressed format.
    private final String SKY_DOC_COMPACT = "QC_1-compact.sky.zip";
    @BeforeClass
    public static void setupProject()
    {
        TargetedMSTransitionChromInfoLimitTest init = getCurrentTest();
        init.setupFolder(FolderType.Experiment);
    }

    @Test
    public void testWithDefaultLimits()
    {
        setStorageLimitModuleProperties("", ""); // Default limits

        setupSubfolder(getProjectName(), "DefaultSettings", FolderType.Experiment);
        importData(SKY_DOC, ++_jobCount);
        importData(SKY_DOC_COMPACT, ++_jobCount);
        verifyDataForRun(SKY_DOC, 18, 6);
        verifyDataForRun(SKY_DOC_COMPACT, 18, 6);
    }

    @Test
    public void testWithCustomLimits()
    {
        // Set "TransitionChromInfo storage limit" and "Precursor storage limit" to small values so that the
        // TransitionChromInfos from the documents are not saved.
        setStorageLimitModuleProperties("18", "2");

        setupSubfolder(getProjectName(), "CustomSettings", FolderType.Experiment);
        importData("QC_1.sky.zip", ++_jobCount);
        importData("QC_1-compact.sky.zip", ++_jobCount);
        verifyDataForRun(SKY_DOC, 0, 6);
        verifyDataForRun(SKY_DOC_COMPACT, 0, 6);
    }

    private void verifyDataForRun(String file, int transitionChromInfoCount, int precursorChromInfoCount)
    {
        Filter fileFilter = new Filter("SampleFileId/ReplicateId/RunId/FileName", file, Filter.Operator.EQUAL);

        int tciCount = executeSelectRowCommand("targetedms", "transitionchrominfo", ContainerFilter.Current,
                getCurrentContainerPath(), List.of(fileFilter), List.of("Id")).getRows().size();
        Assert.assertEquals("Wrong number of transitionChromInfos for the file " + file, transitionChromInfoCount,
                tciCount);

        int pciCount = executeSelectRowCommand("targetedms", "precursorchrominfo", ContainerFilter.Current,
                getCurrentContainerPath(), List.of(fileFilter), List.of("Id")).getRows().size();
        Assert.assertEquals("Wrong number of precursorChromInfos for the file " + file, precursorChromInfoCount, pciCount);

        if (transitionChromInfoCount == 0)
        {
            // If TransitionChromInfos were NOT stored, we expect the TransitionChromatogramIndices column values
            // of the PrecursorChromInfo table to be NON-NULL.
            Filter chromIndicesFilter = new Filter("TransitionChromatogramIndices", null, Filter.Operator.NONBLANK);
            int rowCount = executeSelectRowCommand("targetedms", "precursorchrominfo", ContainerFilter.Current,
                    getCurrentContainerPath(), List.of(fileFilter, chromIndicesFilter), List.of("Id")).getRows().size();
            Assert.assertEquals("Wrong number of precursorChromInfos with non-null TransitionChromatogramIndices for the file " + file,
                    precursorChromInfoCount, rowCount);
        }
        else
        {
            // If TransitionChromInfos were stored, we expect the TransitionChromatogramIndices column values
            // of the PrecursorChromInfo table to be NULL.
            Filter chromIndicesFilter = new Filter("TransitionChromatogramIndices", null, Filter.Operator.ISBLANK);
            int rowCount = executeSelectRowCommand("targetedms", "precursorchrominfo", ContainerFilter.Current,
                    getCurrentContainerPath(), List.of(fileFilter, chromIndicesFilter), List.of("Id")).getRows().size();
            Assert.assertEquals("Wrong number of precursorChromInfos with NULL TransitionChromatogramIndices for the file " + file,
                    precursorChromInfoCount, rowCount);
        }
    }

    @After
    public void setDefaultStorageLimits()
    {
        setStorageLimitModuleProperties("", "");
    }
}
