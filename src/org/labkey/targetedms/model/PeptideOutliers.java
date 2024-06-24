package org.labkey.targetedms.model;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.Map;

public class PeptideOutliers
{
    @Getter @Setter private String peptide;
    @Getter @Setter Map<String, Integer> outlierCountsPerMetric;

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("peptide", peptide);
        json.put("outlierCountsPerMetric", outlierCountsPerMetric);
        return json;
    }
}
