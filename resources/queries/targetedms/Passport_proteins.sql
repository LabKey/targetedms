SELECT
    sequenceid,
  min(sequenceid.description) AS name,
  min(accession) as accession,
  count(DISTINCT pep.Id) AS peptides,
  min(label) as label,
  min(preferredname) as PreferredName,
  min(gene) as gene,
  min(species) as species,
  min(sequenceid.length) AS length,
  min(p.peptideGroupId.runId.created) as created,
  min(p.peptideGroupId.runid) as runid,
  min(p.PeptideGroupId.id) AS proteinId @hidden

FROM protein p
LEFT OUTER JOIN peptide pep ON p.peptidegroupid = pep.peptidegroupid AND pep.standardtype IS NOT NULL
WHERE sequenceid IS NOT NULL AND pep.id IS NULL
GROUP BY sequenceid