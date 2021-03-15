package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.TransitionOptimization;

import java.util.Objects;

public class LibTransitionOptimization extends AbstractLibTransitionOptimization
{
    public LibTransitionOptimization()
    {
    }

    public LibTransitionOptimization(TransitionOptimization optimization)
    {
        super(optimization);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibTransitionOptimization that = (LibTransitionOptimization) o;
        return Objects.equals(_transitionId, that._transitionId) &&
                Objects.equals(_optimizationType, that._optimizationType) &&
                Objects.equals(_optimizationValue, that._optimizationValue);
    }
}
