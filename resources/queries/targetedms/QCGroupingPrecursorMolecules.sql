-- This query for Molecules displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
    *,
    (CASE
         WHEN molPrec.precursorIdentifier IN (SELECT precursorIdentifier FROM targetedms.ExcludedPrecursors) THEN 'Excluded'
         ELSE 'Included'
        END) AS markedAs,
FROM
     (SELECT
        ((CASE WHEN molPrec.MoleculeId.customIonName IS NOT NULL AND molPrec.MoleculeId.ionFormula IS NOT NULL
               THEN (molPrec.MoleculeId.customIonName || ',' || molPrec.MoleculeId.ionFormula)
          CASE WHEN molPrec.MoleculeId.customIonName IS NOT NULL THEN molPrec.MoleculeId.customIonName
          CASE WHEN molPrec.MoleculeId.ionFormula IS NOT NULL THEN molPrec.MoleculeId.ionFormula
          END) || ',' ||
         cast(round(molPrec.massMonoisotopic, 3) AS VARCHAR) || ',' ||
         cast(round(molPrec.massAverage, 3) AS VARCHAR) ||','||
         cast(molPrec.charge AS VARCHAR) ||','||
         cast(round(molPrec.Mz, 3) AS VARCHAR)) AS precursorIdentifier, -- "key field" required for 'requireSelection' to work
        molPrec.GeneralMoleculeId.PeptideGroupId.Label,
        molPrec.MoleculeId.customIonName,
        molPrec.MoleculeId.ionFormula,
        molPrec.charge,
        molPrec.mz,
        molPrec.massMonoisotopic,
        molPrec.massAverage
        FROM
            targetedms.MoleculePrecursor molPrec

        GROUP BY
            molPrec.GeneralMoleculeId.PeptideGroupId.Label,
            molPrec.MoleculeId.IonFormula,
            molPrec.MoleculeId.customIonName,
            molPrec.charge,
            molPrec.mz,
            molPrec.massAverage,
            molPrec.massMonoisotopic) molPrec