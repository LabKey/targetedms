/*
 * Copyright (c) 2019 LabKey Corporation
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
PARAMETERS(StartBillDate TIMESTAMP)
SELECT
    ProjectID,
    LabDirector,
    Researcher,
    Instrument,
    RequestedBy,
    UsageBlockID,
    StartDate,
    EndDate,
    Hours,
    HoursInRange,
    Fee,
    Setup_Cost,
    TotalCost,
    Payment_Method,
    Payment_Method_Name,
    PercentPayment,
    -- Only include the setup fee when the beginning of the reservation falls within the report's time window
    ((HoursInRange * Fee + (CASE WHEN StartDate <= StartBillDate THEN 0 ELSE Setup_Cost END)) * PercentPayment / 100) AS AmountBilled

FROM
    (SELECT
        *,
        TIMESTAMPDIFF('SQL_TSI_HOUR',
                      CASE WHEN StartDate <= StartBillDate THEN StartBillDate ELSE StartDate END,
                      CASE WHEN EndDate >= TIMESTAMPADD('SQL_TSI_MONTH', 1, StartBillDate) THEN TIMESTAMPADD('SQL_TSI_MONTH', 1, StartBillDate) ELSE EndDate END
        ) AS HoursInRange

    FROM targetedms.InstrumentBilling i
    WHERE (StartDate >= StartBillDate AND StartDate <= TIMESTAMPADD('SQL_TSI_MONTH', 1, StartBillDate)) OR
          (EndDate >= StartBillDate AND EndDate <= TIMESTAMPADD('SQL_TSI_MONTH', 1, StartBillDate)) OR
          (StartDate <= StartBillDate AND EndDate >= TIMESTAMPADD('SQL_TSI_MONTH', 1, StartBillDate))
    ) X