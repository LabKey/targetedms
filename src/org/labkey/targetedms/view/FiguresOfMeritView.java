package org.labkey.targetedms.view;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NotFoundException;
import org.labkey.targetedms.TargetedMSController;
import org.labkey.targetedms.TargetedMSManager;
import org.labkey.targetedms.TargetedMSRun;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.parser.GeneralMolecule;
import org.labkey.targetedms.parser.QuantificationSettings;
import org.labkey.targetedms.query.MoleculeManager;
import org.labkey.targetedms.query.PeptideManager;

public class FiguresOfMeritView extends JspView<FiguresOfMeritView.MoleculeInfo>
{
    public FiguresOfMeritView(MoleculeInfo moleculeInfo, UserSchema schema, boolean minimize)
    {
        super("/org/labkey/targetedms/view/figuresOfMerit.jsp");

        Container container = schema.getContainer();
        long generalMoleculeId = moleculeInfo._generalMoleculeId;

        setTitle("Figures of Merit");
        setTitleHref(new ActionURL(TargetedMSController.ShowFiguresOfMeritAction.class, container).addParameter("GeneralMoleculeId", generalMoleculeId));
        setModelBean(moleculeInfo);
        moleculeInfo.setMinimize(minimize);

        moleculeInfo._run = TargetedMSManager.getRun(moleculeInfo.getRunId());
        if (moleculeInfo._run == null || !moleculeInfo._run.getContainer().equals(container))
            throw new NotFoundException("Could not find RunId " + moleculeInfo.getRunId());

        QuantificationSettings settings = new TableSelector(schema.getTable(TargetedMSSchema.TABLE_QUANTIIFICATION_SETTINGS), new SimpleFilter(FieldKey.fromParts("RunId"), moleculeInfo._run.getId()), null).getObject(QuantificationSettings.class);
        moleculeInfo.setQuantificationSettings(settings);

        if (moleculeInfo.getPeptideName() != null)
        {
            moleculeInfo._generalMolecule = PeptideManager.getPeptide(container, generalMoleculeId);
        }
        else
        {
            moleculeInfo._generalMolecule = MoleculeManager.getMolecule(container, generalMoleculeId);
        }
    }

    public static class MoleculeInfo
    {
        long _runId;
        long _generalMoleculeId;
        String _peptideName;
        String _moleculeName;
        String _fileName;
        String _sampleFiles;
        QuantificationSettings _settings;

        GeneralMolecule<?, ?> _generalMolecule;
        TargetedMSRun _run;
        private boolean _minimize;

        public long getRunId()
        {
            return _runId;
        }

        public void setRunId(long runId)
        {
            _runId = runId;
        }

        public long getGeneralMoleculeId()
        {
            return _generalMoleculeId;
        }

        public void setGeneralMoleculeId(long moleculeId)
        {
            _generalMoleculeId = moleculeId;
        }

        public String getPeptideName()
        {
            return _peptideName;
        }

        public void setPeptideName(String peptideName)
        {
            _peptideName = peptideName;
        }

        public String getMoleculeName()
        {
            return _moleculeName;
        }

        public void setMoleculeName(String moleculeName)
        {
            _moleculeName = moleculeName;
        }

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            _fileName = fileName;
        }

        public String getSampleFiles()
        {
            return _sampleFiles;
        }

        public void setSampleFiles(String sampleFiles)
        {
            _sampleFiles = sampleFiles;
        }

        public void setQuantificationSettings(QuantificationSettings settings)
        {
            _settings = settings;
        }

        /** Defaults to 30 if nothing is set */
        public double getMaxLOQBias()
        {
            return _settings == null || _settings.getMaxLOQBias() == null ? 30.0 : _settings.getMaxLOQBias().doubleValue();
        }

        /** Defaults to null if nothing is set */
        public Double getMaxLOQCV()
        {
            return _settings == null ? null : _settings.getMaxLOQCV();
        }

        /** Defaults to "none" if nothing is set */
        public String getLODCalculation()
        {
            return _settings == null || _settings.getLODCalculation() == null ? "none" : _settings.getLODCalculation();
        }

        public TargetedMSRun getRun()
        {
            return _run;
        }

        public GeneralMolecule<?, ?> getGeneralMolecule()
        {
            return _generalMolecule;
        }

        public void setMinimize(boolean minimize)
        {
            _minimize = minimize;
        }

        public boolean getMinimize()
        {
            return _minimize;
        }
    }
}
