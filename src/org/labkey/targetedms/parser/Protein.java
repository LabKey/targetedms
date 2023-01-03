package org.labkey.targetedms.parser;

import org.labkey.api.util.Pair;

import java.util.Collections;
import java.util.List;

public class Protein extends AnnotatedEntity<ProteinAnnotation>
{
    private long _peptideGroupId;

    private String _label;
    private String _name;
    private String _description;
    private String _altDescription;


    private String _accession;
    private String _preferredName;
    private String _gene;
    private String _species;

    private String _sequence;
    private boolean _decoy;
    private Integer _sequenceId;

    private List<Pair<Integer, Integer>> _cdrRanges = Collections.emptyList();

    public Integer getSequenceId()
    {
        return _sequenceId;
    }

    public void setSequenceId(Integer sequenceId)
    {
        _sequenceId = sequenceId;
    }

    public String getSequence()
    {
        return _sequence;
    }

    public void setSequence(String sequence)
    {
        _sequence = sequence;
    }

    public boolean isDecoy()
    {
        return _decoy;
    }

    public void setDecoy(boolean decoy)
    {
        _decoy = decoy;
    }

    public String getAccession()
    {
        return _accession;
    }

    public void setAccession(String accession)
    {
        _accession = accession;
    }

    public String getPreferredName()
    {
        return _preferredName;
    }

    public void setPreferredName(String preferredName)
    {
        _preferredName = preferredName;
    }

    public String getGene()
    {
        return _gene;
    }

    public void setGene(String gene)
    {
        _gene = gene;
    }

    public String getSpecies()
    {
        return _species;
    }

    public void setSpecies(String species)
    {
        _species = species;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(String label)
    {
        _label = label;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getAltDescription()
    {
        return _altDescription;
    }

    public void setAltDescription(String altDescription)
    {
        _altDescription = altDescription;
    }

    public long getPeptideGroupId()
    {
        return _peptideGroupId;
    }

    public void setPeptideGroupId(long peptideGroupId)
    {
        _peptideGroupId = peptideGroupId;
    }

    public List<Pair<Integer, Integer>> getCdrRanges()
    {
        if (getPeptideGroupId() == 18519)
        {
            return List.of(Pair.of(26, 33), Pair.of(51, 57), Pair.of(96, 114));
        }
        if (getPeptideGroupId() == 18520)
        {
            return List.of(Pair.of(26, 33), Pair.of(51, 57), Pair.of(96, 116));
        }
        if (getPeptideGroupId() == 18521)
        {
            return List.of(Pair.of(27, 33), Pair.of(51, 53), Pair.of(90, 98));
        }
        return _cdrRanges;
    }

    public void setCdrRanges(List<Pair<Integer, Integer>> cdrRanges)
    {
        _cdrRanges = cdrRanges;
    }
}
