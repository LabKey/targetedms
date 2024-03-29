/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.calculations.quantification;

import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;
import java.util.List;

public class CalibrationCurveDataSet {
    private NormalizationMethod normalizationMethod = NormalizationMethod.NONE;
    private RegressionFit regressionFit = RegressionFit.NONE;
    private RegressionWeighting regressionWeighting = RegressionWeighting.NONE;
    private List<Replicate> replicates = new ArrayList<>();

    public Replicate addReplicate(SampleType sampleType, Double analyteConcentration, double sampleDilutionFactor, boolean excludeFromCalibration) {
        Replicate replicate = new Replicate(sampleType, analyteConcentration, sampleDilutionFactor, excludeFromCalibration);
        replicates.add(replicate);
        return replicate;
    }

    public NormalizationMethod getNormalizationMethod() {
        return normalizationMethod;
    }

    public void setNormalizationMethod(NormalizationMethod normalizationMethod) {
        this.normalizationMethod = normalizationMethod;
    }

    public RegressionFit getRegressionFit() {
        return regressionFit;
    }

    public void setRegressionFit(RegressionFit regressionFit) {
        this.regressionFit = regressionFit;
    }

    public RegressionWeighting getRegressionWeighting() {
        return regressionWeighting;
    }

    public void setRegressionWeighting(RegressionWeighting regressionWeighting) {
        this.regressionWeighting = regressionWeighting;
    }

    public CalibrationCurve getCalibrationCurve(String label) {
        if (RegressionFit.NONE == this.regressionFit) {
            return CalibrationCurve.forNoExternalStandards();
        }

        List<WeightedObservedPoint> weightedObservedPoints = new ArrayList<>();
        TransitionKeys featuresToQuantifyOn = getFeaturesToQuantifyOn(label);
        for (Replicate replicate : replicates) {
            if (replicate.getSampleType() != SampleType.standard) {
                continue;
            }
            if (replicate.isExcludeFromCalibration()) {
                continue;
            }
            Double x = replicate.getAnalyteConcentration();
            if (x == null) {
                continue;
            }
            Double y = replicate.getNormalizedArea(getNormalizationMethod(), label, featuresToQuantifyOn);
            if (y == null) {
                continue;
            }
            weightedObservedPoints.add(getWeightedPoint(x, y));
        }
        return regressionFit.fit(weightedObservedPoints);
    }

    public WeightedObservedPoint getWeightedPoint(double x, double y) {
        return new WeightedObservedPoint(getRegressionWeighting().getWeighting(x, y), x, y);
    }

    public TransitionKeys getFeaturesToQuantifyOn(String label) {
        if (normalizationMethod instanceof NormalizationMethod.RatioToLabel) {
            return null;
        }
        TransitionKeys transitionKeys = TransitionKeys.EMPTY;
        for (Replicate replicate : replicates) {
            if (replicate.getSampleType() == SampleType.standard && replicate.getAnalyteConcentration() != null) {
                TransitionAreas transitionAreas = replicate.getTransitionAreas(label);
                transitionKeys = transitionKeys.union(transitionAreas.getKeys());
            }
        }
        return transitionKeys;
    }

    public Double getCalculatedConcentration(String label, CalibrationCurve calibrationCurve, Replicate replicate) {
        if (calibrationCurve == null) {
            return null;
        }
        Double y = replicate.getNormalizedArea(getNormalizationMethod(), label, getFeaturesToQuantifyOn(label));
        if (y == null) {
            return null;
        }
        return calibrationCurve.getX(y) * replicate.getSampleDilutionFactor();
    }

    /**
     * Created by nicksh on 4/15/2016.
     */
    public static class Replicate extends ReplicateData {
        private SampleType sampleType;
        private Double analyteConcentration;
        private double sampleDilutionFactor;
        private boolean excludeFromCalibration;
        public Replicate(SampleType sampleType, Double analyteConcentration, double sampleDilutionFactor, boolean excludeFromCalibration) {
            this.sampleType = sampleType;
            this.analyteConcentration = analyteConcentration;
            this.sampleDilutionFactor = sampleDilutionFactor;
            this.excludeFromCalibration = excludeFromCalibration;
        }

        public SampleType getSampleType() {
            return sampleType;
        }

        public Double getAnalyteConcentration() {
            return analyteConcentration == null ? null : (analyteConcentration / sampleDilutionFactor);
        }

        public double getSampleDilutionFactor()
        {
            return sampleDilutionFactor;
        }

        public boolean isExcludeFromCalibration()
        {
            return excludeFromCalibration;
        }
    }
}
