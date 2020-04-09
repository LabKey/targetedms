package org.labkey.api.targetedms;

public class BlibSourceFiles
{
    private String[] spectrumSourceFiles;
    private String[] idFiles;

    public BlibSourceFiles(String[] spectrumSourceFiles, String[] idFiles)
    {
        this.spectrumSourceFiles = spectrumSourceFiles;
        this.idFiles = idFiles;
    }

    public String[] getSpectrumSourceFiles()
    {
        return spectrumSourceFiles;
    }

    public String[] getIdFiles()
    {
        return idFiles;
    }
}
