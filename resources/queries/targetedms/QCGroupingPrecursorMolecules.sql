-- This query for Molecules displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
    mp.Id,
    mp.Label,
    mp.customIonName,
    mp.ionFormula,
    mp.charge,
    mp.mz,
    mp.massMonoisotopic,
    mp.massAverage,
    (CASE WHEN exclPrec.RowId IS NULL THEN 'Included' ELSE 'Excluded' END) AS markedAs
FROM
     (SELECT
        min(molPrec.Id) AS Id,
        molPrec.GeneralMoleculeId.PeptideGroupId.Label AS Label,
        molPrec.customIonName AS customIonName,
        molPrec.ionFormula AS ionFormula,
        molPrec.charge AS charge,
        molPrec.mz AS mz,
        molPrec.massMonoisotopic AS massMonoisotopic,
        molPrec.massAverage AS massAverage
        FROM
            targetedms.MoleculePrecursor molPrec

        GROUP BY
            molPrec.GeneralMoleculeId.PeptideGroupId.Label,
            molPrec.IonFormula,
            molPrec.customIonName,
            molPrec.charge,
            molPrec.mz,
            molPrec.massAverage,
            molPrec.massMonoisotopic) mp
    LEFT JOIN
    targetedms.ExcludedPrecursors exclPrec
    ON
    isequal(exclPrec.customIonName, mp.customIonName) AND
    isequal(exclPrec.ionFormula, mp.ionFormula) AND
    isequal(exclPrec.charge, mp.charge) AND
    isequal(exclPrec.mz, mp.mz) AND
    isequal(exclPrec.massMonoisotopic, mp.massMonoisotopic) AND
    isequal(exclPrec.massAverage, mp.massAverage)