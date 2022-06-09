package org.labkey.targetedms.view;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.targetedms.parser.Peptide;
import org.labkey.targetedms.parser.PeptideGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses cross-linked peptides based on Skyline's modified sequence format.
 * PKEPTIKDEA-PEKPTIDKEB-PEPKTIDEKC[+57.021464]-[+138.06808@2,3,*][+138.06808@*,8,4]
 *
 * <a href="https://skyline.ms/wiki/home/software/Skyline/download.view?entityId=ac4c6cc4-97c6-1038-8aae-e465a393ee52&name=NicholasShulman2020ASMSPoster.pdf">https://skyline.ms/wiki/home/software/Skyline/download.view?entityId=ac4c6cc4-97c6-1038-8aae-e465a393ee52&name=NicholasShulman2020ASMSPoster.pdf</a>
 */
public class CrossLinkedPeptideInfo
{
    private static final Pattern FULL_PATTERN = Pattern.compile("((.+-)+)(\\[(.+@.+)]+)");

    private final List<String> _unmodifiedPeptides = new ArrayList<>();
    private final List<String> _modifiedPeptides = new ArrayList<>();

    private final List<Linker> _linkers = new ArrayList<>();

    public CrossLinkedPeptideInfo(String modifiedSequence)
    {
        Matcher crossLinkMatcher = FULL_PATTERN.matcher(modifiedSequence);

        if (crossLinkMatcher.matches())
        {
            // Work with the peptide and linking sections separately
            String peptides = crossLinkMatcher.group(1);
            String crossLinks = crossLinkMatcher.group(3);

            boolean inModification = false;
            StringBuilder currentUnmodified = new StringBuilder();
            StringBuilder currentModified = new StringBuilder();
            for (int i = 0; i < peptides.length(); i++)
            {
                char c = peptides.charAt(i);
                if (c == '[')
                {
                    inModification = true;
                    currentModified.append(c);
                }
                else if (c == ']')
                {
                    inModification = false;
                    currentModified.append(c);
                }
                else if (!inModification)
                {
                    if (c == '-')
                    {
                        _unmodifiedPeptides.add(currentUnmodified.toString());
                        currentUnmodified = new StringBuilder();
                        _modifiedPeptides.add(currentModified.toString());
                        currentModified = new StringBuilder();
                    }
                    else
                    {
                        currentUnmodified.append(c);
                        currentModified.append(c);
                    }
                }
                else
                {
                    currentModified.append(c);
                }
            }

            Pattern linkPattern = Pattern.compile("\\[([^]]+@[^]]+?)]");
            Matcher linkMatcher = linkPattern.matcher(crossLinks);
            while (linkMatcher.find())
            {
                String fullLink = linkMatcher.group(1);
                Linker linker = new Linker(fullLink);
                _linkers.add(linker);
            }
        }
        else
        {
            _modifiedPeptides.add(modifiedSequence);
            _unmodifiedPeptides.add(Peptide.stripModifications(modifiedSequence, new ArrayList<>()));
        }
    }

    public boolean isCrosslinked()
    {
        return !_linkers.isEmpty();
    }

    public List<PeptideSequence> getExtraSequences()
    {
        return getSequences(1);
    }

    public List<PeptideSequence> getAllSequences()
    {
        return getSequences(0);
    }

    private List<PeptideSequence> getSequences(int startingIndex)
    {
        List<PeptideSequence> result = new ArrayList<>();
        for (int i = startingIndex; i < _unmodifiedPeptides.size(); i++)
        {
            result.add(new PeptideSequence(i));
        }
        return Collections.unmodifiableList(result);
    }

    public PeptideSequence getBaseSequence()
    {
        return new PeptideSequence(0);
    }

    public class PeptideSequence
    {
        private final int _index;

        public PeptideSequence(int index)
        {
            _index = index;
        }

        public String getModified()
        {
            return _modifiedPeptides.get(_index);
        }

        public String getUnmodified()
        {
            return _unmodifiedPeptides.get(_index);
        }

        /** Index is zero-based, though shown to the user as one-based */
        public boolean isCrossLinked(int index)
        {
            for (Linker linker : _linkers)
            {
                if (linker._linkIndices.get(_index).contains(index))
                {
                    return true;
                }
            }
            return false;
        }

        /** Index is zero-based, though shown to the user as one-based */
        public Set<Integer> getLinkIndices()
        {
            Set<Integer> result = new TreeSet<>();
            for (Linker linker : _linkers)
            {
                result.addAll(linker._linkIndices.get(_index));
            }
            return result;
        }

        @Nullable
        public PeptideGroup findMatch(List<PeptideGroup> proteins)
        {
            for (PeptideGroup protein : proteins)
            {
                String proteinSequence = protein.getSequence();
                if (proteinSequence != null && proteinSequence.contains(getUnmodified()))
                {
                    return protein;
                }
            }
            return null;
        }

        public int getPeptidIndex()
        {
            return _index;
        }
    }

