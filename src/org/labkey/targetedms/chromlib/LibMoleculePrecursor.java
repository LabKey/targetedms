package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.PrecursorChromInfo;

import java.util.Map;

public class LibMoleculePrecursor extends AbstractLibPrecursor<LibMoleculeTransition>
{
    private long _moleculeId;

    private Double _massMonoisotopic;
    private Double _massAverage;
    private String _ionFormula;
    private String _customIonName;

    public LibMoleculePrecursor() {}

    public LibMoleculePrecursor(MoleculePrecursor p, Map<Long, String> isotopeLabelMap, PrecursorChromInfo chromInfo,
                                TargetedMSRun run, Map<Long, Integer> sampleFileIdMap)
    {
        super(p, isotopeLabelMap, chromInfo, run, sampleFileIdMap);
        setIonFormula(p.getIonFormula());
        setCustomIonName(p.getCustomIonName());
        setMassMonoisotopic(p.getMassMonoisotopic());
        setMassAverage(p.getMassAverage());
    }

    public long getMoleculeId()
    {
        return _moleculeId;
    }

    public void setMoleculeId(long moleculeId)
    {
        _moleculeId = moleculeId;
    }

    public Double getMassMonoisotopic()
    {
        return _massMonoisotopic;
    }

    public void setMassMonoisotopic(Double massMonoisotopic)
    {
        _massMonoisotopic = massMonoisotopic;
    }

    public Double getMassAverage()
    {
        return _massAverage;
    }

    public void setMassAverage(Double massAverage)
    {
        _massAverage = massAverage;
    }

    public String getIonFormula()
    {
        return _ionFormula;
    }

    public void setIonFormula(String ionFormula)
    {
        _ionFormula = ionFormula;
    }

    public String getCustomIonName()
    {
        return _customIonName;
    }

    public void setCustomIonName(String customIonName)
    {
        _customIonName = customIonName;
    }
}
