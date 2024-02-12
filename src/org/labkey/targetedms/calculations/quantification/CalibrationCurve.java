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

import org.jetbrains.annotations.Nullable;

public class CalibrationCurve implements Cloneable {
    private Double slope;
    private Double intercept;
    private Double turningPoint;
    private Integer pointCount;
    private Double quadraticCoefficient;
    private Double rSquared;
    private String errorMessage;
    private RegressionFit regressionFit;

    public CalibrationCurve(RegressionFit regressionFit) {
        this.regressionFit = regressionFit;
    }

    public double getSlope() {
        return slope == null ? 0 : slope;
    }

    public boolean hasSlope() {
        return slope != null;
    }

    public void setSlope(Double slope) {
        this.slope = slope;
    }

    public double getIntercept() {
        return intercept == null ? 0 : intercept;
    }


    public boolean hasIntercept() {
        return intercept != null;
    }

    public void setIntercept(Double intercept) {
        this.intercept = intercept;
    }
    public double getTurningPoint()
    {
        return turningPoint  == null ? 0 : turningPoint;
    }
    public boolean hasTurningPoint() {
        return turningPoint != null;
    }
    public void setTurningPoint(Double turningPoint)
    {
        this.turningPoint = turningPoint;
    }

    public int getPointCount() {
        return pointCount == null ? 0 : pointCount;
    }

    public boolean hasPointCount() {
        return pointCount != null;
    }

    public void setPointCount(Integer pointCount) {
        this.pointCount = pointCount;
    }

    public double getQuadraticCoefficient() {
        return quadraticCoefficient == null ? 0 : quadraticCoefficient;
    }

    public boolean hasQuadraticCoefficient() {
        return quadraticCoefficient != null;
    }

    public void setQuadraticCoefficient(Double quadraticCoefficient) {
        this.quadraticCoefficient = quadraticCoefficient;
    }

    public Double getRSquared() {
        return rSquared;
    }

    public void setRSquared(Double rSquared) {
        this.rSquared = rSquared;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Double getY(double x)
    {
        return regressionFit.getY(this, x);
    }

    @Nullable
    public Double getX(double y)
    {
        return regressionFit.getX(this, y);
    }

    public RegressionFit getRegressionFit() {
        return regressionFit;
    }

    public void setRegressionFit(RegressionFit regressionFit) {
        this.regressionFit = regressionFit;
    }

    public static CalibrationCurve forNoExternalStandards() {
        CalibrationCurve calibrationCurve = new CalibrationCurve(RegressionFit.NONE);
        calibrationCurve.setPointCount(0);
        calibrationCurve.setSlope(1.0);
        return calibrationCurve;
    }
}
