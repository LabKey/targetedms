/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.targetedms;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.security.User;
import org.labkey.targetedms.parser.speclib.LibSpectrumReader;

/**
 * User: vsharma
 * Date: 8/22/2014
 * Time: 3:22 PM
 */
public class TargetedMSListener implements ContainerManager.ContainerListener
{
    @Override
    public void containerDeleted(Container c, User user)
    {
        // Delete any runs that might have failed to fully import and therefore won't have a wrapper experiment run.
        // See issue 34752
        TargetedMSManager.deleteIncludingExperimentWrapper(c, user);

        // Clean up QC annotations
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoQCAnnotation() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoQCAnnotationType() + " WHERE Container = ?", c);

        // Clean up Guide Sets
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoGuideSet() + " WHERE Container = ?", c);

        // Clean up any orphaned iRT scales
        TargetedMSManager.deleteiRTscales(c);

        // Clean up AutoQCPing
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoAutoQCPing() + " WHERE Container = ?", c);

        //Clean up QC enabled metrics
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoQCEnabledMetrics() + " WHERE Container = ?", c);

        // Clean up Metric Configurations
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoQCMetricConfiguration() + " WHERE Container = ?", c);

        //Clean up Excluded Precursors
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoExcludedPrecursors() + " WHERE Container = ?", c);

        LibSpectrumReader.clearLibCache(c);

        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoQCEmailNotifications() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoInstrumentNickname() + " WHERE Container = ?", c);

        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoInstrumentUsagePayment() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoInstrumentSchedule() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoProjectPaymentMethod() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoProjectResearcher() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoMSProject() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoInstrumentRate() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoMSInstrument() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoPaymentMethod() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoRateType() + " WHERE Container = ?", c);
        new SqlExecutor(TargetedMSManager.getSchema()).execute("DELETE FROM " + TargetedMSManager.getTableInfoSampleFileChromInfo() + " WHERE Container = ?", c);
    }
}
