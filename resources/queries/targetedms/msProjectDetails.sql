SELECT
    p.Id,
    p.affiliation,
    p.title,
    p.collaborationWith,
    p.labDirector,
    p.organization,
    p.scientificQuestion,
    p.abstract,
    p.results,
    GROUP_CONCAT(pr.researcher.DisplayName, ', ') as researchers
FROM msProject p
LEFT JOIN projectResearcher pr ON p.Id = pr.project
GROUP BY p.Id, p.affiliation, p.title, p.collaborationWith, p.labDirector, p.organization, p.scientificQuestion, p.abstract, p.results
