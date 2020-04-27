package org.labkey.api.targetedms.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public abstract class OutlierCounts
{
    private int _CUSUMmP;
    private int _CUSUMvP;
    private int _CUSUMmN;
    private int _CUSUMvN;
    private int _mR;
    private int _leveyJennings;

    public int getCUSUMm()
    {
        return _CUSUMmP + _CUSUMmN;
    }

    public int getCUSUMv()
    {
        return getCUSUMvP() + getCUSUMvP();
    }

    public int getCUSUMmN()
    {
        return _CUSUMmN;
    }

    public void setCUSUMmN(int CUSUMmN)
    {
        _CUSUMmN = CUSUMmN;
    }

    public int getCUSUMmP()
    {
        return _CUSUMmP;
    }

    public void setCUSUMmP(int CUSUMmP)
    {
        _CUSUMmP = CUSUMmP;
    }

    public int getCUSUMvP()
    {
        return _CUSUMvP;
    }

    public void setCUSUMvP(int CUSUMvP)
    {
        _CUSUMvP = CUSUMvP;
    }

    public int getCUSUMvN()
    {
        return _CUSUMvN;
    }

    public void setCUSUMvN(int CUSUMvN)
    {
        _CUSUMvN = CUSUMvN;
    }

    public int getmR()
    {
        return _mR;
    }

    public void setmR(int mR)
    {
        _mR = mR;
    }

    public int getLeveyJennings()
    {
        return _leveyJennings;
    }

    public void setLeveyJennings(int leveyJennings)
    {
        _leveyJennings = leveyJennings;
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("CUSUMm", getCUSUMm());
        jsonObject.put("CUSUMv", getCUSUMv());
        jsonObject.put("CUSUMmN", getCUSUMmN());
        jsonObject.put("CUSUMmP", getCUSUMmP());
        jsonObject.put("CUSUMvN", getCUSUMvN());
        jsonObject.put("CUSUMvP", getCUSUMvP());
        jsonObject.put("mR", getmR());
        jsonObject.put("LeveyJennings", getLeveyJennings());

        return jsonObject;
    }
}
