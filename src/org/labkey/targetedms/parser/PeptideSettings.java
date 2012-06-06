/*
 * Copyright (c) 2012 LabKey Corporation
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

import java.util.List;

/**
 * User: vsharma
 * Date: 4/24/12
 * Time: 12:02 PM
 */
public class PeptideSettings
{
    private Enzyme _enzyme;
    private PeptideModifications _modifications;
    private SpectrumLibrarySettings _librarySettings;
    private EnzymeDigestionSettings _enzymeDigestionSettings;

    public Enzyme getEnzyme()
    {
        return _enzyme;
    }

    public void setEnzyme(Enzyme enzyme)
    {
        _enzyme = enzyme;
    }

    public PeptideModifications getModifications()
    {
        return _modifications;
    }

    public void setModifications(PeptideModifications modifications)
    {
        _modifications = modifications;
    }

    public SpectrumLibrarySettings getLibrarySettings()
    {
        return _librarySettings;
    }

    public void setLibrarySettings(SpectrumLibrarySettings librarySettings)
    {
        _librarySettings = librarySettings;
    }

    public void setDigestSettings(EnzymeDigestionSettings enzymeDigestionSettings)
    {
        _enzymeDigestionSettings = enzymeDigestionSettings;
    }

    public EnzymeDigestionSettings getEnzymeDigestionSettings()
    {
        return _enzymeDigestionSettings;
    }

    // ------------------------------------------------------------------------
    // Isotope labels
    // ------------------------------------------------------------------------
    public static final class IsotopeLabel extends SkylineEntity
    {
        private int _runId;
        private String _name;
        private boolean _standard;

        public static final String LIGHT = "light";

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public boolean isStandard()
        {
            return _standard;
        }

        public void setStandard(boolean standard)
        {
            _standard = standard;
        }
    }
    // ------------------------------------------------------------------------
    // Modification settings
    // ------------------------------------------------------------------------
    public static final class PeptideModifications
    {
        private ModificationSettings _modificationSettings;
        private List<IsotopeLabel> _isotopeLabels;
        private List<RunIsotopeModification> _isotopeModifications;
        private List<RunStructuralModification> _structuralModifications;

        public ModificationSettings getModificationSettings()
        {
            return _modificationSettings;
        }

        public void setModificationSettings(ModificationSettings modificationSettings)
        {
            _modificationSettings = modificationSettings;
        }

        public List<RunIsotopeModification> getIsotopeModifications()
        {
            return _isotopeModifications;
        }

        public void setIsotopeModifications(List<RunIsotopeModification> isotopeModifications)
        {
            _isotopeModifications = isotopeModifications;
        }

        public List<RunStructuralModification> getStructuralModifications()
        {
            return _structuralModifications;
        }

        public void setStructuralModifications(List<RunStructuralModification> structuralModifications)
        {
            _structuralModifications = structuralModifications;
        }

        public List<IsotopeLabel> getIsotopeLabels()
        {
            return _isotopeLabels;
        }

        public void setIsotopeLabels(List<IsotopeLabel> isotopeLabels)
        {
            _isotopeLabels = isotopeLabels;
        }
    }

    public static final class ModificationSettings
    {
        private int _runId;
        private int _maxVariableMods;
        private int _maxNeutralLosses;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public int getMaxVariableMods()
        {
            return _maxVariableMods;
        }

        public void setMaxVariableMods(int maxVariableMods)
        {
            _maxVariableMods = maxVariableMods;
        }

        public int getMaxNeutralLosses()
        {
            return _maxNeutralLosses;
        }

        public void setMaxNeutralLosses(int maxNeutralLosses)
        {
            _maxNeutralLosses = maxNeutralLosses;
        }
    }

    public static final class RunIsotopeModification extends IsotopeModification
    {
        private int _runId;
        private int _isotopeModId;
        private int _isotopeLabelId;

