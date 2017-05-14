package perseus.dsp.fft;

import perseus.dsp.maths.Complex;
import java.util.Arrays;

/******************************************************************************
 *  Compute the SimpleFFT and inverse SimpleFFT of a length n complex sequence.
 *  Bare bones implementation that runs in O(n log n) time. Our goal
 *  is to optimize the clarity of the code, rather than performance.
 *
 *  Limitations
 *  -----------
 *   -  assumes n is a power of 2
 *
 *   -  not the most memory efficient algorithm (because it uses
 *      an object type for representing complex numbers and because
 *      it re-allocates memory for the subarray, instead of doing
 *      in-place or reusing a single temporary array)
 *  
 ******************************************************************************/

public class SimpleFFT {
    
    public final static Complex ZERO = new Complex(0, 0);

    // compute the SimpleFFT of x[], assuming its length is a power of 2
    public static Complex[] FFT(Complex[] x) {
        if (x == null) {
            throw new IllegalArgumentException("The provided argument must not be 'null'.");
        }
        
        int nbPoints = x.length;
        if (nbPoints == 1) {
            return new Complex[] { x[0] };
        } else if (nbPoints == 0 || (nbPoints & -nbPoints) != nbPoints) {
            // Fastest technique to check if the number of points is a power of 2 or not
            throw new IllegalArgumentException("The number of points is not a power of 2.");
        }

        // SimpleFFT of even terms
        Complex[] even = new Complex[nbPoints/2];
        for (int i = 0 ; i < nbPoints/2 ; i++) {
            even[i] = x[2*i];
        }
        Complex[] even_fft = FFT(even);    // recursive call

        // SimpleFFT of odd terms (reusing the previous array)
        Complex[] odd  = even;
        for (int i = 0 ; i < nbPoints/2 ; i++) {
            odd[i] = x[2*i + 1];
        }
        Complex[] odd_fft = FFT(odd);    // recursive call

        // Combine odds and evens
        Complex[] fft = new Complex[nbPoints];
        for (int i = 0 ; i < nbPoints/2 ; i++) {
            double ith = -2*i*Math.PI / nbPoints;
            Complex wk  = new Complex(Math.cos(ith), Math.sin(ith));
            fft[i]      = even_fft[i].plus(wk.multiply(odd_fft[i]));
            fft[i+nbPoints/2] = even_fft[i].minus(wk.multiply(odd_fft[i]));
        }
        return fft;
    }

    // Compute the inverse SimpleFFT of x[], assuming its length is a power of 2
    public static Complex[] IFFT(Complex[] x) {
        if (x == null) {
            throw new IllegalArgumentException("The provided argument must not be 'null'.");
        }
        
        int nbPoints = x.length;
        if (nbPoints == 0 || (nbPoints & -nbPoints) != nbPoints) {
            // Fastest technique to check if the number of points is a power of 2 or not
            throw new IllegalArgumentException("The number of points is not a power of 2.");
        }

        Complex[] y = new Complex[nbPoints];

        // Take conjugate
        for (int i = 0 ; i < nbPoints ; i++) {
            y[i] = x[i].conjugate();
        }

        // Compute forward SimpleFFT
        y = FFT(y);

        // Take conjugate again
        for (int i = 0; i < nbPoints; i++) {
            y[i] = y[i].conjugate();
        }

        // Divide by nbPoints
        for (int i = 0; i < nbPoints; i++) {
            y[i] = y[i].scale(1.0 / nbPoints);
        }

        return y;

    }

    /**
     * Computes the circular convolution of x and y
     * @param x The first array of complex numbers : 'x'
     * @param y The second array of complex numbers : 'y'
     * @return The array of computed complex numbers after circular convolution.
     */
    public static Complex[] cconvolve(Complex[] x, Complex[] y) {
        if (x == null || y == null) {
            throw new IllegalArgumentException("The provided arguments must not be 'null'.");
        }

        int nbPointsX = x.length;
        int nbPointsY = y.length;
        if (nbPointsX == 0 || (nbPointsX & -nbPointsX) != nbPointsX) {
            // Fastest technique to check if the number of points is a power of 2 or not
            throw new IllegalArgumentException("The number of points of 'x' must be different from 0 and a power of 2.");
        }
        if (nbPointsY == 0 || (nbPointsY & -nbPointsX) != nbPointsX) {
            // Fastest technique to check if the number of points is a power of 2 or not
            throw new IllegalArgumentException("The number of points of 'y' must be different from 0 and a power of 2.");
        }
        if (x.length != y.length) {
            throw new IllegalArgumentException("Dimensions of 'x' and 'y' must be identical."); 
        }

        // Compute SimpleFFT of each sequence
        Complex[] fft_x = FFT(x);
        Complex[] fft_y = FFT(y);

        // Point-wise multiply
        Complex[] c = new Complex[nbPointsX];
        for (int i = 0; i < nbPointsX; i++) {
            c[i] = fft_x[i].multiply(fft_y[i]);
        }

        // Compute inverse SimpleFFT
        return IFFT(c);
    }

    /**
     * Computes the linear convolution of x and y
     * @param x The first array of complex numbers : 'x'
     * @param y The second array of complex numbers : 'y'
     * @return The array of computed complex numbers after linear convolution.
     */
    public static Complex[] convolve(Complex[] x, Complex[] y) {
        Complex[] a = new Complex[2*x.length];
        System.arraycopy(x, 0, a, 0, x.length);
        Arrays.fill(a, x.length, 2*x.length, ZERO);

        Complex[] b = new Complex[2*y.length];
        System.arraycopy(y, 0, b, 0, y.length);
        Arrays.fill(b, y.length, 2*y.length, ZERO);

        return cconvolve(a, b);
    }

    // display an array of Complex numbers to standard output
    public static void show(Complex[] x, String title) {
        System.out.println(title);
        System.out.println("-------------------");
        for (Complex x1 : x) {
            System.out.println(x1);
        }
        System.out.println();
    }

}
