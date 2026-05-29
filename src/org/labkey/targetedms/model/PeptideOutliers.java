/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.targetedms.model;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.Map;

@Setter
@Getter
public class PeptideOutliers
{
    private String peptide;
    Map<String, Integer> outlierCountsPerMetric;
    private int totalOutliers;
    private Long precursorId;

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("peptide", peptide);
        json.put("outlierCountsPerMetric", outlierCountsPerMetric);
        json.put("totalOutliers", totalOutliers);
        json.put("precursorId", precursorId);
        return json;
    }
}
