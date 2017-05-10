package perseus.circuits;

/**
 * Class enumerating all values for the Preselector Filter offered by the PErseus Hardware.
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
public enum PreselectorFilter {

    /** Low-pass filter &lt; 1.7 MHz */
    FILTER_1    ("Low-Pass Filter <  1.7 MHz",   0,      1700000),

    /** Low-pass filter &lt; 2.1 MHz */
    FILTER_2    ("Low-Pass Filter <  2.1 MHz",   1,      2100000),

    /** Low-pass filter &lt; 3.0 MHz */
    FILTER_3    ("Low-Pass Filter <  3.0 MHz",   2,      3000000),

    /** Low-pass filter &lt; 4.2 MHz */
    FILTER_4    ("Low-Pass Filter <  4.2 MHz",   3,      4200000),

    /** Low-pass filter &lt; 6.0 MHz */
    FILTER_5    ("Low-Pass Filter <  6.0 MHz",   4,      6000000),

    /** Low-pass filter &lt; 8.5 MHz */
    FILTER_6    ("Low-Pass Filter <  8.5 MHz",   5,      8500000),

    /** Low-pass filter &lt; 12 MHz */
    FILTER_7    ("Low-Pass Filter < 12.0 MHz",   6,     12000000),

    /** Low-pass filter &lt; 17 MHz */
    FILTER_8    ("Low-Pass Filter < 17.0 MHz",   7,     17000000),

    /** Low-pass filter &lt; 24 MHz */
    FILTER_9    ("Low-Pass Filter < 24.0 MHz",   8,     24000000),

    /** Low-pass filter &lt; 32 MHz */
    FILTER_10   ("Low-Pass Filter < 32.0 MHz",   9,     32000000),

    /** No filter --> Wideband 0-40 MHz */
    FILTER_WB   ("No filter (Wideband)",        10,         null);
    
    /** Lowest Significant Bit for the Serial IO interface */
    private final static int SIO_START_BIT = 0;
    
    /**  Number of bits for the Serial IO interface */
    private final static int SIO_NB_BITS = 4;
    
    /** Name of the preselector filter */
    private final String name;
    
    /** Value used for the Serial IO register */
    private final int sioValue;

    /** Upper cutoff frequency of the preselections filter in Hz */
    private final Integer upperCutoffFrequencyHz;
    
    /**
     * Initialize the @PreselectorFilter enum
     * @param name Name of the preselector filter
     * @param sioValue Value used for the SIO register
     * @param upperCutoffFrequencyHz Upper cutoff frequency of the preselections filter in Hz
     */
    PreselectorFilter(String name, int sioValue, Integer upperCutoffFrequencyHz) {
        this.name = name;
        this.sioValue = sioValue;
        this.upperCutoffFrequencyHz = upperCutoffFrequencyHz;
    }
    
    /**
     * Method used to find the @PreselectorFilter enum corresponding to a frequency in Hz
     * @param frequencyHz The frequency to match the @PreselectorFilter to.
     * @return The corresponding @PreselectorFilter enum, or null if not found.
     */
    public static PreselectorFilter Find(Double frequencyHz) {
        PreselectorFilter ret = null;
        if (frequencyHz == null) {
            ret = PreselectorFilter.FILTER_WB;
        } else {
            for (PreselectorFilter filter : PreselectorFilter.values()) {
                if (filter.upperCutoffFrequencyHz == null) {
                    continue;
                }
                if (filter.upperCutoffFrequencyHz > frequencyHz && (ret == null || filter.sioValue < ret.sioValue)) {
                    ret = filter;
                }
            }
        }
        return ret;
    }
    
    /**
     * Method used to get the AND-bits mask for this class.
     * @return The corresponding AND-bit mask.
     */
    public static int GetMask() {
        return ((2 << SIO_NB_BITS) - 1) << SIO_START_BIT;
    }
    
    /**
     * Method used to get the masked value for this class.
     * @return The corresponding bit masked value.
     */
    public int getMaskedValue() {
        return (this.sioValue << SIO_START_BIT) & GetMask();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
}