-- This query for Molecules displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
mol.peptideGroupId.Label,
-- mol.PeptideGroupId.RunId.File,
molPrec.IonFormula,
molPrec.customIonName,
molPrec.charge,
molPrec.mz,
molPrec.massAverage,
molPrec.massMonoisotopic
FROM
    targetedms.Molecule mol
LEFT JOIN
    targetedms.MoleculePrecursor molPrec
ON
    mol.IonFormula = molPrec.moleculeId.ionFormula

GROUP BY
    mol.peptideGroupId.Label,
-- mol.PeptideGroupId.RunId.File,
    molPrec.IonFormula,
    molPrec.customIonName,
    molPrec.charge,
    molPrec.mz,
    molPrec.massAverage,
    molPrec.massMonoisotopic