package perseus.circuits;

import static perseus.utils.Definitions.PERSEUS_ADCSAMPLINGFREQKHZ;
import static perseus.utils.Definitions.PERSEUS_CMD_FPGASIO;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class represents the FX2 MCU <-> FPGA serial IO data structure
 * 
 * The JPerseusSDR v1.0.0 library is free software; you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either version 
 * 3.0 of the License, or (at your option) any later version.
 * 
 * The JPerseusSDR v1.0.0 library is distributed in the hope that it will
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
 * @author Mehdi DHAKOUANI
 * Copyright 2017, Mehdi DHAKOUANI
 * @version 1.0.0
 */
public class SIOControl {
    
    /** Receiver control bit masks (control field of SIOCTL data structure) */

    /** Enable inbound data USB endpoint */
    private static final byte SIO_FIFOEN =    (byte) 0x01;
    
    /** Enable ADC dither generator */
    private static final byte SIO_DITHER =    (byte) 0x02;
    
    /** Enable ADC preamplifier */
    private static final byte SIO_GAINHIGH =  (byte) 0x04;
    
    /** For Microtelecom internal use only */
    private static final byte SIO_RESERVED =  (byte) 0x08;
    
    /** For Microtelecom internal use only */
    private static final byte SIO_TESTFIFO =  (byte) 0x10;

    /** For Microtelecom internal use only */
    private static final byte SIO_ADCBYPASS = (byte) 0x20;

    /** For Microtelecom internal use only */
    private static final byte SIO_ADCATT60 =  (byte) 0x40;

    /** For Microtelecom internal use only */
    private static final byte SIO_SCRAMBLE =  (byte) 0x80;

    /** Variable that holds the value of FIFO enabling */
    private boolean enableFIFOEN = false;

    /** Variable that holds the value of dithering generation enabling */
    private boolean enableDither = false;

    /** Variable that holds the value of preamplification enabling */
    private boolean enablePreamp = false;
    
    /** Variable that holds the value of the central frequency to be used (for narrowband mode only) */
    private double frequency = 0;
    
    public SIOControl() {
    }

    public void enableFIFOEN(boolean enable) {
        this.enableFIFOEN = enable;
    }
    
    public void enableDither(boolean enable) {
        this.enableDither = enable;
    }
    
    public void enablePreamp(boolean enable) {
        this.enablePreamp = enable;
    }
    
    public void setFrequencyHz(double frequency) {
	if (frequency < 0 || frequency >= (PERSEUS_ADCSAMPLINGFREQKHZ*1000/2)) {
            throw new IllegalArgumentException("Frequency is out of the range allowed by the Perseus hardware.");
        }
        this.frequency = frequency;
    }
    
    public ByteBuffer getValue(boolean wideband) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(wideband ? 5 : 6);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(PERSEUS_CMD_FPGASIO);
        buffer.put((byte) ((this.enableFIFOEN ? SIO_FIFOEN : 0) | (this.enableDither ? SIO_DITHER : 0) | (this.enablePreamp ? SIO_GAINHIGH : 0)));
        if (wideband) {
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.put((byte) 0);
        } else {
            // Compute and set the value of the frequency register of the Perseus 32-bits phase accumulator with the usual formula FReg = F/Fsampling*(2^32)
            buffer.putInt((int) ((this.frequency / (PERSEUS_ADCSAMPLINGFREQKHZ * 1000)) * Math.pow(2, 32)) & 0x00FFFFFFFF);
        }
        return buffer;
    }

    /**
     * @return the enableDither
     */
    public boolean isDitherEnabled() {
        return enableDither;
    }

    /**
     * @return the enablePreamp
     */
    public boolean isGainHighEnabled() {
        return enablePreamp;
    }

    /**
     * @return the frequency
     */
    public double getFrequency() {
        return frequency;
    }
    
}
