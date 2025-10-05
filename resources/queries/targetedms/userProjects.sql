SELECT
    p.id AS Id,
    p.title AS Title,
    p.submitDate AS SubmitDate,
    p.collaborationStatus AS CollaborationStatus
FROM msProject p WHERE p.id IN (SELECT project FROM projectresearcher pr WHERE pr.researcher = USERID())
