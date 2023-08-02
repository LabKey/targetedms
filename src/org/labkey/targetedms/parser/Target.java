/*
 * Copyright (c) 2023 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.targetedms.parser.proto.ChromatogramGroupDataOuterClass;

import java.util.ArrayList;
import java.util.List;

public class Target
{
    private String modifiedPeptideSequence;
    private String formula;
    private String name;
    private double monoMass;
    private double averageMass;
    private String inChiKey;
    private String cas;
    private String hmdb;
    private String inChi;
    private String smiles;
    private String kegg;

    private Target() {

    }

    public Target(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.Target proto)
    {
        modifiedPeptideSequence = proto.getModifiedPeptideSequence();
        formula = proto.getFormula();
        monoMass = proto.getMonoMass();
        averageMass = proto.getAverageMass();
        inChiKey = proto.getInChiKey();
        cas = proto.getCas();
        hmdb = proto.getHmdb();
        inChi = proto.getInChi();
        smiles = proto.getSmiles();
        kegg = proto.getKegg();
    }

    public String getModifiedPeptideSequence()
    {
        return modifiedPeptideSequence;
    }

    public String getFormula()
    {
        return formula;
    }

    public String getName()
    {
        return name;
    }

    public double getMonoMass()
    {
        return monoMass;
    }

    public double getAverageMass()
    {
        return averageMass;
    }

    public String getInChiKey()
    {
        return inChiKey;
    }

    public String getCas()
    {
        return cas;
    }

    public String getHmdb()
    {
        return hmdb;
    }

    public String getInChi()
    {
        return inChi;
    }

    public String getSmiles()
    {
        return smiles;
    }

    public String getKegg()
    {
        return kegg;
    }

    public static Target fromChromatogramTextId(String textId)
    {
        Target target = new Target();
        if (!textId.startsWith("#"))
        {
            target.modifiedPeptideSequence = textId;
            return target;
        }
        // The separator is whatever appears between the first two "#". Usually it's "$", but could be
        // followed by any number of underscores.
        int ichSeparatorEnd = textId.indexOf('#', 1);
        if (ichSeparatorEnd < 0)
        {
            return null;
        }
        List<String> parts = parseTextIdParts(textId);
        if (parts == null)
        {
            return null;
        }

        if (parts.size() > 0)
        {
            target.name = parts.get(0);
        }
        if (parts.size() > 1)
        {
            target.formula = parts.get(1);
            int ichSlash = target.formula.indexOf("/");
            if (ichSlash > 0)
            {
                // Check to see if formula is actually two masses separated by a slash
                try
                {
                    double monoMass = Double.parseDouble(target.formula.substring(0, ichSlash));
                    double averageMass = Double.parseDouble(target.formula.substring(ichSlash + 1));

                    target.formula = null;
                    target.monoMass = monoMass;
                    target.averageMass = averageMass;
                }
                catch (Exception e)
                {
                    // Not masses: must be a real formula
                }
            }
        }
        return target;

    }
    private static List<String> parseTextIdParts(String textId)
    {
        List<String> parts = new ArrayList<>();
        int ichSeparatorEnd = textId.indexOf('#', 1);
        if (ichSeparatorEnd < 0)
        {
            return null;
        }
        String separator = textId.substring(1, ichSeparatorEnd);
        for (String escapedPart : StringUtils.splitByWholeSeparatorPreserveAllTokens(
                textId.substring(ichSeparatorEnd + 1), separator))
        {
            String part =escapedPart.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
            parts.add(part);
        }
        return parts;
    }
}

