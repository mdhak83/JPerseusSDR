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
 * The JPerseus library, source code and documentation is published under the GNU Lesser General Public Licence v3
 *  (available under the folder "/resources").
 * 
 * Copyright 2017, Mehdi DHAKOUANI
 */
package perseus;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.usb4java.LibUsb;

/**
 * Class used to handle USB events.
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.0
 */
public class LibUsbPollThread extends Thread {

    private static LibUsbPollThread INSTANCE = null;
    
    private LibUsbPollThread() {
	int maxpri;
        if ((maxpri = Thread.MAX_PRIORITY) >= 0) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Setting thread priority to {0}...", maxpri);
            this.setPriority(MAX_PRIORITY);
        }
    }
    
    public static LibUsbPollThread GetInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LibUsbPollThread();
        }
        return INSTANCE;
    }
    
    @Override
    public void run() {
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "LibUsb Poll thread started...");
        // Handle LibUsb events until Perseus.Stop() is called
        while (!PerseusInstance.LIBUSB_POLL_THREAD_STOP) {
            LibUsb.handleEventsTimeout(PerseusInstance.CONTEXT, 1000);
        }
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "LibUsb Poll thread terminating...");
    }
    
}
