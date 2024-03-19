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
SELECT * FROM (
                  SELECT
                      pci.Id AS PrecursorChromInfoId,
                      SampleFileId AS SampleFileId,
                      -- Use the error from the most intense transition associated with the precursor
                      (SELECT COALESCE(MassErrorPPM, -1000) AS MetricValue
                       FROM TransitionChromInfo tci
                       WHERE TransitionId.Charge IS NOT NULL
                         AND tci.PrecursorChromInfoId = pci.Id
                         AND Area IS NOT NULL
                       ORDER BY Area DESC, Id LIMIT 1) AS MetricValue FROM PrecursorChromInfo pci
 ) X
WHERE MetricValue IS NOT NULL
