/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Don't allow duplicate project/researcher mapping rows
DELETE FROM targetedms.projectresearcher WHERE Id NOT IN (SELECT MIN(Id) FROM targetedms.projectresearcher GROUP BY project, researcher);

ALTER TABLE targetedms.projectResearcher
    ADD CONSTRAINT UQ_projectResearcher_project_researcher UNIQUE (project, researcher);

DROP INDEX targetedms.IDX_projectResearcher_Project;
