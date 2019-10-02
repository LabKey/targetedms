/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

 /**
 * Retrieve a list of containers a given entry belongs to. DocumentGUID should
 * always be the same since it shared for all versions of a document.
 */
PARAMETERS(ENTRY_ID INTEGER, CONTAINER_ID VARCHAR)
WITH logTree as (
  SELECT e.entryId, e.entryHash, e.parentEntryHash, e.versionId
  FROM targetedms.AuditLogEntry e
  WHERE entryId = ENTRY_ID
  UNION ALL
  SELECT e.entryId, e.entryHash, e.parentEntryHash, e.versionId
  FROM logTree
         JOIN targetedms.AuditLogEntry e
              ON logTree.entryHash = e.parentEntryHash
)
SELECT logTree.*,
       r.Container,
       r.DocumentGUID
  FROM logTree
  JOIN targetedms.Runs r
    ON logTree.versionId = r.Id
 WHERE r.Container = CONTAINER_ID

