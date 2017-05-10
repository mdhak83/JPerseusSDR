package perseus.circuits;

import perseus.PerseusInstance;
import perseus.callback.InputQueue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that can be used in a never-ending loop to modify some parametters of the Perseus HW.
 *  (not used in this version)
 * In such a case, remove all debug logging and start a new thread of this class
 *      FIFO fifo = FIFO.Make(@PerseusInstance)
 *      fifo.start();
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
public class FIFO implements Runnable {

    private PerseusInstance perseus;
    private Thread thread;
    
    private FIFO() {
    }
    
    public static FIFO Make(PerseusInstance perseus) throws IOException {
        FIFO fifo = new FIFO();
        fifo.perseus = perseus;
        return fifo;
    }
    
    @Override
    public void run() {
        Scanner scanner = new Scanner(new InputStreamReader(System.in));
        System.out.println("Reading input from console.");
        System.out.println("Use \"hz xxxxxxx\" to set the frequency in Hz.");
        System.out.println("Use \"mhz xx.xxxxxx\" to set the frequency in MHz.");
        System.out.println("Use \"att (0|10|20[30)\" to set the attenuation.");

        while (true) {
            System.out.print("Please enter your command: ");
            boolean commandOK = false;
            // Scan command line input and process as necessary
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("quit")) {
                commandOK = true;
                break;
            } else if (input.startsWith("mhz")) {
                String[] f = input.split("\\s+");
                if (f.length == 2) {
                    if (f[1].matches("\\d{1,2}\\.\\d+")) {
                        double frequency = Double.parseDouble(f[1]);
                        this.perseus.setDDCCenterFrequency(frequency, true);
                        commandOK = true;
                    }
                }
            } else if (input.startsWith("hz")) {
                String[] f = input.split("\\s+");
                if (f.length == 2) {
                    if (f[1].matches("\\d{1,8}")) {
                        int frequency = Integer.parseInt(f[1]);
                        this.perseus.setDDCCenterFrequency(frequency, true);
                        commandOK = true;
                    }
                }
            } else if (input.startsWith("att")) {
                String[] f = input.split("\\s+");
                if (f.length == 2) {
                    if (f[1].matches("\\d{1,2}")) {
                        int att = Integer.parseInt(f[1]);
                        this.perseus.setAttenuator(Attenuator.FindDB(att));
                        commandOK = true;
                    }
                }
            }
            if (!commandOK) {
                System.out.println("Invalid command.");
            }
            System.out.println();
        }
    }
    
    public void start() {
        Thread t = new Thread(this);
        this.thread = t;
        t.start();
    }
    
    public void stop() throws InterruptedException {
        Logger.getLogger(InputQueue.class.getName()).log(Level.INFO, "FIFO quit sent for Perseus #{0}.", new Object[] { this.perseus.getEeprom().getSerialNumber()});
        this.thread.join();
    }
    
}
