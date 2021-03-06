/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
package org.labkey.targetedms.conflict;

import org.labkey.api.util.HtmlString;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.query.PrecursorManager;
import org.labkey.targetedms.view.ModifiedPeptideHtmlMaker;
import org.labkey.targetedms.view.PrecursorHtmlMaker;

/**
 * User: vsharma
 * Date: 11/25/12
 * Time: 7:15 PM
 */
public class ConflictPrecursor
{
    private int _newPrecursorId;
    private int _newPrecursorRunId;
    private String _newRunFile;
    private String _newPrecursorLabel;
    private int _oldPrecursorId;
    private int _oldPrecursorRunId;
    private String _oldRunFile;
    private String _oldPrecursorLabel;
    private boolean _peptide;
    private String _moleculeName;

    public ConflictPrecursor() {}

    public int getNewPrecursorId()
    {
        return _newPrecursorId;
    }

    public void setNewPrecursorId(int newPrecursorId)
    {
        _newPrecursorId = newPrecursorId;
    }

    public int getNewPrecursorRunId()
    {
        return _newPrecursorRunId;
    }

    public void setNewPrecursorRunId(int newPrecursorRunId)
    {
        _newPrecursorRunId = newPrecursorRunId;
    }

    public String getNewRunFile()
    {
        return _newRunFile;
    }

    public void setNewRunFile(String newRunFile)
    {
        _newRunFile = newRunFile;
    }

    public String getNewPrecursorLabel()
    {
        return _newPrecursorLabel;
    }

    public void setNewPrecursorLabel(String newPrecursorLabel)
    {
        _newPrecursorLabel = newPrecursorLabel;
    }

    public int getOldPrecursorId()
    {
        return _oldPrecursorId;
    }

    public void setOldPrecursorId(int oldPrecursorId)
    {
        _oldPrecursorId = oldPrecursorId;
    }

    public int getOldPrecursorRunId()
    {
        return _oldPrecursorRunId;
    }

    public void setOldPrecursorRunId(int oldPrecursorRunId)
    {
        _oldPrecursorRunId = oldPrecursorRunId;
    }

    public String getOldRunFile()
    {
        return _oldRunFile;
    }

    public void setOldRunFile(String oldRunFile)
    {
        _oldRunFile = oldRunFile;
    }

    public String getOldPrecursorLabel()
    {
        return _oldPrecursorLabel;
    }

    public void setOldPrecursorLabel(String oldPrecursorLabel)
    {
        _oldPrecursorLabel = oldPrecursorLabel;
    }

    public boolean isPeptide()
    {
        return _peptide;
    }

    public void setPeptide(boolean peptide)
    {
        _peptide = peptide;
    }

    public String getMoleculeName()
    {
        return _moleculeName;
    }

    public void setMoleculeName(String moleculeName)
    {
        _moleculeName = moleculeName;
    }

    public HtmlString getHTML(TargetedMSSchema schema, ModifiedPeptideHtmlMaker modifiedPeptideHtmlMaker, boolean newVersion)
    {
        if (_peptide)
        {
            return PrecursorHtmlMaker.getModSeqChargeHtml(modifiedPeptideHtmlMaker,
                    PrecursorManager.getPrecursor(schema.getContainer(), newVersion ? getNewPrecursorId() : getOldPrecursorId(), schema.getUser()),
                    newVersion ? getNewPrecursorRunId() : getOldPrecursorRunId(), schema);
        }
        return HtmlString.of(_moleculeName);
    }
}
