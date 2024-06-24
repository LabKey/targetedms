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

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("peptide", peptide);
        json.put("outlierCountsPerMetric", outlierCountsPerMetric);
        json.put("totalOutliers", totalOutliers);
        return json;
    }
}
