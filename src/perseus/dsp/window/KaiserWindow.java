/**
 * The JPerseusSDR library is free software; you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either version 
 * 3.0 of the License, or (at your option) any later version.
 * 
 * The JPerseusSDR library is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with the Perseus SDR Library; 
 * if not, see 'http://www.gnu.org/licenses/'.
 * 
 * This library is assumed to work under J2SE &gt;= 7.
 * NOTE: It is assumed that the Perseus USB drivers are already installed on your system. The Perseus USB drivers 
 *       are available in the distribution CD-ROM which comes with the Perseus receiver.
 * 
 * This library is inspired from the Microtelecom Software Defined Radio Developer Kit (SDRDK) and is subject to licensing
 *  conditions contained in the document "SDRDK-Licensing-Agreement-v20.docx" (available under the folder "/resources")
 *  Microtelecom SDRDK is (c) 2007-2013, Microtelecom s.r.l. - Pavia di Udine, Italy
 *  Author : Nicolangelo PALERMO - IV3NWV
 * 
 * This library is also inspired from the Perseus Software Defined Radio Control Library for Linux (libperseus-sdr),
 *  originally written by Nicolangelo PALERMO and maintained by Andrea MONTEFUSCO. This library can be found at :
 *  - https://github.com/Microtelecom/libperseus-sdr
 * 
 * The JPerseus library, source code and documentation is published under the GNU Lesser General Public Licence v3
 *  (available under the folder "/resources").
 * 
 * Copyright 2017, Mehdi DHAKOUANI
 */
package perseus.dsp.window;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import perseus.dsp.maths.Bessel;
import perseus.dsp.maths.Complex;

/**
 * This class computes the filter coefficients of of the n-points Kaiser window based on the number of points and the β parameter.
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.1
 * 
 */
public class KaiserWindow extends Window {
    
    protected static final String[] ALIASES = { "KAISER", "KAISERBESSEL" };
    protected static final String[] PARAMETERS = { "NBPOINTS", "BETA" };

    private static final Map<Integer, KaiserWindow> WINDOWS = new HashMap<>();

    private Double beta;
    private Integer nbPoints;
    private double[] result;
    
    static {
        Window.Register(HannWindow.class, ALIASES, PARAMETERS);
    }
    
    private KaiserWindow() {
    }

    /** Creates a Kaiser window and computes the filter coefficients of of the n-point Kaiser window based on the number of points  and the β parameter.
     *  The main lobe width, in between the nulls, is given by Math.sqrt(1+α²),  in units of Discrete Fourier Transform bins, and a typical value of α is 3.
     *  https://en.wikipedia.org/wiki/Window_function#Kaiser_window
     * If the window was already created, retrieves the existing one.
     * Examples of KaiserWindow creation delays (Windows 10 x64 Intel Core i7-4700HQ 2.40GHz Java 8 x64)
     *      - dimension=   4096 =   1.50 ms
     *      - dimension=   8192 =   2.54 ms
     *      - dimension=  16384 =   5.14 ms
     *      - dimension=  32768 =  10.16 ms
     *      - dimension=  65536 =  20.18 ms
     *      - dimension= 131072 = 117.46 ms
     *      - dimension= 262144 = 206.28 ms
     *      - dimension= 524288 = 393.46 ms
     *      - dimension=1048576 = 765.22 ms
     * @param beta the beta parameter for the FIR filter (beta=PI*α)
     * @param nbPoints the number of points of the Kaiser window (must be a power of 2)
     * @return the generated Kaiser Window (or the pre-existing one)
     */
    public static KaiserWindow GetWindow(Integer nbPoints, Double beta) {
        if (nbPoints <= 1) {
            throw new IllegalArgumentException("The number of samples provided must fit future FFT processing : 'nbSamples' has to be a positive integer > 1.");
        } else if ((nbPoints & -nbPoints) != nbPoints) {
            throw new IllegalArgumentException("The number of samples provided must fit future FFT processing : 'nbSamples' has to be a power of 2.");
        }
        KaiserWindow window = new KaiserWindow();
        window.beta = beta;
        window.nbPoints = nbPoints;
        int hashcode = window.hashCode();
        if (WINDOWS.containsKey(hashcode)) {
            return WINDOWS.get(hashcode);
        }

        window.result = new double[window.nbPoints];
        
        // Generates a Kaiser window function and normalize window area to 1
        int windowSize = window.result.length;
        double kwinsum = 0;
        for (int n = 0 ; n < windowSize ; n++) {
            double temp = 2 * beta * Math.sqrt(n * (windowSize - 1 - n)) / (windowSize-1);
            double coefficient = Bessel.I0(temp) / Bessel.I0(beta);
            window.result[n] = coefficient;
            kwinsum += coefficient;
        }
        for (int k = 0 ; k < windowSize ; k++) {
            window.result[k] /= kwinsum;
        }
        WINDOWS.put(window.hashCode(), window);
        return window;
    }

