/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.MoleculePrecursor;
import org.labkey.targetedms.parser.MoleculeTransition;
import org.labkey.targetedms.parser.TransitionChromInfo;
import org.labkey.targetedms.parser.TransitionOptimization;

import java.util.List;

/**
 * User: vsharma
 * Date: 12/31/12
 * Time: 9:25 AM
 */
public class LibMoleculeTransition extends AbstractLibTransition<LibMoleculeTransitionOptimization>
{
    private long _moleculePrecursorId;
    private String _fragmentName;
    private String _chemicalFormula;
    private String _adduct;

    public LibMoleculeTransition() {}

    public LibMoleculeTransition(MoleculeTransition transition, TransitionChromInfo tci, MoleculePrecursor precursor, List<TransitionOptimization> optimizations)
    {
        super(transition, tci, precursor, optimizations);
        setFragmentName(transition.getCustomIonName());
        setChemicalFormula(transition.getChemicalFormula());
        setAdduct(transition.getAdduct());
    }

    @Override
    protected LibMoleculeTransitionOptimization createOptimization(TransitionOptimization optimization)
    {
        return new LibMoleculeTransitionOptimization(optimization);
    }

    public long getMoleculePrecursorId()
    {
        return _moleculePrecursorId;
    }

    public void setMoleculePrecursorId(long moleculePrecursorId)
    {
        _moleculePrecursorId = moleculePrecursorId;
    }

    public String getFragmentName()
    {
        return _fragmentName;
    }

    public void setFragmentName(String fragmentName)
    {
        _fragmentName = fragmentName;
    }

    public String getChemicalFormula()
    {
        return _chemicalFormula;
    }

    public void setChemicalFormula(String chemicalFormula)
    {
        _chemicalFormula = chemicalFormula;
    }

    public String getAdduct()
    {
        return _adduct;
    }

    public void setAdduct(String adduct)
    {
        _adduct = adduct;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LibMoleculeTransition)) return false;

        LibMoleculeTransition that = (LibMoleculeTransition) o;

        if (_moleculePrecursorId != that._moleculePrecursorId) return false;
        if (!_area.equals(that._area)) return false;
        if (_charge != null ? !_charge.equals(that._charge) : that._charge != null) return false;
        if (_chromatogramIndex != null ? !_chromatogramIndex.equals(that._chromatogramIndex) : that._chromatogramIndex != null)
            return false;
        if (_fragmentOrdinal != null ? !_fragmentOrdinal.equals(that._fragmentOrdinal) : that._fragmentOrdinal != null)
            return false;
        if (!_fragmentType.equals(that._fragmentType)) return false;
        if (!_fwhm.equals(that._fwhm)) return false;
        if (!_height.equals(that._height)) return false;
        if (_massIndex != null ? !_massIndex.equals(that._massIndex) : that._massIndex != null) return false;
        if (_mz != null ? !_mz.equals(that._mz) : that._mz != null) return false;
        if (_massErrorPPM != null ? !_massErrorPPM.equals(that._massErrorPPM) : that._massErrorPPM != null) return false;
        if (_chemicalFormula != null ? !_chemicalFormula.equals(that._chemicalFormula) : that._chemicalFormula != null) return false;
        if (_fragmentName != null ? !_fragmentName.equals(that._fragmentName) : that._fragmentName != null) return false;
        if (_adduct != null ? !_adduct.equals(that._adduct) : that._adduct != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) _moleculePrecursorId;
        result = 31 * result + (_mz != null ? _mz.hashCode() : 0);
        result = 31 * result + (_charge != null ? _charge.hashCode() : 0);
        result = 31 * result + _fragmentType.hashCode();
        result = 31 * result + (_fragmentOrdinal != null ? _fragmentOrdinal.hashCode() : 0);
        result = 31 * result + (_massIndex != null ? _massIndex.hashCode() : 0);
        result = 31 * result + _area.hashCode();
        result = 31 * result + _height.hashCode();
        result = 31 * result + _fwhm.hashCode();
        result = 31 * result + (_chromatogramIndex != null ? _chromatogramIndex.hashCode() : 0);
        result = 31 * result + (_massErrorPPM != null ? _massErrorPPM.hashCode() : 0);
        result = 31 * result + (_chemicalFormula != null ? _chemicalFormula.hashCode() : 0);
        result = 31 * result + (_fragmentName != null ? _fragmentName.hashCode() : 0);
        result = 31 * result + (_adduct != null ? _adduct.hashCode() : 0);
        return result;
    }
}
