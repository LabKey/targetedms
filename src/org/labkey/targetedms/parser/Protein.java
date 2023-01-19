package org.labkey.targetedms.parser;

import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.util.Pair;
import org.labkey.api.util.logging.LogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Protein extends AnnotatedEntity<ProteinAnnotation>
{
    private static final Logger LOG = LogHelper.getLogger(Protein.class, "Panorama protein parsing for CDR ranges");
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

    /** One-based indices for the complementarity-determining regions, specified by a PeptideGroup "CDR Range" annotation in the Skyline document */
    public List<Pair<Integer, Integer>> getCdrRangesList()
    {
        return _cdrRanges;
    }

    /** Parses out the start/end indices from a larger list like "[[26,33],[51,57],[96,114]]" */
    private static final Pattern CDR_ELEMENT = Pattern.compile("\\[(\\d+),(\\d+)]");

    public void setCdrRanges(String cdrRangesString)
    {
        List<Pair<Integer, Integer>> parsed = new ArrayList<>();
        if (cdrRangesString != null)
        {
            Matcher matcher = CDR_ELEMENT.matcher(cdrRangesString);
            while (matcher.find())
            {
                try
                {
                    int start = Integer.parseInt(matcher.group(1));
                    int end = Integer.parseInt(matcher.group(2));
                    parsed.add(Pair.of(start, end));
                }
                catch (NumberFormatException e)
                {
                    LOG.debug("Invalid protein CDR value: " + cdrRangesString);
                }
            }
        }
        _cdrRanges = Collections.unmodifiableList(parsed);
    }

    public void setCdrRangesList(List<Pair<Integer, Integer>> cdrRanges)
    {
        _cdrRanges = cdrRanges;
    }

    public static class TestCase
    {
        @Test
        public void testCDRParse()
        {
            Protein p1 = new Protein();

            Assert.assertEquals("Wrong CDR defaults", Collections.emptyList(), p1.getCdrRangesList());

            p1.setCdrRanges(null);
            Assert.assertEquals("Wrong CDR defaults", Collections.emptyList(), p1.getCdrRangesList());

            p1.setCdrRanges("");
            Assert.assertEquals("Wrong CDR defaults", Collections.emptyList(), p1.getCdrRangesList());

            p1.setCdrRanges("[[26,33],[51,57],[96,114]]");
            Assert.assertEquals("Wrong CDR parsing", List.of(Pair.of(26, 33), Pair.of(51, 57), Pair.of(96, 114)),
                    p1.getCdrRangesList());

            p1.setCdrRanges("[[26,33]");
            Assert.assertEquals("Wrong CDR parsing", List.of(Pair.of(26, 33)),
                    p1.getCdrRangesList());
        }
    }
}