    /**
     * @return the result of the windowing processing
     */
    @Override
    public double[] getResult() {
        return result;
    }
    
    /* Applies the Kaiser Window to the given input and provides the result as an array of double
     * Examples of windowing delays (Windows 10 x64 Intel Core i7-4700HQ 2.40GHz Java 8 x64) :
     *  dimension=   4096 =  0.18 ms
     *  dimension=   8192 =  0.15 ms
     *  dimension=  16384 =  0.30 ms
     *  dimension=  32768 =  0.74 ms
     *  dimension=  65536 =  1.48 ms
     *  dimension= 131072 =  2.62 ms
     *  dimension= 262144 =  5.57 ms
     *  dimension= 524288 = 10.55 ms
     *  dimension=1048576 = 20.35 ms
     */
    @Override
    public double[] windowInputToDouble(double[] buffer) {
        if (buffer.length != this.nbPoints) {
            throw new IllegalArgumentException("The number of samples provided must match the Kaiser-window input parameter : \"number of points\" per buffer");
        }
        double[] ret = new double[buffer.length];
        for (int i = 0 ; i < buffer.length ; i++) {
            ret[i] = buffer[i] / (this.nbPoints-1); // Normalize samples to unit
            ret[i] = ret[i] * this.result[i];     // Applies Kaiser window
        }
        return ret;
    }
    
    /* Applies the Kaiser Window to the given input and provides the result as an array of Complex objects
     * Examples of windowing delays (Windows 10 x64 Intel Core i7-4700HQ 2.40GHz Java 8 x64):
     *  dimension=   4096 =  0.25 ms
     *  dimension=   8192 =  0.28 ms
     *  dimension=  16384 =  0.38 ms
     *  dimension=  32768 =  0.92 ms
     *  dimension=  65536 =  1.91 ms
     *  dimension= 131072 =  3.56 ms
     *  dimension= 262144 =  6.57 ms
     *  dimension= 524288 = 13.12 ms
     *  dimension=1048576 = 23.36 ms
     */
    @Override
    public Complex[] windowInputToComplex(double[] buffer) {
        if (buffer.length != this.nbPoints) {
            throw new IllegalArgumentException("The number of samples provided must match the Kaiser-window input parameter : \"number of points\" per buffer");
        }
        Complex[] ret = new Complex[buffer.length];
        for (int i = 0 ; i < buffer.length ; i++) {
            ret[i] = new Complex(buffer[i] / (this.nbPoints-1) * this.result[i], 0);    // Normalize samples to unit and applies Kaiser window
        }
        return ret;
    }
    
    /**
     * Computes the hashCode of this KaiserWindow
     * @return the hashCode = fn(nbPoints,beta)
     */
    @Override
    public int hashCode() {
        int x = 13;
        x = 7 * x + this.beta.hashCode();
        x = 7 * x + this.nbPoints;
        return x;
    }

    /**
     * Compares this with obj
     * @return true if the 2 objects are the same.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KaiserWindow other = (KaiserWindow) obj;
        if (!Objects.equals(this.beta, other.beta)) {
            return false;
        }
        return Objects.equals(this.nbPoints, other.nbPoints);
    }
    
}