        private String _isotopeLabel;
        private Boolean _explicitMod;
        private String _relativeRt;  // One of "Matching", "Overlapping", "Preceding", "Unknown"

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public int getIsotopeModId()
        {
            return _isotopeModId;
        }

        public void setIsotopeModId(int isotopeModId)
        {
            _isotopeModId = isotopeModId;
        }

        public int getIsotopeLabelId()
        {
            return _isotopeLabelId;
        }

        public void setIsotopeLabelId(int isotopeLabelId)
        {
            _isotopeLabelId = isotopeLabelId;
        }

        public String getIsotopeLabel()
        {
            return _isotopeLabel;
        }

        public void setIsotopeLabel(String isotopeLabel)
        {
            _isotopeLabel = isotopeLabel;
        }

        public Boolean getExplicitMod()
        {
            return _explicitMod;
        }

        public void setExplicitMod(Boolean explicitMod)
        {
            _explicitMod = explicitMod;
        }

        public String getRelativeRt()
        {
            return _relativeRt;
        }

        public void setRelativeRt(String relativeRt)
        {
            _relativeRt = relativeRt;
        }
    }

    public static class IsotopeModification extends Modification
    {
        private Boolean _label13C;
        private Boolean _label15N;
        private Boolean _label18O;
        private Boolean _label2H;

        public Boolean getLabel13C()
        {
            return _label13C;
        }

        public void setLabel13C(Boolean label13C)
        {
            _label13C = label13C;
        }

        public Boolean isLabel15N()
        {
            return _label15N;
        }

        public void setLabel15N(Boolean label15N)
        {
            _label15N = label15N;
        }

        public Boolean isLabel18O()
        {
            return _label18O;
        }

        public void setLabel18O(Boolean label18O)
        {
            _label18O = label18O;
        }

        public Boolean isLabel2H()
        {
            return _label2H;
        }

        public void setLabel2H(Boolean label2H)
        {
            _label2H = label2H;
        }
    }

    public static final class RunStructuralModification extends StructuralModification
    {
        private int _runId;
        private int _structuralModId;

        private Boolean explicitMod;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public int getStructuralModId()
        {
            return _structuralModId;
        }

        public void setStructuralModId(int structuralModId)
        {
            _structuralModId = structuralModId;
        }

        public Boolean getExplicitMod()
        {
            return explicitMod;
        }

        public void setExplicitMod(Boolean explicitMod)
        {
            this.explicitMod = explicitMod;
        }
    }

    public static class StructuralModification extends Modification
    {
        private boolean _variable;

        public boolean isVariable()
        {
            return _variable;
        }

        public void setVariable(boolean variable)
        {
            this._variable = variable;
        }
    }

    public static class Modification extends SkylineEntity
    {
        private String _name;
        private String _aminoAcid;
        private String _terminus;
        private String _formula;
        private Double _massDiffMono;
        private Double _massDiffAvg;
        private Integer _unimodId;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getAminoAcid()
        {
            return _aminoAcid;
        }

        public void setAminoAcid(String aminoAcid)
        {
            _aminoAcid = aminoAcid;
        }

        public String getTerminus()
        {
            return _terminus;
        }

        public void setTerminus(String terminus)
        {
            _terminus = terminus;
        }

        public String getFormula()
        {
            return _formula;
        }

        public void setFormula(String formula)
        {
            _formula = formula;
        }

        public Double getMassDiffMono()
        {
            return _massDiffMono;
        }

        public void setMassDiffMono(Double massDiffMono)
        {
            _massDiffMono = massDiffMono;
        }

        public Double getMassDiffAvg()
        {
            return _massDiffAvg;
        }

        public void setMassDiffAvg(Double massDiffAvg)
        {
            _massDiffAvg = massDiffAvg;
        }

        public Integer getUnimodId()
        {
            return _unimodId;
        }

        public void setUnimodId(Integer unimodId)
        {
            _unimodId = unimodId;
        }
    }

