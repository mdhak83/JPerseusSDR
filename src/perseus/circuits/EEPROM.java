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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Class used to represent the content of the EEPROM, which includes all information about the Perseus HW versioning.
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.1
 */
public class EEPROM {
    
    /* EEPROM response header */
    private byte eeprom_op;
    private byte op_retcode;

    /** Perseus Receiver Serial Number */
    private String serialNumber;
	
    /* Microtelecom Product Code */
    private String productCode;
	
    /* Product hardware version and release */
    private String hardwareVersionRelease;
	
    /* Microtelecom Original Product Signature */
    private String productSignature;
    
    /* Perseus EEPROM Definitions and data structures */
    public static final short ADDR_EEPROM_PRODID = 8;

    /* Perseus EEPROM Definitions and data structures size*/
    public static final byte ADDR_EEPROM_PRODID_SIZE = 12;

    /**
     * Creates an @EEPROM object based on the EEPROM @ByteBuffer requested to the Perseus HW.
     * @param eepromBuffer the EEPROM @ByteBuffer issued by the Perseus HW when requested
     */
    public EEPROM(ByteBuffer eepromBuffer) {
        if (eepromBuffer == null) {
            throw new IllegalArgumentException("The buffer providing the EEPROM data is NULL.");
        }
        eepromBuffer.rewind();
        if (eepromBuffer.remaining() != (ADDR_EEPROM_PRODID_SIZE+2)) {
            throw new IllegalArgumentException("The buffer providing the EEPROM data is not as expected : 14 bytes.");
        }
        this.eeprom_op = eepromBuffer.get();
        this.op_retcode = eepromBuffer.get();
        this.serialNumber = String.format("%05d", eepromBuffer.getShort());
        this.productCode = String.format("0x%04X", eepromBuffer.getShort());
        int release = eepromBuffer.get() & 0x00FF;
        int version = eepromBuffer.get() & 0x00FF;
        this.hardwareVersionRelease = String.format("%01d.%01d", version, release);
        int sig3 = eepromBuffer.getShort() & 0x00FFFF;
        int sig2 = eepromBuffer.getShort() & 0x00FFFF;
        int sig1 = eepromBuffer.getShort() & 0x00FFFF;
        this.productSignature = String.format("%04X-%04X-%04X", sig1, sig2, sig3);
    }
    
    /**
     * Dumps the EEPROM content to the provided @FileWriter.
     * @param fw The output to dump the EEPROM's content to.
     * @throws java.io.IOException
     */
    public void dump(FileWriter fw) throws IOException {
        fw.write("Microtelecom product code: " + this.productCode + "\r\n");
        fw.write("Serial Number            : " + this.serialNumber+"-"+this.productSignature + "\r\n");
        fw.write("Hardware version/revision: " + this.hardwareVersionRelease + "\r\n");
    }

    /**
     * @return the serial number of the Perseus HW
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * @return the product code of the Perseus HW
     */
    public String getProductCode() {
        return productCode;
    }

    /**
     * @return the hardwar version and release of the Perseus HW
     */
    public String getHardwareVersionRelease() {
        return hardwareVersionRelease;
    }

    /**
     * @return the product signature of the Perseus HW
     */
    public String getProductSignature() {
        return productSignature;
    }
    
}
