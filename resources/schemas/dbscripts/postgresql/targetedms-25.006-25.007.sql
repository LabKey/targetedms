-- Don't allow duplicate project/researcher mapping rows
DELETE FROM targetedms.projectresearcher WHERE Id NOT IN (SELECT MIN(Id) FROM targetedms.projectresearcher GROUP BY project, researcher);

ALTER TABLE targetedms.projectResearcher
    ADD CONSTRAINT UQ_projectResearcher_project_researcher UNIQUE (project, researcher);

DROP INDEX targetedms.IDX_projectResearcher_Project;
