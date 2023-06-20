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

public class ChromatogramGroupId
{
    private Target _target;
    private String _qcTraceName;

    private ChromatogramGroupId()
    {
    }
    public ChromatogramGroupId(Target target) {
        _target = target;
    }

    public Target getTarget()
    {
        return _target;
    }

    public String getQcTraceName()
    {
        return _qcTraceName;
    }

    public static ChromatogramGroupId forQcTraceName(String qcTraceName) {
        ChromatogramGroupId chromatogramGroupId = new ChromatogramGroupId();
        chromatogramGroupId._qcTraceName = qcTraceName;
        return chromatogramGroupId;
    }

    public static List<ChromatogramGroupId> fromProtos(ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto proto)
    {
        List<Target> targets = new ArrayList<>();
        targets.add(null);
        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.Target target : proto.getTargetsList()) {
            targets.add(new Target(target));
        }
        List<ChromatogramGroupId> list = new ArrayList<>();
        for (ChromatogramGroupDataOuterClass.ChromatogramGroupIdsProto.ChromatogramGroupId chromatogramGroupId : proto.getChromatogramGroupIdsList()) {
            list.add(new ChromatogramGroupId(targets.get(chromatogramGroupId.getTargetIndex())));
        }
        return list;
    }
}
