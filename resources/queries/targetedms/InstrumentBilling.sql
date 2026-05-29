/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
SELECT
    Project.Id AS ProjectID,
    Project.LabDirector AS LabDirector,
    InstrumentOperator.DisplayName AS Researcher,
    i.Instrument.Name AS Instrument,
    Name AS RequestedBy,
    i.Id AS UsageBlockID,
    StartTime AS StartDate,
    EndTime AS EndDate,
    TIMESTAMPDIFF('SQL_TSI_HOUR', StartTime, EndTime) AS Hours,
    ir.Fee,
    ir.rateType.setupFee AS Setup_Cost,
    TIMESTAMPDIFF('SQL_TSI_HOUR', StartTime, EndTime) * ir.Fee + ir.rateType.setupFee AS TotalCost,
    iup.PaymentMethod.UWBudgetNumber AS Payment_Method,
    iup.PaymentMethod.Name AS Payment_Method_Name,
    iup.PercentPayment,
    ((TIMESTAMPDIFF('SQL_TSI_HOUR', StartTime, EndTime) * Fee + ir.rateType.setupFee) * PercentPayment / 100) AS AmountBilled

FROM targetedms.InstrumentSchedule i
INNER JOIN targetedms.InstrumentUsagePayment iup ON i.Id = iup.InstrumentScheduleId
INNER JOIN targetedms.InstrumentRate ir ON i.Instrument = ir.Instrument AND iup.PaymentMethod.RateType = ir.rateType