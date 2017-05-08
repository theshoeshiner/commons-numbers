/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.numbers.gamma;

import org.apache.commons.numbers.fraction.ContinuedFraction;

/**
 * <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
 * Regularized Gamma functions</a>.
 *
 * Class is immutable.
 */
public abstract class RegularizedGamma {
    /** Helper. */
    private static final LogGamma LOG_GAMMA = LogGamma.instance;
    /** Maximum allowed numerical error. */
    private static final double DEFAULT_EPSILON = 1e-15;

    /**
     * @param a Parameter.
     * @param x Argument.
     * @param epsilon When the absolute value of the n-th element in the
     * series is less than epsilon the approximation ceases to calculate
     * further elements in the series.
     * @param maxIterations Maximum number of iterations.
     * @return the value of the function.
     * @throws IllegalArgumentException if the algorithm fails to converge.
     */
    public abstract double value(double a,
                                 double x,
                                 double epsilon,
                                 int maxIterations);

    /**
     * @param a Parameter.
     * @param x Argument.
     * @return the value of the function.
     * @throws IllegalArgumentException if the algorithm fails to converge.
     */
    public abstract double value(double a,
                                 double x);

    /**
     * \( P(a, x) \) <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * regularized Gamma function</a>.
     *
     * Class is immutable.
     */
    public static class P {
        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * {@inheritDoc}
         */
        public double value(double a,
                            double x) {
            return value(a, x, DEFAULT_EPSILON, Integer.MAX_VALUE);
        }

        /**
         * Computes the regularized gamma function \( P(a, x) \).
         *
         * The implementation of this method is based on:
         * <ul>
         *  <li>
         *   <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
         *   Regularized Gamma Function</a>, equation (1)
         *  </li>
         *  <li>
         *   <a href="http://mathworld.wolfram.com/IncompleteGammaFunction.html">
         *   Incomplete Gamma Function</a>, equation (4).
         *  </li>
         *  <li>
         *   <a href="http://mathworld.wolfram.com/ConfluentHypergeometricFunctionoftheFirstKind.html">
         *   Confluent Hypergeometric Function of the First Kind</a>, equation (1).
         *  </li>
         * </ul>
         *
         * {@inheritDoc}
         */
        public double value(double a,
                            double x,
                            double epsilon,
                            int maxIterations) {
            double ret;

            if (Double.isNaN(a) ||
                Double.isNaN(x) ||
                a <= 0 ||
                x < 0) {
                ret = Double.NaN;
            } else if (x == 0) {
                ret = 0;
            } else if (x >= a + 1) {
                // Q should converge faster in this case.
                final RegularizedGamma.Q q = new RegularizedGamma.Q();
                ret = 1 - q.value(a, x, epsilon, maxIterations);
            } else {
                // Series.
                double n = 0; // current element index
                double an = 1 / a; // n-th element in the series
                double sum = an; // partial sum
                while (Math.abs(an / sum) > epsilon &&
                       n < maxIterations &&
                       sum < Double.POSITIVE_INFINITY) {
                    // compute next element in the series
                    n += 1;
                    an *= x / (a + n);

                    // update partial sum
                    sum += an;
                }
                if (n >= maxIterations) {
                    throw new GammaException(GammaException.CONVERGENCE, maxIterations);
                } else if (Double.isInfinite(sum)) {
                    ret = 1;
                } else {
                    ret = Math.exp(-x + (a * Math.log(x)) - LOG_GAMMA.value(a)) * sum;
                }
            }

            return ret;
        }
    }

    /**
     * Creates the \( Q(a, x) \equiv 1 - P(a, x) \) \) <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
     * regularized Gamma function</a>.
     *
     * Class is immutable.
     */
    public static class Q {
        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * {@inheritDoc}
         */
        public double value(double a,
                            double x) {
            return value(a, x, DEFAULT_EPSILON, Integer.MAX_VALUE);
        }

        /**
         * Computes the regularized gamma function \( Q(a, x) = 1 - P(a, x) \).
         *
         * The implementation of this method is based on:
         * <ul>
         *  <li>
         *   <a href="http://mathworld.wolfram.com/RegularizedGammaFunction.html">
         *   Regularized Gamma Function</a>, equation (1).
         *  </li>
         *  <li>
         *   <a href="http://functions.wolfram.com/GammaBetaErf/GammaRegularized/10/0003/">
         *   Regularized incomplete gamma function: Continued fraction representations
         *   (formula 06.08.10.0003)</a>
         *  </li>
         * </ul>
         *
         * {@inheritDoc}
         */
        public double value(final double a,
                            double x,
                            double epsilon,
                            int maxIterations) {
            double ret;

            if (Double.isNaN(a) ||
                Double.isNaN(x) ||
                a <= 0 ||
                x < 0) {
                ret = Double.NaN;
            } else if (x == 0) {
                ret = 1;
            } else if (x < a + 1) {
                // P should converge faster in this case.
                final RegularizedGamma.P p = new RegularizedGamma.P();
                ret = 1 - p.value(a, x, epsilon, maxIterations);
            } else {
                final ContinuedFraction cf = new ContinuedFraction() {
                        /** {@inheritDoc} */
                        @Override
                        protected double getA(int n, double x) {
                            return ((2 * n) + 1) - a + x;
                        }

                        /** {@inheritDoc} */
                        @Override
                        protected double getB(int n, double x) {
                            return n * (a - n);
                        }
                    };

                ret = 1 / cf.evaluate(x, epsilon, maxIterations);
                ret = Math.exp(-x + (a * Math.log(x)) - LOG_GAMMA.value(a)) * ret;
            }

            return ret;
        }
    }
}
