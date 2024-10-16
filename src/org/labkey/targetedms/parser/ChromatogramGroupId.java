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

import org.labkey.targetedms.parser.proto.ChromatogramGroupDataOuterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChromatogramGroupId
{
    private Target _target;
    private String _qcTraceName;
    private SpectrumFilter _spectrumFilter;

    private ChromatogramGroupId()
    {
    }
    public ChromatogramGroupId(Target target, SpectrumFilter spectrumFilter)
    {
        _target = target;
        _spectrumFilter = spectrumFilter;
    }

    public Target getTarget()
    {
        return _target;
    }

    public String getQcTraceName()
    {
        return _qcTraceName;
    }

    public SpectrumFilter getSpectrumFilter()
    {
        return _spectrumFilter;
    }

    public static ChromatogramGroupId forQcTraceName(String qcTraceName)
    {
        ChromatogramGroupId chromatogramGroupId = new ChromatogramGroupId();
        chromatogramGroupId._qcTraceName = qcTraceName;
        return chromatogramGroupId;
    }

    public static List<ChromatogramGroupId> fromProtos(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto proto)
    {
        List<Target> targets = new ArrayList<>();
        // Make one-based lookups easy
        targets.add(null);

        List<SpectrumFilter.FilterClause> filterClauses = new ArrayList<>();

        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.Target target : proto.getTargetsList())
        {
            targets.add(new Target(target));
        }
        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.SpectrumFilter spectrumFilter : proto.getFiltersList()) 
        {
            filterClauses.add(SpectrumFilter.FilterClause.fromProtocolMessage(spectrumFilter));
        }
        List<ChromatogramGroupId> list = new ArrayList<>();
        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.ChromatogramGroupId chromatogramGroupId : proto.getChromatogramGroupIdsList())
        {
            SpectrumFilter spectrumFilter = SpectrumFilter.fromFilterClauses(
                    chromatogramGroupId.getFilterIndexesList().stream()
                    .map(filterClauses::get).collect(Collectors.toList())).orElse(null);
            list.add(new ChromatogramGroupId(targets.get(chromatogramGroupId.getTargetIndex()), spectrumFilter));
        }
        return list;
    }
}
