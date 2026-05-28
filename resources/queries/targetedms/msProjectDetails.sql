/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    p.Id,
    p.affiliation,
    p.title,
    p.collaborationWith,
    p.labDirector,
    p.scientificQuestion,
    p.abstract,
    GROUP_CONCAT(pr.researcher.DisplayName, ', ') as researchers
FROM msProject p
LEFT JOIN projectResearcher pr ON p.Id = pr.project
GROUP BY p.Id, p.affiliation, p.title, p.collaborationWith, p.labDirector, p.scientificQuestion, p.abstract
