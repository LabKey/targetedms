-- This query for Precursor Peptides displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
*,
(CASE
    WHEN precPep.precursorIdentifier IN (SELECT precursorIdentifier FROM targetedms.ExcludedPrecursors) THEN 'Excluded'
    ELSE 'Included'
END) AS markedAs,
FROM
     (SELECT
        (pep.PeptideModifiedSequence || '-' || cast(prec.mz AS VARCHAR)) AS precursorIdentifier, -- "key field" required for requireSelection to work
        pep.PeptideModifiedSequence AS peptideSequence,
        -- pep.PeptideGroupId.RunId.File,
        pep.PeptideGroupId.Label,
        prec.charge,
        prec.mz,
        prec.neutralMass,
        prec.IsotopeLabelId.Name
        FROM
        targetedms.Peptide pep
        LEFT JOIN
        targetedms.Precursor prec
        ON
        pep.PeptideModifiedSequence = prec.PeptideId.modifiedPeptideDisplayColumn

        GROUP BY
            pep.PeptideModifiedSequence,
        --     pep.PeptideGroupId.RunId.File,
            pep.PeptideGroupId.Label,
            prec.charge,
            prec.mz,
            prec.neutralMass,
            prec.IsotopeLabelId.Name
     ) precPep
