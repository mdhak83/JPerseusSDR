package perseus.circuits;

import java.util.Arrays;

/**
 * Class used to represent a @Firmware block of data.
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
public class FirmwareBlock {
    
    private int requestValue = 0;
    private byte[] content;
    private Byte checksum = 0;
    
    protected FirmwareBlock(int requestValue, byte[] content, Byte checksum) {
        if (content != null && content.length > 128) {
            throw new IllegalArgumentException("The provided block exceeds the maximum size of 128 bytes.");
        }
        this.requestValue = requestValue;
        this.content = content;
        this.checksum = checksum;
    }

    /**
     * @return the requestValue
     */
    public int getRequestValue() {
        return requestValue;
    }

    /**
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * @return the checksum
     */
    public byte getChecksum() {
        return checksum;
    }
    
    @Override
    public String toString() {
        String ret = "Command: " + Integer.toHexString(this.requestValue);
        ret += " ; Length: " + (this.content != null ? this.content.length : 0);
        ret += " ; Block: " + (this.content != null ? "'" + Arrays.toString(this.content) + "'" : "''");
        return ret;
    }
    
}
