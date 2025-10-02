SELECT
    p.id AS Id,
    p.title AS Title,
    p.submitDate AS SubmitDate,
    p.collaborationStatus AS CollaborationStatus
FROM projectResearcher pr
LEFT JOIN msProject p ON pr.project = p.id
GROUP BY p.id, p.title, p.submitDate, p.collaborationStatus