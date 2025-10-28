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

SELECT
       qmc.id,
       qmc.name,
       qmc.QueryName,
       qmc.PrecursorScoped,
       qmc.Container, -- including to lock out editing pre-configured qc metrics,
       qem.Status,
       CASE WHEN qem.metric IS NULL THEN FALSE
            ELSE TRUE END AS Inserted,
       COALESCE(qem.lowerBound, CASE WHEN qem.Status IS NULL OR qem.Status = 'LeveyJennings' THEN -3 END) AS lowerBound,
       COALESCE(qem.upperBound, CASE WHEN qem.Status IS NULL OR qem.Status = 'LeveyJennings' THEN 3 END) AS upperBound,
       qmc.TraceValue,
       qmc.MinTimeValue,
       qmc.MaxTimeValue,
       qmc.TimeValueOption,
       qmc.TraceName,
       qmc.YAxisLabel
FROM
      qcmetricconfiguration qmc
FULL JOIN   qcenabledmetrics qem
       ON   qem.metric=qmc.id