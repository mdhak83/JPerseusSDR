package perseus.utils;

import java.io.PrintStream;

/**
 * Helper class with some useful methods
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
 * The JPerseus v1.0.0 library, source code and documentation is published under the GNU Lesser General Public Licence v3
 *  (available under the folder "/resources").
 * 
 * @author Mehdi DHAKOUANI
 * Copyright 2017, Mehdi DHAKOUANI
 * @version 1.0.0
 * 
 */
public class XTools {
    
    /**
     * Converts a hex string of length 2 to a byte.
     * @param in The input string representing the 2-characters hexadecimal value.
     * @return the corresponding Byte value.
     */
    public static Byte ConvertHexString(String in) {
        Byte ret = null;
        if (in.length() == 2) {
            ret = (byte) (Byte.parseByte(in.substring(0,1), 16) << 4 | Byte.parseByte(in.substring(1,2), 16));
        } else {
            throw new IllegalArgumentException("Wrong hexadecimal input");
        }
        return ret;
    }
    
    /**
     * Dumps a byte array content to a @PrintStream
     * @param ps The PrintStream to be used for the dump.
     * @param ba The byte array to dump.
     * @param title The title to prefix the dump with.
     */
    public static void Dump(PrintStream ps, byte[] ba, String title) {
        if (ps != null) {
            ps.print(title);
            for (int i = 0 ; i < ba.length ; i++) {
                if (i % 16 == 0) {
                    ps.println();
                    ps.print(String.format("  0x%04X : ", i));
                }
                ps.print(String.format("%02X ", ba[i]));
            }
            ps.println();
        }
    }
    
}