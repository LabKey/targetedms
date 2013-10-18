/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: vsharma
 * Date: 4/24/12
 * Time: 1:09 PM
 */
class PeptideSettingsParser
{
    private static final String PEPTIDE_SETTINGS = "peptide_settings";
    private static final String ENZYME = "enzyme";
    private static final String DIGEST_SETTINGS = "digest_settings";
    private static final String PEPTIDE_PREDICTION = "peptide_prediction";
    private static final String PEPTIDE_LIBRARIES = "peptide_libraries";
    private static final String PEPTIDE_MODIFICATIONS = "peptide_modifications";
    private static final String INTERNAL_STANDARD = "internal_standard";
    private static final String STATIC_MODIFICATIONS = "static_modifications";
    private static final String STATIC_MODIFICATION = "static_modification";
    private static final String POTENTIAL_LOSS = "potential_loss";
    private static final String HEAVY_MODIFICATIONS = "heavy_modifications";
    private static final String AMINOACID = "aminoacid";
    private static final String TERMINUS = "terminus";
    private static final String FORMULA = "formula";
    private static final String MASSDIFF_MONOISOTOPIC = "massdiff_monoisotopic";
    private static final String MASSDIFF_AVERAGE = "massdiff_average";
    private static final String EXPLICIT_DECL = "explicit_decl";
    private static final String UNIMOD_ID = "unimod_id";
    private static final String NAME = "name";
    private static final String PICK = "pick";
    private static final String RANK_TYPE = "rank_type";
    private static final String PEPTIDE_COUNT = "peptide_count";
    private static final String BIBLIOSPEC_LITE_LIB = "bibliospec_lite_library";
    private static final String BIBLIOSPEC_LIB = "bibliospec_library";
    private static final String HUNTER_LIB = "hunter_library";
    private static final String NIST_LIB = "nist_library";
    private static final String SPECTRAST_LIB = "spectrast_library";
    private static final String FILE_NAME_HINT = "file_name_hint";
    private static final String LSID = "lsid";
    private static final String ID = "id";
    private static final String REVISION = "revision";
    private static final String DOCUMENT_LIBRARY = "document_library";

    private String _documentName;

    public PeptideSettings parse(XMLStreamReader reader, String documentName) throws XMLStreamException
    {
        _documentName = documentName;

        PeptideSettings settings = new PeptideSettings();

        while(reader.hasNext())
         {
             int evtType = reader.next();
             if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_SETTINGS))
             {
                 break;
             }

