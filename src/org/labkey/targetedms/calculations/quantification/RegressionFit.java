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
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class RegressionFit {
    public static final RegressionFit NONE = new RegressionFit("none", "None") {
        @Override
        protected CalibrationCurve performFit(List<WeightedObservedPoint> points) {
            CalibrationCurve curve = new CalibrationCurve(this);
            curve.setPointCount(0);
            curve.setSlope(1.0);
            return curve;
        }
    };

    public static final RegressionFit LINEAR = new RegressionFit("linear", "Linear") {
        @Override
        protected CalibrationCurve performFit(List<WeightedObservedPoint> points) {
            CalibrationCurve curve = new CalibrationCurve(this);
            curve.setPointCount(points.size());
            double[][] x = new double[points.size()][];
            double[] y = new double[points.size()];
            double[] weights = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                x[i] = new double[]{points.get(i).getX()};
                y[i] = points.get(i).getY();
                weights[i] = points.get(i).getWeight();
            }
            double[] result = WeightedRegression.weighted(x, y, weights, true);
            curve.setIntercept(result[0]);
            curve.setSlope(result[1]);
            return curve;
        }
    };

    public static final RegressionFit LINEAR_THROUGH_ZERO = new RegressionFit("linear_through_zero", "Linear through zero") {
        @Override
        protected CalibrationCurve performFit(List<WeightedObservedPoint> points) {
            CalibrationCurve curve = new CalibrationCurve(this);
            curve.setPointCount(points.size());
            double[][] x = new double[points.size()][];
            double[] y = new double[points.size()];
            double[] weights = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                x[i] = new double[]{points.get(i).getX()};
                y[i] = points.get(i).getY();
                weights[i] = points.get(i).getWeight();
            }
            double[] result = WeightedRegression.weighted(x, y, weights, false);
            curve.setSlope(result[0]);
            return curve;
        }
    };

    public static final RegressionFit QUADRATIC = new RegressionFit("quadratic", "Quadratic") {
        @Override
        protected CalibrationCurve performFit(List<WeightedObservedPoint> points) {
            CalibrationCurve curve = new CalibrationCurve(this);
            curve.setPointCount(points.size());
            double[][] x = new double[points.size()][];
            double[] y = new double[points.size()];
            double[] weights = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                double xValue = points.get(i).getX();
                x[i] = new double[]{xValue, xValue*xValue};
                y[i] = points.get(i).getY();
                weights[i] = points.get(i).getWeight();
            }
            double[] result = WeightedRegression.weighted(x, y, weights, true);
            curve.setIntercept(result[0]);
            curve.setSlope(result[1]);
            curve.setQuadraticCoefficient(result[2]);
            return curve;
        }

        @Override
        public double getY(CalibrationCurve calibrationCurve, double x)
        {
            return x*x*calibrationCurve.getQuadraticCoefficient() + x*calibrationCurve.getSlope() + calibrationCurve.getIntercept();
        }

        @Override
        public Double getX(CalibrationCurve calibrationCurve, double y)
        {
            double discriminant = calibrationCurve.getSlope()*calibrationCurve.getSlope()
                    - 4*calibrationCurve.getQuadraticCoefficient()*(calibrationCurve.getIntercept() - y);
            if (discriminant < 0)
            {
                return Double.NaN;
            }
            double sqrtDiscriminant = Math.sqrt(discriminant);
            return (-calibrationCurve.getSlope() + sqrtDiscriminant)/2/calibrationCurve.getQuadraticCoefficient();
        }
    };

    public static final RegressionFit LINEAR_IN_LOG_SPACE = new RegressionFit("linear_in_log_space", "Linear in Log Space")
    {
        protected CalibrationCurve performFit(List<WeightedObservedPoint> points)
        {
            if (points.stream().anyMatch(pt -> pt.getY() <= 0 || pt.getX() <= 0))
            {
                CalibrationCurve calibrationCurve = new CalibrationCurve(this);
                calibrationCurve.setErrorMessage("Unable to do a regression in log space because one or more points are non-positive.");
                return calibrationCurve;
            }
            var logPoints = points.stream().map(pt->logPoint(pt)).collect(Collectors.toList());
            var calibrationCurve = LINEAR.fit(logPoints);
            calibrationCurve.setRegressionFit(this);
            return calibrationCurve;
        }

        protected WeightedObservedPoint logPoint(WeightedObservedPoint pt)
        {
            return new WeightedObservedPoint(pt.getWeight(), Math.log(pt.getX()), Math.log(pt.getY()));
        }

        @Override
        public Double getX(CalibrationCurve calibrationCurve, double y)
        {
            var x = super.getX(calibrationCurve, Math.log(y));
            return x == null ? null : Math.exp(x);
        }

        @Override
        public double getY(CalibrationCurve calibrationCurve, double x)
        {
            var y = super.getY(calibrationCurve, Math.log(x));
            return Math.exp(y);
        }

        @Override
        public Double computeRSquared(CalibrationCurve curve, List<WeightedObservedPoint> points)
        {
            return LINEAR.computeRSquared(curve, points.stream().map(this::logPoint).collect(Collectors.toList()));
        }
    };

    public static final RegressionFit BILINEAR = new RegressionFit("bilinear", "Bilinear")
    {
        @Override
        protected CalibrationCurve performFit(List<WeightedObservedPoint> points)
        {
            Double bestLod = null;
            Double bestScore = Double.MAX_VALUE;
            var xValues = points.stream().map(pt -> pt.getX()).distinct().sorted().collect(Collectors.toList());
            for (int i = 0; i < xValues.size() - 1; i++)
            {
                double stepSize = (xValues.get(i + 1) - xValues.get(i)) / 4;
                double initialValue = (xValues.get(i) + xValues.get(i + 1)) / 2;
                var simplex = new NelderMeadSimplex(1, stepSize);
                var optimizer = new SimplexOptimizer(new SimpleValueChecker(0, 0, 50));
                PointValuePair optimum = optimizer.optimize(
                        GoalType.MINIMIZE,
                        MaxEval.unlimited(),
                        new ObjectiveFunction(multivariatePoints->LodObjectiveFunction(multivariatePoints[0], points)),
                        simplex,
                        new InitialGuess(new double[]{initialValue}));
                if (optimum.getValue() < bestScore) {
                    bestLod = optimum.getPoint()[0];
                    bestScore = optimum.getValue();
                }
            }
            if (bestLod == null)
            {
                var calibrationCurve = LINEAR.performFit(points);
                calibrationCurve.setRegressionFit(this);
                return calibrationCurve;
            }
            return getCalibrationCurveWithLod(bestLod, points);
        }

        /// <summary>
        /// Optimization function used when doing NelderMeadSimplex to find the best Limit of Detection.
        /// </summary>
        private static double LodObjectiveFunction(double lod, List<WeightedObservedPoint> WeightedObservedPoints)
        {
            CalibrationCurve calibrationCurve = getCalibrationCurveWithLod(lod, WeightedObservedPoints);
            if (calibrationCurve == null || !calibrationCurve.hasTurningPoint())
            {
                return Double.MAX_VALUE;
            }
            double totalDelta = 0;
            double totalWeight = 0;
            for (var pt : WeightedObservedPoints)
            {
                double delta = pt.getY() - calibrationCurve.getY(pt.getX());
                totalWeight += pt.getWeight();
                totalDelta += pt.getWeight() * delta * delta;
            }
            var score = totalDelta / totalWeight;
            return score;
        }
        private static CalibrationCurve getCalibrationCurveWithLod(double lod, List<WeightedObservedPoint> weightedObservedPoints)
        {
            var linearPoints = weightedObservedPoints.stream().map(pt -> pt.getX() > lod
                    ? pt : new WeightedObservedPoint(pt.getWeight(), lod, pt.getY()))
                    .collect(Collectors.toList());
            if (linearPoints.stream().map(p -> p.getX()).distinct().count() <= 1)
            {
                return null;
            }
            var calibrationCurve = LINEAR.fit(linearPoints);
            if (null != calibrationCurve.getErrorMessage())
            {
                return null;
            }

            calibrationCurve.setTurningPoint(lod);
            calibrationCurve.setRegressionFit(BILINEAR);
            return calibrationCurve;
        }

        @Override
        public double getY(CalibrationCurve calibrationCurve, double x)
        {
            if (calibrationCurve.hasTurningPoint() && x < calibrationCurve.getTurningPoint())
            {
                x = calibrationCurve.getTurningPoint();
            }
            return super.getY(calibrationCurve, x);
        }

        @Override
        public Double getX(CalibrationCurve calibrationCurve, double y)
        {
            Double x = super.getX(calibrationCurve, y);
            if (x != null && calibrationCurve.hasTurningPoint() && x < calibrationCurve.getTurningPoint()) {
                return null;
            }
            return x;
        }
    };


    private final String name;
    private final String label;

    public RegressionFit(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public CalibrationCurve fit(List<WeightedObservedPoint> points) {
        if (points.size() == 0) {
            CalibrationCurve curve = new CalibrationCurve(this);
            curve.setErrorMessage("Unable to calculate curve, since there are no data points available");
            return curve;
        }

        try {
            CalibrationCurve curve = performFit(points);
            if (curve != null) {
                curve.setRSquared(computeRSquared(curve, points));
            }
            return curve;
        } catch (Exception e) {
            CalibrationCurve curve = new CalibrationCurve(this);
            curve.setErrorMessage(e.toString());
            return curve;
        }
    }

    protected abstract CalibrationCurve performFit(List<WeightedObservedPoint> points);

    public Double computeRSquared(CalibrationCurve curve, List<WeightedObservedPoint> points) {
        SummaryStatistics yValues = new SummaryStatistics();
        SummaryStatistics residuals = new SummaryStatistics();
        for (WeightedObservedPoint point : points) {
            Double yFitted = curve.getY(point.getX());
            if (yFitted == null) {
                continue;
            }
            yValues.addValue(point.getY());
            residuals.addValue(point.getY() - yFitted);
        }
        if (0 == residuals.getN()) {
            return null;
        }
        double yMean = yValues.getMean();
        double totalSumOfSquares = points.stream()
                .mapToDouble(p->(p.getY() - yMean) * (p.getY() - yMean))
                .sum();
        double sumOfSquaresOfResiduals = residuals.getSumsq();
        double rSquared = 1 - sumOfSquaresOfResiduals / totalSumOfSquares;
        return rSquared;
    }

    public static List<RegressionFit> listAll() {
        return Arrays.asList(NONE, LINEAR_THROUGH_ZERO, LINEAR, BILINEAR, QUADRATIC, LINEAR_IN_LOG_SPACE);
    }

    public static RegressionFit parse(String name) {
        if (name == null) {
            return null;
        }
        return listAll().stream()
                .filter(regressionFit->regressionFit.getName().equals(name)).findFirst()
                .orElse(NONE);
    }

    public double getY(CalibrationCurve calibrationCurve, double x)
    {
        return x * calibrationCurve.getSlope() + calibrationCurve.getIntercept();
    }

    @Nullable
    public Double getX(CalibrationCurve  calibrationCurve, double y)
    {
        return (y - calibrationCurve.getIntercept()) / calibrationCurve.getSlope();
    }

    public static class TestCase {
        @Test
        public void testBilinearFit()
        {
            CalibrationCurve calcurve = RegressionFit.BILINEAR.fit(Arrays.asList(LKPALAVILLER_POINTS));
            Assert.assertTrue(calcurve.hasTurningPoint());
            Assert.assertEquals(11673.593881022069, calcurve.getTurningPoint(), 1);
            Assert.assertEquals(1.2771070764E-12, calcurve.getSlope(), 1E-15);
            Assert.assertEquals(-1.4118993633E-08, calcurve.getIntercept(), 1E-12);
        }

        private static final WeightedObservedPoint[] LKPALAVILLER_POINTS = new WeightedObservedPoint[]
        {
            new WeightedObservedPoint(1, 33100.0,4.43587139161e-08),
                    new WeightedObservedPoint(1, 33100.0,1.95494454654e-08),
                    new WeightedObservedPoint(1, 33100.0,2.27918101526e-08),
                    new WeightedObservedPoint(1, 23170.0,2.30656418097e-08),
                    new WeightedObservedPoint(1, 23170.0,5.4350527721e-09),
                    new WeightedObservedPoint(1, 23170.0,1.23134930273e-08),
                    new WeightedObservedPoint(1, 16550.0,1.31851525562e-08),
                    new WeightedObservedPoint(1, 16550.0,3.38514991628e-09),
                    new WeightedObservedPoint(1, 16550.0,7.84140959374e-09),
                    new WeightedObservedPoint(1, 9930.0,6.02807861449e-09),
                    new WeightedObservedPoint(1, 9930.0,4.8092607441e-10),
                    new WeightedObservedPoint(1, 9930.0,2.59774090514e-09),
                    new WeightedObservedPoint(1, 3310.0,7.90722974839e-10),
                    new WeightedObservedPoint(1, 3310.0,9.17255524711e-10),
                    new WeightedObservedPoint(1, 3310.0,4.5257191949e-10),
                    new WeightedObservedPoint(1, 2317.0,1.36493396214e-10),
                    new WeightedObservedPoint(1, 2317.0,6.32731302763e-10),
                    new WeightedObservedPoint(1, 2317.0,3.39855605838e-10),
                    new WeightedObservedPoint(1, 1655.0,1.89132927819e-10),
                    new WeightedObservedPoint(1, 1655.0,1.69052020568e-09),
                    new WeightedObservedPoint(1, 1655.0,1.72719025457e-10),
                    new WeightedObservedPoint(1, 993.0,6.7422496619e-10),
                    new WeightedObservedPoint(1, 993.0,7.82699319274e-10),
                    new WeightedObservedPoint(1, 993.0,1.06796529348e-09),
                    new WeightedObservedPoint(1, 331.0,9.71117065365e-10),
                    new WeightedObservedPoint(1, 331.0,7.33700136294e-10),
                    new WeightedObservedPoint(1, 331.0,1.01713787044e-09),
                    new WeightedObservedPoint(1, 231.7,1.94581859484e-10),
                    new WeightedObservedPoint(1, 231.7,8.71487280515e-10),
                    new WeightedObservedPoint(1, 231.7,1.99592855031e-10),
                    new WeightedObservedPoint(1, 165.5,3.1227318896e-10),
                    new WeightedObservedPoint(1, 165.5,2.01576290459e-09),
                    new WeightedObservedPoint(1, 165.5,0.0),
                    new WeightedObservedPoint(1, 99.3,2.35626363707e-10),
                    new WeightedObservedPoint(1, 99.3,9.74312769554e-10),
                    new WeightedObservedPoint(1, 99.3,0.0),
                    new WeightedObservedPoint(1, 33.1,0.0),
                    new WeightedObservedPoint(1, 33.1,5.53513509328e-10),
                    new WeightedObservedPoint(1, 33.1,1.31377272904e-10),
                    new WeightedObservedPoint(1, 0.0,4.45448077834e-10),
                    new WeightedObservedPoint(1, 0.0,0.0),
                    new WeightedObservedPoint(1, 0.0,4.41809527853e-10)
        };

    }
}
