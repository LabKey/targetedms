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
    (CASE WHEN exclPrec.markedAs = 'Excluded' THEN 'Excluded' ELSE 'Included' END) AS markedAs
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
    (SELECT *, 'Excluded' AS markedAs FROM targetedms.ExcludedPrecursors) exclPrec
    ON
    isequal(exclPrec.customIonName, mp.customIonName) AND
    isequal(exclPrec.ionFormula, mp.ionFormula) AND
    isequal(exclPrec.charge, mp.charge) AND
    isequal(round(exclPrec.mz, 4), round(mp.mz,4)) AND
    isequal(round(exclPrec.massMonoisotopic, 4), round(mp.massMonoisotopic, 4)) AND
    isequal(round(exclPrec.massAverage, 4), round(mp.massAverage, 4))