             if(XmlUtil.isStartElement(reader, evtType, ENZYME))
             {
                 settings.setEnzyme(readEnzyme(reader));
             }
             else if(XmlUtil.isStartElement(reader, evtType, DIGEST_SETTINGS))
             {
                 settings.setDigestSettings(readDigestSettings(reader));
             }
             else if(XmlUtil.isStartElement(reader, evtType, PEPTIDE_PREDICTION))
             {
                 // TODO: read peptide prediction settings
             }
             else if(XmlUtil.isStartElement(reader, evtType, PEPTIDE_LIBRARIES))
             {
                 settings.setLibrarySettings(readLibrarySettings(reader));
             }
              else if(XmlUtil.isStartElement(reader, evtType, PEPTIDE_MODIFICATIONS))
             {
                 settings.setModifications(readModifications(reader));
             }
         }

        return settings;
    }

    private PeptideSettings.EnzymeDigestionSettings readDigestSettings(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.EnzymeDigestionSettings result = new PeptideSettings.EnzymeDigestionSettings();
        result.setMaxMissedCleavages(XmlUtil.readIntegerAttribute(reader, "max_missed_cleavages"));
        result.setExcludeRaggedEnds(XmlUtil.readBooleanAttribute(reader, "exclude_ragged_ends"));
        return result;
    }

    private PeptideSettings.Enzyme readEnzyme(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.Enzyme enzyme = new PeptideSettings.Enzyme();
        enzyme.setName(XmlUtil.readRequiredAttribute(reader, NAME, ENZYME));
        enzyme.setCut(XmlUtil.readRequiredAttribute(reader, "cut", ENZYME));
        enzyme.setNoCut(reader.getAttributeValue(null, "no_cut"));
        enzyme.setSense(XmlUtil.readRequiredAttribute(reader, "sense", ENZYME));
        return enzyme;
    }

    private PeptideSettings.PeptideModifications readModifications(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.PeptideModifications modifications = new PeptideSettings.PeptideModifications();

        PeptideSettings.ModificationSettings settings = new  PeptideSettings.ModificationSettings();
        Integer maxVariableMods = XmlUtil.readIntegerAttribute(reader, "max_variable_mods");
        if (maxVariableMods != null)
        {
            settings.setMaxVariableMods(maxVariableMods.intValue());
        }
        Integer maxNeutralLosses = XmlUtil.readIntegerAttribute(reader, "max_neutral_losses");
        if (maxNeutralLosses != null)
        {
            settings.setMaxNeutralLosses(maxNeutralLosses.intValue());
        }
        modifications.setModificationSettings(settings);

        // If there is a single internal standard it is written out as an attribute.
        // Otherwise, there is one <internal_standard> element for each standard
        String inernalStandard = reader.getAttributeValue(null, INTERNAL_STANDARD);
        Set<String> internalStandards = new HashSet<>();

        List<PeptideSettings.RunStructuralModification> staticMods = new ArrayList<>();
        List<PeptideSettings.RunIsotopeModification> isotopeMods = new ArrayList<>();
        modifications.setStructuralModifications(staticMods);
        modifications.setIsotopeModifications(isotopeMods);

        if(null != inernalStandard)
        {
            internalStandards.add(inernalStandard);
        }

        List<String> isotopeLabelNames  = new ArrayList<>();

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_MODIFICATIONS))
            {
                break;
            }

            if(XmlUtil.isStartElement(reader, evtType, INTERNAL_STANDARD))
            {
                internalStandards.add(XmlUtil.readRequiredAttribute(reader, NAME, INTERNAL_STANDARD));
            }
            else if(XmlUtil.isStartElement(reader, evtType, STATIC_MODIFICATIONS))
            {
                staticMods.addAll(readStaticModifications(reader));
            }
            else if(XmlUtil.isStartElement(reader, evtType, HEAVY_MODIFICATIONS))
            {
                isotopeMods.addAll(readIsotopeModifications(reader, isotopeLabelNames));
            }
        }

        // Mark the label types that were used as an internal standard
        isotopeLabelNames.add(0, PeptideSettings.IsotopeLabel.LIGHT);

        // If we did not find either the "internal_standard" attribute or elements, check if we have
        // a "heavy" isotope label.  If we do, set "heavy" as the internal standard
        if(internalStandards.size() == 0 && isotopeLabelNames.contains(PeptideSettings.HEAVY_LABEL))
        {
            internalStandards.add(PeptideSettings.HEAVY_LABEL);
        }

        List<PeptideSettings.IsotopeLabel> labels = new ArrayList<>(isotopeLabelNames.size());
        for(String name: isotopeLabelNames)
        {
            PeptideSettings.IsotopeLabel isotopeLabel = new PeptideSettings.IsotopeLabel();
            isotopeLabel.setName(name);
            if(internalStandards.contains(name))
            {
                isotopeLabel.setStandard(true);
            }
            labels.add(isotopeLabel);
        }
        modifications.setIsotopeLabels(labels);
        return modifications;
    }

    private List<PeptideSettings.RunStructuralModification> readStaticModifications(XMLStreamReader reader) throws XMLStreamException
    {
        List<PeptideSettings.RunStructuralModification> modList = new ArrayList<>();

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, STATIC_MODIFICATIONS))
            {
                break;
            }

            if(XmlUtil.isStartElement(reader, evtType, STATIC_MODIFICATION))
            {
                modList.add(readStaticModification(reader));
            }
        }
        return modList;
    }
    
    private PeptideSettings.RunStructuralModification readStaticModification(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.RunStructuralModification mod = new PeptideSettings.RunStructuralModification();
        mod.setName(XmlUtil.readRequiredAttribute(reader, NAME, STATIC_MODIFICATION));
        mod.setAminoAcid(reader.getAttributeValue(null, AMINOACID));
        mod.setTerminus(reader.getAttributeValue(null, TERMINUS));
        mod.setVariable(XmlUtil.readBooleanAttribute(reader, "variable", false));
        mod.setFormula(reader.getAttributeValue(null, FORMULA));
        mod.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, MASSDIFF_MONOISOTOPIC));
        mod.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, MASSDIFF_AVERAGE));
        mod.setExplicitMod(XmlUtil.readBooleanAttribute(reader, EXPLICIT_DECL));
        mod.setUnimodId(XmlUtil.readIntegerAttribute(reader, UNIMOD_ID));

        List<PeptideSettings.PotentialLoss> potentialLosses = new ArrayList<>();
        mod.setPotentialLosses(potentialLosses);

        while (reader.hasNext())
        {
            int evtType = reader.next();

            if (XmlUtil.isEndElement(reader, evtType, STATIC_MODIFICATION))
            {
                break;
            }

            if (XmlUtil.isStartElement(reader, evtType, POTENTIAL_LOSS))
            {
                PeptideSettings.PotentialLoss potentialLoss = new PeptideSettings.PotentialLoss();
                potentialLoss.setFormula(reader.getAttributeValue(null, "formula"));
                potentialLoss.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, "massdiff_average"));
                potentialLoss.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, "massdiff_monoisotopic"));
                potentialLosses.add(potentialLoss);
            }
        }
        
        // TODO: read potential losses
        return mod;
    }

    private List<PeptideSettings.RunIsotopeModification> readIsotopeModifications(XMLStreamReader reader, List<String> isotopeLabelNames) throws XMLStreamException
    {
        String isotopeLabel = reader.getAttributeValue(null, "isotope_label");
        if(isotopeLabel == null)
        {
            isotopeLabel = PeptideSettings.HEAVY_LABEL;
        }

        List<PeptideSettings.RunIsotopeModification> modList = new ArrayList<>();

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, HEAVY_MODIFICATIONS))
            {
                break;
            }

            if(XmlUtil.isStartElement(reader, evtType, STATIC_MODIFICATION))
            {
                // These are the modifications associated with one isotope label
                // Example:
                // <heavy_modifications isotope_label="all 15N">
                // <static_modification name="all 15N" label_15N="true" />
                // </heavy_modifications>
                PeptideSettings.RunIsotopeModification mod = readIsotopeModification(reader);
                mod.setIsotopeLabel(isotopeLabel);
                modList.add(mod);
            }
        }

        isotopeLabelNames.add(isotopeLabel);
        return modList;
    }

     private PeptideSettings.RunIsotopeModification readIsotopeModification(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.RunIsotopeModification mod = new PeptideSettings.RunIsotopeModification();
        mod.setName(XmlUtil.readRequiredAttribute(reader, NAME, STATIC_MODIFICATION));
        mod.setAminoAcid(reader.getAttributeValue(null, AMINOACID));
        mod.setTerminus(reader.getAttributeValue(null, TERMINUS));
        mod.setFormula(reader.getAttributeValue(null, FORMULA));
        mod.setMassDiffMono(XmlUtil.readDoubleAttribute(reader, MASSDIFF_MONOISOTOPIC));
        mod.setMassDiffAvg(XmlUtil.readDoubleAttribute(reader, MASSDIFF_AVERAGE));
        mod.setExplicitMod(XmlUtil.readBooleanAttribute(reader, EXPLICIT_DECL));
        mod.setUnimodId(XmlUtil.readIntegerAttribute(reader, UNIMOD_ID));
        mod.setLabel13C(XmlUtil.readBooleanAttribute(reader, "label_13C"));
        mod.setLabel15N(XmlUtil.readBooleanAttribute(reader, "label_15N"));
        mod.setLabel18O(XmlUtil.readBooleanAttribute(reader, "label_18O"));
        mod.setLabel2H(XmlUtil.readBooleanAttribute(reader, "label_2H"));
        mod.setRelativeRt(reader.getAttributeValue(null, "relative_rt"));

        // TODO: read potential losses
        return mod;
    }

    private PeptideSettings.SpectrumLibrarySettings readLibrarySettings(XMLStreamReader reader) throws XMLStreamException
    {
        PeptideSettings.SpectrumLibrarySettings settings = new PeptideSettings.SpectrumLibrarySettings();
        settings.setPick(XmlUtil.readRequiredAttribute(reader, PICK, PEPTIDE_LIBRARIES));
        settings.setRankType(XmlUtil.readAttribute(reader, RANK_TYPE, null));
        settings.setPeptideCount(XmlUtil.readIntegerAttribute(reader, PEPTIDE_COUNT));

        List<PeptideSettings.SpectrumLibrary> libraryList = new ArrayList<>();
        settings.setLibraries(libraryList);

        boolean documentLibrary = XmlUtil.readBooleanAttribute(reader, DOCUMENT_LIBRARY, false);
        if(documentLibrary && _documentName != null)
        {
            // If there is a "document library" we will not have a separate library element
            // with the library name.  Document libraries have the same name as the .sky file.
            libraryList.add(getDocumentLibrary());
        }

        while(reader.hasNext())
        {
            int evtType = reader.next();
            if(XmlUtil.isEndElement(reader, evtType, PEPTIDE_LIBRARIES))
            {
                break;
            }

            if(XmlUtil.isStartElement(evtType))
            {
                if(XmlUtil.isElement(reader, BIBLIOSPEC_LIB) ||
                   XmlUtil.isElement(reader, BIBLIOSPEC_LITE_LIB) ||
                   XmlUtil.isElement(reader, HUNTER_LIB) ||
                   XmlUtil.isElement(reader, NIST_LIB) ||
                   XmlUtil.isElement(reader, SPECTRAST_LIB))
                {
                    libraryList.add(readLibrary(reader, reader.getLocalName()));
                }
            }
        }
        return  settings;
    }

    private PeptideSettings.SpectrumLibrary getDocumentLibrary()
    {
        PeptideSettings.SpectrumLibrary library = new PeptideSettings.SpectrumLibrary();
        library.setName(_documentName);
        library.setLibraryType(BIBLIOSPEC_LITE_LIB.substring(0, BIBLIOSPEC_LITE_LIB.indexOf("_library")));
        return library;
    }

    private PeptideSettings.SpectrumLibrary readLibrary(XMLStreamReader reader, String elementName) throws XMLStreamException
    {
        PeptideSettings.SpectrumLibrary library = new PeptideSettings.SpectrumLibrary();
        library.setName(XmlUtil.readRequiredAttribute(reader, NAME, elementName));
        library.setFileNameHint(XmlUtil.readAttribute(reader, FILE_NAME_HINT, null));
        library.setRevision(XmlUtil.readAttribute(reader, REVISION, null));
        library.setLibraryType(elementName.substring(0, elementName.indexOf("_library")));

        String skylineLibraryId;
        if(BIBLIOSPEC_LITE_LIB.equalsIgnoreCase(elementName))
        {
            skylineLibraryId = XmlUtil.readRequiredAttribute(reader, LSID, BIBLIOSPEC_LITE_LIB);
        }
        else
        {
            skylineLibraryId = XmlUtil.readAttribute(reader, ID, null);
        }
        // SpectrumLibrary.SkylineLibraryId is limited to 200 characters. Truncate longer ids for now since we don't use them anywhere.
        // TODO: Increase limit on this column.
        if(skylineLibraryId != null)
        {
            library.setSkylineLibraryId(skylineLibraryId.length() > 200 ? skylineLibraryId.substring(0, 200) : skylineLibraryId);
        }
        return library;
    }
}