    public static class TestCase
    {
        @Test
        public void testSingleCrossLink()
        {
            CrossLinkedPeptideInfo i = new CrossLinkedPeptideInfo("SCDK-GEC-[-2.01565@2,3]");
            Assert.assertEquals("Unmodified peptides", Arrays.asList("SCDK", "GEC"), i._unmodifiedPeptides);
            Assert.assertEquals("Modified peptides", Arrays.asList("SCDK", "GEC"), i._modifiedPeptides);
            Assert.assertEquals("Linker count", 1, i._linkers.size());
            Assert.assertEquals("Extra sequences", 1, i.getExtraSequences().size());
            Assert.assertEquals("Link location", Set.of(1), i.getBaseSequence().getLinkIndices());
            Assert.assertEquals("Link location", Set.of(2), i.getExtraSequences().get(0).getLinkIndices());
            Assert.assertEquals("Linker mass", -2.01565, i._linkers.get(0).getMass(), 0.00001);
        }

        @Test
        public void testModifiedAndMultiLinked()
        {
            CrossLinkedPeptideInfo i = new CrossLinkedPeptideInfo("SC[+57.0]DK-GE[-4.5]C-[-2.01565@2-3,1-2]");
            Assert.assertEquals("Unmodified peptides", Arrays.asList("SCDK", "GEC"), i._unmodifiedPeptides);
            Assert.assertEquals("Modified peptides", Arrays.asList("SC[+57.0]DK", "GE[-4.5]C"), i._modifiedPeptides);
            Assert.assertEquals("Linker count", 1, i._linkers.size());
            Assert.assertEquals("Link location", Set.of(1, 2), i.getBaseSequence().getLinkIndices());
            Assert.assertEquals("Link location", Set.of(0, 1), i.getExtraSequences().get(0).getLinkIndices());
        }

        @Test
        public void testTripleLink()
        {
            CrossLinkedPeptideInfo i = new CrossLinkedPeptideInfo("SCDK-GEC-AC[+4.5]KK[+100.5]R-[-2.01565@2,2,*][-4.555@*,3,4]");
            Assert.assertEquals("Unmodified peptides", Arrays.asList("SCDK", "GEC", "ACKKR"), i._unmodifiedPeptides);
            Assert.assertEquals("Modified peptides", Arrays.asList("SCDK", "GEC", "AC[+4.5]KK[+100.5]R"), i._modifiedPeptides);
            Assert.assertEquals("Linker count", 2, i._linkers.size());
            Assert.assertEquals("Link location", Set.of(1), i.getBaseSequence().getLinkIndices());
            Assert.assertEquals("Link location", Set.of(1, 2), i.getExtraSequences().get(0).getLinkIndices());
            Assert.assertEquals("Link location", Set.of(3), i.getExtraSequences().get(1).getLinkIndices());
            Assert.assertEquals("Linker mass", -2.01565, i._linkers.get(0).getMass(), 0.00001);
            Assert.assertEquals("Linker mass", -4.555, i._linkers.get(1).getMass(), 0.00001);
        }

        @Test
        public void testOmittedIndices()
        {
            CrossLinkedPeptideInfo i = new CrossLinkedPeptideInfo("CCVECPPCPAPPVAGPSVFLFPPKPK-GPSVFPLAPCSR-TVAPTECS-TVAPTECS-GPSVFPLAPCSR-CCVECPPCPAPPVAGPSVFLFPPKPK-[-2.01565@5,*,*,*,*,5][-2.01565@8,*,*,*,*,8][-2.01565@1,*,7][-2.01565@2,10][-2.01565@*,*,*,7,*,1][-2.01565@*,*,*,*,10,2]");
            Assert.assertEquals("Unmodified peptides", Arrays.asList("CCVECPPCPAPPVAGPSVFLFPPKPK", "GPSVFPLAPCSR", "TVAPTECS", "TVAPTECS", "GPSVFPLAPCSR", "CCVECPPCPAPPVAGPSVFLFPPKPK"), i._unmodifiedPeptides);
            Assert.assertEquals("Link location", Set.of(4, 7, 0, 1), i.getBaseSequence().getLinkIndices());
            Assert.assertEquals("Link location", Set.of(9), i.getExtraSequences().get(0).getLinkIndices());
            Assert.assertEquals("Link location", Set.of(4, 7, 0, 1), i.getExtraSequences().get(4).getLinkIndices());
        }
    }

    public class Linker
    {
        private final double _mass;

        private final List<Set<Integer>> _linkIndices = new ArrayList<>();

        /** Parses the linking info itself. Of the general form: [-2.01565@3,9] */
        public Linker(String linkInfo)
        {
            String[] parts = linkInfo.split("@");
            _mass = Double.parseDouble(parts[0]);

            String indexString = parts[1];
            String[] allIndices = indexString.split(",");

            // Parse the indices where the links are happening
            for (int i = 0; i < _unmodifiedPeptides.size(); i++)
            {
                String indices = i < allIndices.length ? allIndices[i] : null;
                Set<Integer> parsedIndices = new TreeSet<>();
                String[] peptideIndexStrings = indices == null ? new String[0] : indices.split("-");
                for (String index : peptideIndexStrings)
                {
                    // * means that the peptide isn't involved in the cross-linking
                    if (!"*".equals(index))
                    {
                        parsedIndices.add(Integer.parseInt(index) - 1);
                    }
                }
                _linkIndices.add(parsedIndices);
            }
        }

        public double getMass()
        {
            return _mass;
        }
    }

}