    // ------------------------------------------------------------------------
    // Enzyme settings
    // ------------------------------------------------------------------------
    public static final class EnzymeDigestionSettings
    {
        private int _enzymeId;
        private int _runId;
        private Integer _maxMissedCleavages;
        private Boolean _excludeRaggedEnds;

        public int getEnzymeId()
        {
            return _enzymeId;
        }

        public void setEnzymeId(int enzymeId)
        {
            _enzymeId = enzymeId;
        }

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public Integer getMaxMissedCleavages()
        {
            return _maxMissedCleavages;
        }

        public void setMaxMissedCleavages(Integer maxMissedCleavages)
        {
            _maxMissedCleavages = maxMissedCleavages;
        }

        public Boolean getExcludeRaggedEnds()
        {
            return _excludeRaggedEnds;
        }

        public void setExcludeRaggedEnds(Boolean excludeRaggedEnds)
        {
            _excludeRaggedEnds = excludeRaggedEnds;
        }
    }

    public static final class Enzyme extends SkylineEntity
    {
        private String _name;
        private String _cut; // amino acids at which this _enzyme cleaves the peptide
        private String _noCut;
        private String _sense; // 'N' or  'C'

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getCut()
        {
            return _cut;
        }

        public void setCut(String cut)
        {
            _cut = cut;
        }

        public String getNoCut()
        {
            return _noCut;
        }

        public void setNoCut(String noCut)
        {
            _noCut = noCut;
        }

        public String getSense()
        {
            return _sense;
        }

        public void setSense(String sense)
        {
            _sense = sense;
        }
    }

    // ------------------------------------------------------------------------
    // Spectrum Library Settings
    // ------------------------------------------------------------------------
    public static final class SpectrumLibrarySettings
    {
        private int _runId;
        private String _pick;  // One of 'library', 'filter', 'both', 'either'
        private String _rankType; // One of 'Picked intensity' or 'Spectrum count'
        private Integer _peptideCount;

        private List<SpectrumLibrary> libraries;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public String getPick()
        {
            return _pick;
        }

        public void setPick(String pick)
        {
            _pick = pick;
        }

        public String getRankType()
        {
            return _rankType;
        }

        public void setRankType(String rankType)
        {
            _rankType = rankType;
        }

        public Integer getPeptideCount()
        {
            return _peptideCount;
        }

        public void setPeptideCount(Integer peptideCount)
        {
            _peptideCount = peptideCount;
        }

        public List<SpectrumLibrary> getLibraries()
        {
            return libraries;
        }

        public void setLibraries(List<SpectrumLibrary> libraries)
        {
            this.libraries = libraries;
        }
    }

    public static final class SpectrumLibrary extends SkylineEntity
    {
        private int _runId;
        private int _librarySourceId;
        private String _name;
        private String _fileNameHint;
        private String _skylineLibraryId;  // lsid in <bibliospec_lite_library> element, id in others
        private String _revision;
        private String _libraryType;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }

        public int getLibrarySourceId()
        {
            return _librarySourceId;
        }

        public void setLibrarySourceId(int librarySourceId)
        {
            _librarySourceId = librarySourceId;
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getFileNameHint()
        {
            return _fileNameHint;
        }

        public void setFileNameHint(String fileNameHint)
        {
            _fileNameHint = fileNameHint;
        }

        public String getSkylineLibraryId()
        {
            return _skylineLibraryId;
        }

        public void setSkylineLibraryId(String skylineLibraryId)
        {
            _skylineLibraryId = skylineLibraryId;
        }

        public String getRevision()
        {
            return _revision;
        }

        public void setRevision(String revision)
        {
            _revision = revision;
        }

        public String getLibraryType()
        {
            return _libraryType;
        }

        public void setLibraryType(String libraryType)
        {
            _libraryType = libraryType;
        }
    }
}
