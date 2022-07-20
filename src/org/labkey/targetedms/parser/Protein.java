package org.labkey.targetedms.parser;

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
}
