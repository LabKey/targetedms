/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.parser;

/**
 * A calculated fold change comparing protein or peptide levels between two different groups of replicates.
 * The control group is the set of replicates whose {@link GroupComparisonSettings#getControlAnnotation()} value
 * is {@link GroupComparisonSettings#getControlValue()}. For the scenario when more than two groups are being
 * compared, {@link #getGroupIdentifier()} will contain the value identifying the set of replicates in the numerator.
 */
public class FoldChange
{
    private int _id;
    private long _runId;
    private long _groupComparisonSettingsId;
    private Long _peptideGroupId;
    private Long _generalMoleculeId;
    private Long _isotopeLabelId;
    private Integer _msLevel;
    private String _groupIdentifier;
    private double _log2FoldChange;
    private double _adjustedPValue;
    private double _standardError;
    private int _degreesOfFreedom;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public long getRunId()
    {
        return _runId;
    }

    public void setRunId(long runId)
    {
        _runId = runId;
    }

    /**
     * Returns the ID of the {@link GroupComparisonSettings}
     */
    public long getGroupComparisonSettingsId()
    {
        return _groupComparisonSettingsId;
    }

    public void setGroupComparisonSettingsId(long groupComparisonSettingsId)
    {
        _groupComparisonSettingsId = groupComparisonSettingsId;
    }

    public Long getPeptideGroupId()
    {
        return _peptideGroupId;
    }

    public void setPeptideGroupId(Long peptideGroupId)
    {
        _peptideGroupId = peptideGroupId;
    }

    public Long getGeneralMoleculeId()
    {
        return _generalMoleculeId;
    }

    public void setGeneralMoleculeId(Long generalMoleculeId)
    {
        _generalMoleculeId = generalMoleculeId;
    }

    public Long getIsotopeLabelId()
    {
        return _isotopeLabelId;
    }

    public void setIsotopeLabelId(Long isotopeLabelId)
    {
        _isotopeLabelId = isotopeLabelId;
    }

    public Integer getMsLevel()
    {
        return _msLevel;
    }

    public void setMsLevel(Integer msLevel)
    {
        _msLevel = msLevel;
    }

    public String getGroupIdentifier()
    {
        return _groupIdentifier;
    }

    public void setGroupIdentifier(String groupIdentifier)
    {
        _groupIdentifier = groupIdentifier;
    }

    public double getLog2FoldChange()
    {
        return _log2FoldChange;
    }

    public void setLog2FoldChange(double log2FoldChange)
    {
        _log2FoldChange = log2FoldChange;
    }

    public double getAdjustedPValue()
    {
        return _adjustedPValue;
    }

    public void setAdjustedPValue(double adjustedPValue)
    {
        _adjustedPValue = adjustedPValue;
    }

    public double getStandardError()
    {
        return _standardError;
    }

    public void setStandardError(double standardError)
    {
        _standardError = standardError;
    }

    public int getDegreesOfFreedom()
    {
        return _degreesOfFreedom;
    }

    public void setDegreesOfFreedom(int degreesOfFreedom)
    {
        _degreesOfFreedom = degreesOfFreedom;
    }
}
