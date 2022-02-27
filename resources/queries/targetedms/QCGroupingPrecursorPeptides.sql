-- This query for Precursor Peptides displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
*,
(CASE
    WHEN precPep.precursorIdentifier IN (SELECT precursorIdentifier FROM targetedms.ExcludedPrecursors) THEN 'Excluded'
    ELSE 'Included'
END) AS markedAs
FROM
     (SELECT
        (prec.GeneralMoleculeId.PeptideModifiedSequence || ',' || cast(prec.charge AS VARCHAR) || ',' || cast(round(prec.mz, 3) AS VARCHAR)) AS precursorIdentifier, -- "key field" required for 'requireSelection' to work
        prec.GeneralMoleculeId.PeptideModifiedSequence AS peptideSequence,
        prec.GeneralMoleculeId.PeptideGroupId.Label,
        prec.charge,
        prec.mz,
        prec.neutralMass,
        prec.IsotopeLabelId.Name
        FROM
        targetedms.Precursor prec

        GROUP BY
            prec.GeneralMoleculeId.PeptideModifiedSequence,
            prec.GeneralMoleculeId.PeptideGroupId.Label,
            prec.charge,
            prec.mz,
            prec.neutralMass,
            prec.IsotopeLabelId.Name
     ) precPep
