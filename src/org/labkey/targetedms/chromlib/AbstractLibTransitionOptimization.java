package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.TransitionOptimization;

import java.util.Objects;

public abstract class AbstractLibTransitionOptimization extends AbstractLibEntity
{
    protected int _transitionId;
    protected String _optimizationType;
    protected double _optimizationValue;

    public AbstractLibTransitionOptimization() {}

    public AbstractLibTransitionOptimization(TransitionOptimization optimization)
    {
        _optimizationType = optimization.getOptimizationType();
        _optimizationValue = optimization.getOptValue();
    }

    public int getTransitionId()
    {
        return _transitionId;
    }

    public void setTransitionId(int transitionId)
    {
        _transitionId = transitionId;
    }

    public String getOptimizationType()
    {
        return _optimizationType;
    }

    public void setOptimizationType(String optimizationType)
    {
        _optimizationType = optimizationType;
    }

    public double getOptimizationValue()
    {
        return _optimizationValue;
    }

    public void setOptimizationValue(double optimizationValue)
    {
        _optimizationValue = optimizationValue;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(_transitionId, _optimizationType, _optimizationValue);
    }
}
