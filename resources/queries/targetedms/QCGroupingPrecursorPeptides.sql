-- This query for Precursor Peptides displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
precPep.Id,
precPep.modifiedSequence,
precPep.Label,
precPep.charge,
precPep.mz,
precPep.neutralMass,
precPep.name,
(CASE WHEN exclPrec.RowId IS NULL THEN 'Included' ELSE 'Excluded' END) AS markedAs
FROM
     (SELECT
        min(prec.Id) AS Id,
        prec.modifiedSequence AS modifiedSequence,
        prec.GeneralMoleculeId.PeptideGroupId.Label AS Label,
        prec.charge AS charge,
        prec.mz AS mz,
        prec.neutralMass AS neutralMass,
        prec.IsotopeLabelId.Name AS name
        FROM
        targetedms.Precursor prec

        GROUP BY
            prec.modifiedSequence,
            prec.GeneralMoleculeId.PeptideGroupId.Label,
            prec.charge,
            prec.mz,
            prec.neutralMass,
            prec.IsotopeLabelId.Name
     ) precPep
     LEFT JOIN targetedms.ExcludedPrecursors exclPrec ON
         isequal(exclPrec.modifiedSequence, precPep.modifiedSequence) AND
         isequal(exclPrec.charge, precPep.charge) AND
         isequal(exclPrec.mz, precPep.mz)
