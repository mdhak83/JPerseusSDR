package perseus.circuits;

/**
 * Class used to represent the status and frequency applicable to the Preselector of the Perseus HW.
 * 
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
 * @author Mehdi DHAKOUANI
 * Copyright 2017, Mehdi DHAKOUANI
 * @version 1.0.0
 */
public class Preselector {
    
    private PreselectorFilter filter = null;
    
    public Preselector() {
    }
    
    /**
     * Sets the Preselector filter to be used by the Perseus instance
     * @param filter The @PreselectorFilter to be used by the Perseus instance
     * @throws IllegalArgumentException if the provided filter is NULL
     * @return true if the filter was modified, false otherwise.
     */
    public boolean set(PreselectorFilter filter) {
        boolean ret = false;
        if (filter == null) {
            throw new IllegalArgumentException("Preselector Filter cannot be null.");
        }
        ret = (this.filter != filter);
        this.filter = filter;
        return ret;
    }
    
    /**
     * Reset the Preselector filter to a specific value
     * @param filter The value to reset the preselector to (or NULL).
     */
    public void resetTo(PreselectorFilter filter) {
        this.filter = filter;
    }
    
    /**
     * Sets the Preselector filter to be used by the Perseus instance based upon a center frequency in Hz.
     * @param frequency The center frequency to be used to find the correct Low-Pass Filter to be applied.
     * @throws IllegalArgumentException if the provided filter is NULL
     * @return true if the filter was modified, false otherwise.
     */
    public boolean setFrequency(double frequency) {
        PreselectorFilter foundFilter = PreselectorFilter.Find(frequency);
        if (foundFilter == null) {
            throw new IllegalArgumentException("Provided center frequency is out of range.");
        }
        boolean ret = (this.filter == null || foundFilter != this.filter);
        this.filter = foundFilter;
        return ret;
    }

    /**
     * @return the current @PreselectorFilter
     */
    public PreselectorFilter getFilter() {
        return filter;
    }
    
}
