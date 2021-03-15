package org.labkey.targetedms.chromlib;

import org.labkey.targetedms.parser.TransitionOptimization;

import java.util.Objects;

public class LibMoleculeTransitionOptimization extends AbstractLibTransitionOptimization
{
    public LibMoleculeTransitionOptimization()
    {
    }

    public LibMoleculeTransitionOptimization(TransitionOptimization optimization)
    {
        super(optimization);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibMoleculeTransitionOptimization that = (LibMoleculeTransitionOptimization) o;
        return Objects.equals(_transitionId, that._transitionId) &&
                Objects.equals(_optimizationType, that._optimizationType) &&
                Objects.equals(_optimizationValue, that._optimizationValue);
    }
}
