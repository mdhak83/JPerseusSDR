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
 * Copyright 2017, Mehdi DHAKOUANI
 */
package perseus.circuits;

/**
 * Class used to enumerates all allowed values for attenuation.
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.0
 */
public enum Attenuator {
    /** 0 dB Attenuator */
    _00DB( "0 dB",  0,  0),

    /** 10 dB Attenuator */
    _10DB("10 dB",  1,  10),

    /** 20 dB Attenuator */
    _20DB("20 dB",  2,  20),

    /** 30 dB Attenuator */
    _30DB("30 dB",  3,  30);
    
    /** Name of the attenuator */
    private final String name;

    /** Value used for the Serial IO register */
    private final int sioValue;

    /** dB Value for this attenuator */
    private final int dbValue;

    /** Lowest Significant Bit for the Serial IO interface */
    private final static int SIO_START_BIT = 4;
    
    /**  Number of bits for the Serial IO interface */
    private final static int SIO_NB_BITS = 2;
    
    /**
     * Initialize the @Attenuator enum
     * @param name Name of the attenuator
     * @param sioValue Value used for the SIO register
     * @param dbValue dB Value for this attenuator
     */
    Attenuator(String name, int sioValue, int dbValue) {
        this.name = name;
        this.sioValue = sioValue;
        this.dbValue = dbValue;
    }
    
    /**
     * Method used to find the @Attenuator enum corresponding to a value of attenuation in dB 
     * @param value The attenuation value (as per the Perseus SIO interface) to look for.
     * @return The corresponding @Attenuator enum, or null if not found.
     */
    public static Attenuator Find(int value) {
        Attenuator ret = null;
        for (Attenuator a : Attenuator.values()) {
            if (a.sioValue == value) {
                ret = a;
                break;
            }
        }
        return ret;
    }

    /**
     * Method used to find the @Attenuator enum corresping to a value of attenuation in dB 
     * @param value The attenuation in dB to look for.
     * @return The corresponding @Attenuator enum, or null if not found.
     */
    public static Attenuator FindDB(int value) {
        Attenuator ret = null;
        for (Attenuator a : Attenuator.values()) {
            if (a.dbValue == value) {
                ret = a;
                break;
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
     * @return the name of the attenuator
     */
    public String getName() {
        return name;
    }

    /**
     * @return the name of the attenuator
     */
    public int getDbValue() {
        return dbValue;
    }

    /**
     * @return the value of the attenuator (as per Perseus SIO interface)
     */
    public int getSIOValue() {
        return sioValue;
    }
    
}
