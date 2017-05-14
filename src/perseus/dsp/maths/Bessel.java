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
package perseus.dsp.maths;

/**
 * Class used to compute Bessel functions of order 0
 *  (based on Abramowitz and Stegun - Handbook of Mathematical Functions - Page 378 - §9.8.1 and §9.8.2)
 * Approximately 28,6% faster than 'cern.jet.math.tdouble.Bessel' functions.
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.1
 */
public class Bessel {
    
    /**
     * Coefficients for I0(x) with t=x/3.75 when -3.75 ≤ x ≤ 3.75
     *   I0(x) = 1 + 3.5156229*t^2 + 3.0899424*t^4 + 1.2067492*t^6 + 0.2659732*t^8 + 0.0360768*t^10 + 0.0045813*t^12 + ε
     *   and |ε| &lt; 1.6*10^-7
     */ 
    private static final double[] I0_COEFF1 = {
        3.5156229,
        3.0899424,
        1.2067492,
        0.2659732,
        0.0360768,
        0.0045813
    };
    
    /**
     * Coefficients for I0(x) with t=x/3.75 when 3.75 ≤ x &lt; ∞
     *   I0(x) = exp^x / x^-.5 * ( 0.39894228 + 0.01328592*t^-1 + 0.00225319*t^-2 - 0.00157565*t^-3 + 0.00916281*t^-4 - 0.02057706*t^-5 + 0.02635537*t^-6 - 0.01647633*t^-7 + 0.00392377*t^-8 + ε
     *   and |ε| &lt; 1.9*10^-7
     */ 
    private static final double[] I0_COEFF2 = {
         0.39894228,
         0.01328592,
         0.00225319,
        -0.00157565,
         0.00916281,
        -0.02057706,
         0.02635537,
        -0.01647633,
         0.00392377
    };
    
    /**
     * Coefficients for I1(x) with t=x/3.75 when -3.75 ≤ x ≤ 3.75
     *   I1(x) = x * (0.5 + 0.87890594*t^2 + 0.51498869*t^4 + 0.15084934*t^6 + 0.02658733*t^8 + 0.00301532*t^10 + 0.00032411*t^12 + ε)
     *   and |ε| &lt; 8*10^-9
     */ 
    private static final double[] I1_COEFF1 = {
        0.5,
        0.87890594,
        0.51498869,
        0.15084934,
        0.02658733,
        0.00301532,
        0.00032411
    };
    
    /**
     * Coefficients for I1(x) with t=x/3.75 when 3.75 ≤ x &lt; ∞
     *   I1(x) = exp^x / x^-.5 * ( 0.39894228 - 0.03988024*t^-1 - 0.00362018*t^-2 + 0.00163801*t^-3 - 0.01031555*t^-4 + 0.02282967*t^-5 - 0.02895312*t^-6 + 0.01787654*t^-7 - 0.00420059*t^-8 + ε
     *   and |ε| &lt; 2.2*10^-7
     */ 
    private static final double[] I1_COEFF2 = {
         0.39894228,
        -0.03988024,
        -0.00362018,
         0.00163801,
        -0.01031555,
         0.02282967,
        -0.02895312,
         0.01787654,
        -0.00420059
    };
    
    public static double I0(double x) {
        x = (x < 0 ? -x : x);
        double t = x / 3.75;
        double tn, ret;
        
        if (t < 1) {
            ret = 1;
            tn = 1;
            for (int i = 1 ; i <= I0_COEFF1.length ; i++) {
                tn *= t*t;
                ret += I0_COEFF1[i-1] * tn;
            }
        } else {
            ret = I0_COEFF2[0];
            tn = 1;
            for (int i = 2 ; i <= I0_COEFF2.length ; i++) {
                tn /= t;
                ret += I0_COEFF2[i-1] * tn;
            }
            ret *= Math.exp(x)/Math.sqrt(x);
        }
        return ret;
    }
    
    public static double I1(double x) {
        boolean neg = x<0;
        x = (x < 0 ? -x : x);
        double t = x / 3.75;
        double tn, ret;
        
        if (t < 1) {
            ret = I1_COEFF1[0];
            tn = 1;
            for (int i = 2 ; i <= I1_COEFF1.length ; i++) {
                tn *= t*t;
                ret += I1_COEFF1[i-1] * tn;
            }
            ret *= x;
        } else {
            ret = I1_COEFF2[0];
            tn = 1;
            for (int i = 2 ; i <= I1_COEFF2.length ; i++) {
                tn /= t;
                ret += I1_COEFF2[i-1] * tn;
            }
            ret *= Math.exp(x)/Math.sqrt(x);
        }
        return (neg ? -ret : ret);
    }
    
    public static void main(String[] args) {
        System.out.println("\r\n\r\nI0:");
        for (double d = -5 ; d <= 10 ; d += 0.5) {
            double ret0 = I0(d);
            double ret1 = cern.jet.math.tdouble.Bessel.i0(d);
            double err = (ret0-ret1)/ret1*100;
            System.out.println(String.format("%2.1f %6.6f %6.6f %2.9f%%", d, ret0, ret1, err));
        }
        System.out.println("\r\n\r\nI1:");
        for (double d = -5 ; d <= 10 ; d += 0.5) {
            double ret0 = I1(d);
            double ret1 = cern.jet.math.tdouble.Bessel.i1(d);
            double err = (ret0-ret1)/ret1*100;
            System.out.println(String.format("%2.1f %6.6f %6.6f %2.9f%%", d, ret0, ret1, err));
        }
        double diffmean = 0;
        double diffmean2 = 0;
        for (int j = 0 ; j < 100 ; j++) {
            int length = 1000000;
            double[] ds = new double[length];
            for (int i = 0 ; i < ds.length ; i++) {
                ds[i] = (Math.random()-0.5) * 20;
            }
            long t0 = System.currentTimeMillis();
            for (int i = 0 ; i < length ; i++) {
                I0(ds[i]);
            }
            long t1 = System.currentTimeMillis();
            long t2 = System.currentTimeMillis();
            for (int i = 0 ; i < length ; i++) {
                cern.jet.math.tdouble.Bessel.i0(ds[i]);
            }
            long t3 = System.currentTimeMillis();
            double diff = (double) ((t1-t0)-(t3-t2))/(t3-t2)*100;
            diffmean += diff;
            System.out.println(String.format("%6d %6d %2.4f", (t1-t0), (t3-t2), diff));
            t0 = System.currentTimeMillis();
            for (int i = 0 ; i < length ; i++) {
                I1(ds[i]);
            }
            t1 = System.currentTimeMillis();
            t2 = System.currentTimeMillis();
            for (int i = 0 ; i < length ; i++) {
                cern.jet.math.tdouble.Bessel.i1(ds[i]);
            }
            t3 = System.currentTimeMillis();
            diff = (double) ((t1-t0)-(t3-t2))/(t3-t2)*100;
            diffmean2 += diff;
        }
        System.out.println(String.format("I0: %2.4f", diffmean/100));
        System.out.println(String.format("I1: %2.4f", diffmean/100));
    }
    
}
