/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.targetedms.RepresentativeDataState;
import org.labkey.targetedms.query.PeptideGroupManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: vsharma
 * Date: 4/2/12
 * Time: 2:13 PM
 */
public class PeptideGroup extends AnnotatedEntity<PeptideGroupAnnotation>
{
    private static final int MAX_LENGTH = 1050;

    private long _runId;

    private String _label;
    private String _name;
    private String _description;

    private boolean _decoy;
    private Double _decoyMatchProportion;

    private List<Protein> _proteins;

    private String _note;

    private String _altDescription;

    protected RepresentativeDataState _representativeDataState = RepresentativeDataState.NotRepresentative;

    public long getRunId()
    {
        return _runId;
    }

    public void setRunId(long runId)
    {
        _runId = runId;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(String label)
    {
        _label = StringUtils.truncate(label, MAX_LENGTH);
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = StringUtils.truncate(name, MAX_LENGTH);
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = StringUtils.truncate(description, MAX_LENGTH);
    }

    public boolean isDecoy()
    {
        return _decoy;
    }

    public void setDecoy(boolean decoy)
    {
        _decoy = decoy;
    }

    public Double getDecoyMatchProportion()
    {
        return _decoyMatchProportion;
    }

    public void setDecoyMatchProportion(Double decoyMatchProportion)
    {
        _decoyMatchProportion = decoyMatchProportion;
    }

    public void setNote(String note)
    {
        _note = note;
    }

    public String getNote()
    {
        return _note;
    }

    public List<Protein> getProteins()
    {
        return getProteins(true);
    }
    public List<Protein> getProteins(boolean includeNullSeqIds)
    {
        if (_proteins == null)
        {
            _proteins = PeptideGroupManager.getProteinsForPeptideGroup(getId(), true);
        }
        return Collections.unmodifiableList(includeNullSeqIds ? _proteins :
                _proteins.stream().filter(p -> p.getSequence() != null).collect(Collectors.toList()));
    }

    public void setProteins(List<Protein> proteins)
    {
        _proteins = proteins;
    }

    public RepresentativeDataState getRepresentativeDataState()
    {
        return _representativeDataState;
    }

    public void setRepresentativeDataState(RepresentativeDataState representativeDataState)
    {
        _representativeDataState = representativeDataState;
    }

    public String getAltDescription()
    {
        return _altDescription;
    }

    public void setAltDescription(String altDescription)
    {
        _altDescription = StringUtils.truncate(altDescription, MAX_LENGTH);
    }

    public Protein addSingleProtein()
    {
        if (_proteins == null)
        {
            _proteins = new ArrayList<>();
        }
        Protein result = new Protein();
        result.setName(_name);
        result.setLabel(_label);
        result.setDescription(_description);
        result.setAltDescription(_altDescription);
        _proteins.add(result);
        return result;
    }

    public void addProtein(Protein protein)
    {
        if (_proteins == null)
        {
            _proteins = new ArrayList<>();
        }
        _proteins.add(protein);
    }

    /**
     * @return a description of the type of the grouping - "Protein" for single-protein proteomics data or "Group"
     * when there's small molecule data or multiple proteins
     */
    public String getFieldLabel()
    {
        return getProteins().isEmpty() ? "Group" : "Protein";
    }
}
