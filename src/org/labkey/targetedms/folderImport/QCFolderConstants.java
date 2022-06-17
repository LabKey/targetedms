package org.labkey.targetedms.folderImport;

import org.labkey.targetedms.TargetedMSSchema;

public class QCFolderConstants
{
    protected static final String QC_FOLDER_DIR = "PanoramaQC";
    public static final String CATEGORY = "TargetedMSLeveyJenningsPlotOptions";

    protected static final String QC_METRIC_CONFIGURATION_FILE_NAME = TargetedMSSchema.TABLE_QC_METRIC_CONFIGURATION + ".tsv";
    protected static final String QC_ENABLED_METRICS_FILE_NAME = TargetedMSSchema.TABLE_QC_ENABLED_METRICS + ".tsv";
    protected static final String GUIDE_SET_FILE_NAME = TargetedMSSchema.TABLE_GUIDE_SET + ".tsv";
    protected static final String QC_METRIC_EXCLUSION_FILE_NAME = TargetedMSSchema.TABLE_QC_METRIC_EXCLUSION + ".tsv";
    protected static final String PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION_FILE_NAME = TargetedMSSchema.TABLE_PEPTIDE_MOLECULE_PRECURSOR_EXCLUSION + ".tsv";
    protected static final String QC_ANNOTATION_FILE_NAME = TargetedMSSchema.TABLE_QC_ANNOTATION + ".tsv";
    protected static final String QC_ANNOTATION_TYPE = TargetedMSSchema.TABLE_QC_ANNOTATION_TYPE + ".tsv";
    protected static final String REPLICATE_ANNOTATION_FILE_NAME = TargetedMSSchema.TABLE_REPLICATE_ANNOTATION + ".tsv";

    protected static final String QC_PLOT_SETTINGS_PROPS_FILE_NAME = "PlotSettings.properties";
}
