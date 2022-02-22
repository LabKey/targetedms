-- This query for Molecules displays under QC Summary menu 'Include or Exclude Peptides/Molecules'

SELECT
    *,
    (CASE
         WHEN molPrec.precursorIdentifier IN (SELECT precursorIdentifier FROM targetedms.ExcludedPrecursors) THEN 'Excluded'
         ELSE 'Included'
        END) AS markedAs,
FROM
     (SELECT
        (molPrec.customIonName || ',' ||
         molPrec.IonFormula || ',' ||
         cast(molPrec.massMonoisotopic AS VARCHAR) || ',' ||
         cast(molPrec.massAverage AS VARCHAR) ||','||
         cast(molPrec.charge AS VARCHAR) ||','||
         cast(molPrec.mz AS VARCHAR)) AS precursorIdentifier, -- "key field" required for 'requireSelection' to work
        mol.peptideGroupId.Label,
        -- mol.PeptideGroupId.RunId.File,
        molPrec.customIonName,
        molPrec.IonFormula,
        molPrec.charge,
        molPrec.mz,
        molPrec.massMonoisotopic,
        molPrec.massAverage
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
            molPrec.massMonoisotopic) molPrec