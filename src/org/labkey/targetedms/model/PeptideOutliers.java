package org.labkey.targetedms.model;

import lombok.Getter;
import lombok.Setter;

public class PeptideOutliers
{
    @Getter @Setter private String peptide;
    @Getter @Setter private String metric;
    @Getter @Setter private int count;
}
