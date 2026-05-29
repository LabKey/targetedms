/*
 * Copyright (c) 2021-2026 LabKey Corporation
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

import java.util.regex.Pattern;

public class OptimizationDBRow
{
    private String _peptideModSeq;
    private String _charge;
    private String _fragmentIon;
    private String _productCharge;
    private double _value;
    private String _type;

    public String getPeptideModSeq()
    {
        return _peptideModSeq;
    }

    public void setPeptideModSeq(String peptideModSeq)
    {
        _peptideModSeq = peptideModSeq;
    }

    public String getCharge()
    {
        return _charge;
    }

    public void setCharge(String charge)
    {
        _charge = charge;
    }

    public String getFragmentIon()
    {
        return _fragmentIon;
    }

    public void setFragmentIon(String fragmentIon)
    {
        _fragmentIon = fragmentIon;
    }

    public String getProductCharge()
    {
        return _productCharge;
    }

    public void setProductCharge(String productCharge)
    {
        _productCharge = productCharge;
    }

    public double getValue()
    {
        return _value;
    }

    public void setValue(double value)
    {
        _value = value;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        _type = type;
    }

    public boolean matches(Transition transition, Precursor precursor)
    {
        return !_peptideModSeq.startsWith("#") &&
                _peptideModSeq.equalsIgnoreCase(precursor.getModifiedSequence()) &&
                Integer.parseInt(_productCharge) == transition.getCharge() &&
                _fragmentIon.equalsIgnoreCase(transition.getFragmentType() + transition.getFragmentOrdinal()) &&
                Integer.parseInt(_charge) == precursor.getCharge();
    }

    public boolean matches(MoleculeTransition transition, MoleculePrecursor precursor, Molecule molecule)
    {
        if (_peptideModSeq.startsWith("#") && _peptideModSeq.length() > 3)
        {
            char separator = _peptideModSeq.charAt(1);
            String[] parts = _peptideModSeq.substring(3).split(Pattern.quote(String.valueOf(separator)));
            if (parts.length >= 2)
            {
                if (!parts[0].equals(molecule.getCustomIonName()))
                {
                    return false;
                }
                if (parts[1].contains("/"))
                {
                    if (precursor.getMassMonoisotopic() == null || precursor.getMassAverage() == null)
                    {
                        return false;
                    }
                    String masses = String.format("%.9f/%.9f", precursor.getMassMonoisotopic(), precursor.getMassAverage());
                    if (!masses.equals(parts[1]))
                    {
                        return false;
                    }
                }
                else
                {
                    if (!parts[1].equals(molecule.getIonFormula()))
                    {
                        return false;
                    }
                }
            }
            else
            {
                return false;
            }

            if (!_charge.equals(precursor.getIonFormula()) || !_productCharge.equals(transition.getIonFormula()))
            {
                return false;
            }

            if (transition.getMassMonoisotopic() == null || transition.getMassAverage() == null)
            {
                return false;
            }

            String fragmentIon = String.format("[%.6f/%.6f]", transition.getMassMonoisotopic(), transition.getMassAverage());
            return _fragmentIon.contains(fragmentIon);
        }
        return false;
    }
}
