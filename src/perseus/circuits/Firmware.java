package perseus.circuits;

import perseus.utils.XResource;
import perseus.utils.XTools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to enumerate and preload both firmwares (Narroband and Wideband) usable with the Perseus Hardware.
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
 * Please note that :
 * - "FX2" firmware converts x kHz (-x/2 &lt; Fc &lt; +x/2) around central frequency (NarrowBand ADC)
 * - "WB"  firmware converts the whole 0-40 MHz band (Wideband ADC)
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
public enum Firmware {
    
    //Each firmware refers to a file located in the 'Resources' folder
    FX2 ("perseus_default_ds16v21.hex"),    // The default firmware enum for the Perseus
    WB  ("perseus_wb_ds16v21.hex");         // The wideband firmware enum for the Perseus
    
    /* Firmware files are supposed to be formatted as follows :
        :ssvvvvccb0b1b2...bXCC      where :
        ':' :   The colon sign marks the beginninf of the row ; all leading and trailing spaces are removed within the code
        'ss':   The number of data bytes to be downloaded
        'vvvv': The request value to be used for USB transaction (along with bmRequestType=0x40 and bRequest=0xA0)
        'cc':   The FPGA command (00 = load ; 01 = end)
        'bX':   the Xth byte of data to be downloaded
        'CC':   An optional checksum byte (not used for now)
        [CR|LF|CRLF] : marks the end of a block to be transmitted
    */
    
    private String filename = null;
    private File file = null;
    private final List<FirmwareBlock> blocks = new ArrayList<>();
    
    private Firmware(String filename) {
        XResource res;
        try {
            res = XResource.GetInstance();
            this.filename = filename;
            File f = res.getResourceFile(filename);
            if (f == null) {
                throw new IllegalArgumentException("Firmware '" + filename + "' is not available.");
            }
            this.file = f;
            this.load();
        } catch (IOException ex) {
            Logger.getLogger(Firmware.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void load() {
        try {
            FileReader fr = new FileReader(this.file);
            try (BufferedReader br = new BufferedReader(fr)) {
                String row;
                int rowCount = 1;
                while ((row = br.readLine()) != null) {
                    row = row.trim().toUpperCase();
                    if (!row.isEmpty()) {
                        if (row.startsWith(":")) {
                            row = row.substring(1);
                            if (row.length() >= 2) {
                                int nbBytes = Integer.parseInt(row.substring(0, 2), 16);
                                if (row.length() < (2 * nbBytes) + 4 + 2 + 2) {
                                    throw new IllegalArgumentException("Firmware file '" + filename + "' : Syntax error on line " + rowCount + ".");
                                } else {
                                    int command = (XTools.ConvertHexString(row.substring(2, 4)) & 0x00FF) << 8 | (XTools.ConvertHexString(row.substring(4, 6)) & 0x00FF);
                                    int col = 8;
                                    byte[] blockContent = null;
                                    if (nbBytes != 0) {
                                        blockContent = new byte[nbBytes];
                                        for (int i = 0 ; i < nbBytes ; i++) {
                                            blockContent[i] = XTools.ConvertHexString(row.substring(col, col+2));
                                            col += 2;
                                        }
                                    }
                                    Byte checksum = null;
                                    if (row.length() == (col+2)) {
                                        checksum = XTools.ConvertHexString(row.substring(col, col+2));
                                    }
                                    this.blocks.add(new FirmwareBlock(command, blockContent, checksum));
                                }
                            } else {
                                throw new IllegalArgumentException("Firmware file '" + filename + "' : Syntax error on line " + rowCount + ".");
                            }
                        } else if (!row.startsWith("#")) {
                            throw new IllegalArgumentException("Firmware file '" + filename + "' : Syntax error on line " + rowCount + ".");
                        }
                    }
                    rowCount++;
                }
            }
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("Firmware file '" + filename + "' was not found.");
        } catch (IOException ex) {
            throw new IllegalArgumentException("Firmware file '" + filename + "' cannot be opened.");
        }
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the blocks
     */
    public List<FirmwareBlock> getBlocks() {
        return blocks;
    }
    
}
