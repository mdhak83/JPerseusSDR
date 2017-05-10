package perseus.utils;

import java.io.File;

/**
 * Generic definitions associated with the Perseus HW
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
 * The JPerseus v1.0.0 library, source code and documentation is published under the GNU Lesser General Public Licence v3
 *  (available under the folder "/resources").
 * 
 * @author Mehdi DHAKOUANI
 * Copyright 2017, Mehdi DHAKOUANI
 * @version 1.0.0
 * 
 */
public class Definitions {

    public static final String PROJECT_NAME = "JPerseusSDR";
    public static final String PROJECT_VERSION = "v1.0.0";
    
    // Directory where all FPGA and Firmware files will be stored
    public static final String RESOURCES_PATH = System.getProperty("user.home") + File.separator + PROJECT_NAME + "." + PROJECT_VERSION;
    
    public static final int USB_BASE_TRANSFER  = 1024;
    
    // Perseus receiver USB buffering scheme definitions (never touch)
    public static final int PERSEUS_SAMPLESPERFRAME =       170;    // Samples contained in one FX2 USB transaction
    public static final int PERSEUS_COMPONENTSPERSAMPLE =   2;      // I & Q components
    public static final int PERSEUS_BYTESPERCOMPONENT =     3;      // 24 bit data

    // Derived defines
    public static final int PERSEUS_BYTESPERSAMPLE = (PERSEUS_BYTESPERCOMPONENT*PERSEUS_COMPONENTSPERSAMPLE);   // Currently 6 bytes

    // When calling PerseusStartAsyncInput, the buffer size must always be a multiple of PERSEUS_BYTESPERFRAME
    public static final int PERSEUS_BYTESPERFRAME = (PERSEUS_SAMPLESPERFRAME*PERSEUS_BYTESPERSAMPLE);           // Currently 1.020 bytes


    // Nominal ADC sampling frequency
    public static final int PERSEUS_ADC_CLK_FREQ = 80000000;

    // Minimum DDC frequency
    public static final int PERSEUS_DDC_FREQ_MIN = 0;
    
    // Maximum DDC frequency
    public static final int PERSEUS_DDC_FREQ_MAX = PERSEUS_ADC_CLK_FREQ/2;

    // Perseus instances maximum number
    public static final int PERSEUS_MAX_NR =    8;

    // Perseus Timeouts
    public static final int FX2_TIMEOUT =   1000;
    
    // Perseus SDR Vendor/Product Identifiers
    public static final int PERSEUS_VENDOR_ID =         0x04B4;
    public static final int PERSEUS_PRODUCT_ID =        0x325C;
    public static final int PERSEUS_PID_BLANKEEPROM =   0x8613;

    // Perseus Fx2 firmware end points
    public static final byte PERSEUS_EP_CMD =       (byte) 0x01;
    public static final byte PERSEUS_EP_STATUS =    (byte) 0x81;
    public static final byte PERSEUS_EP_DATAIN =    (byte) 0x82;

    // Perseus Fx2 MCU specific vendor command defines
    public static final byte FX2_BM_VENDOR_REQUEST =        (byte) 0x40;
    public static final byte FX2_REQUEST_FIRMWARE_LOAD =    (byte) 0xA0;
    public static final short FX2_ADDR_CPUCS =              (short) 0xE600;

    public static final byte PERSEUS_CMD_FPGACONFIG =   0x00;
    public static final byte PERSEUS_CMD_FPGARESET =    0x01;
    public static final byte PERSEUS_CMD_FPGACHECK =    0x02;
    public static final byte PERSEUS_CMD_FPGASIO =      0x03;
    public static final byte PERSEUS_CMD_FX2PORTE =     0x04;
    public static final byte PERSEUS_CMD_EEPROMREAD =   0x06;
    public static final byte PERSEUS_CMD_SHUTDOWN =     0x08;

    // Perseus nominal ADC sampling frequency in kHz
    public static final double PERSEUS_ADCSAMPLINGFREQKHZ = 80000.0;

    // Perseus DDC Output Sample Rate Definitions
    public static final int PERSEUS_DDC_48K =   0;
    public static final int PERSEUS_DDC_96K =   1;
    public static final int PERSEUS_DDC_192K =  2;
    public static final int PERSEUS_DDC_95K =   3;
    public static final int PERSEUS_DDC_125K =  4;
    public static final int PERSEUS_DDC_250K =  5;
    public static final int PERSEUS_DDC_500K =  6;
    public static final int PERSEUS_DDC_1M =    7;
    public static final int PERSEUS_DDC_2M =    8;
    public static final int PERSEUS_DDC_LAST =  (PERSEUS_DDC_2M+1);

    // Attenuator Settings defines 
    public static final int PERSEUS_ATT_0DB =   0;
    public static final int PERSEUS_ATT_10DB =  1;
    public static final int PERSEUS_ATT_20DB =  2;
    public static final int PERSEUS_ATT_30DB =  3;

    // Intel Hex File Format Record Types
    public static final int IHEX_DATA =         0x00;
    public static final int IHEX_EOF =          0x01;
    public static final int IHEX_EXTSEGADDR =   0x02;
    public static final int IHEX_EXTLINADDR =   0x03;
    public static final int IHEX_INVALID =      0xFF;
    
    // Values returned by FirmwareDownload()
    public static final int IHEX_DOWNLOAD_OK =              0;  // FX2 Firmware successfully downloaded
    public static final int IHEX_DOWNLOAD_FILENOTFOUND =    1;  // FX2 Firmware file not found
    public static final int IHEX_DOWNLOAD_IOERROR =         2;  // USB IO Error
    public static final int IHEX_DOWNLOAD_INVALIDHEXREC =   3;  // Invalid HEX Record 
    public static final int IHEX_DOWNLOAD_INVALIDEXTREC =   4;  // Invalide Extended HEX Record

    // Values returned by FpgaConfig()
    public static final int FPGA_CONFIG_OK =            0;  // FPGA successfully configured
    public static final int FPGA_CONFIG_FILENOTFOUND =  1;  // FPGA bitstream file not found
    public static final int FPGA_CONFIG_IOERROR =       2;  // USB IO Error
    public static final int FPGA_CONFIG_FWNOTLOADED =   3;  // FX2 Firmware not loaded

}
