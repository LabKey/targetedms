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
CREATE TABLE targetedms.QCEnabledMetrics
(
  metric          INTEGER,
  enabled         BIT,
  lowerBound      DOUBLE PRECISION,
  upperBound      DOUBLE PRECISION,
  cusumLimit      DOUBLE PRECISION,

  Created         DATETIME,
  CreatedBy       USERID,
  Modified        DATETIME,
  ModifiedBy      USERID,
  Container       ENTITYID NOT NULL,

  CONSTRAINT PK_QCEnabledMetrics PRIMARY KEY (metric),
  CONSTRAINT FK_QCEnabledMetrics_Metric FOREIGN KEY (metric) REFERENCES targetedms.qcmetricconfiguration(Id),
  CONSTRAINT FK_QCEnabledMetrics_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
GO

