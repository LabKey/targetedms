SELECT
    '[View]' AS ViewLink,
    p.id AS Id,
    p.title AS Title,
    p.type AS Type,
    p.submitDate AS SubmitDate,
    p.collaborationStatus AS CollaborationStatus,
    pr.researcher,
FROM projectResearcher pr
LEFT JOIN msProject p ON pr.project = p.id