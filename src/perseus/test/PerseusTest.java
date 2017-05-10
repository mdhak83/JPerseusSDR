package perseus.test;

import java.io.File;
import java.io.FileWriter;
import perseus.PerseusInstance;
import perseus.circuits.FPGA;
import perseus.circuits.Firmware;
import perseus.circuits.Attenuator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.usb4java.LibUsbException;

/**
 * Test example for multiple sequential Perseus HW configuration and recording
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
public class PerseusTest {
    
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);
    public static int TEST_DURATION_MS = 10000;
    public static int BufferCounter = 0;
    public static int SamplesCounter = 0;
    public static final SimpleDateFormat SDF_DATE_TIME = new SimpleDateFormat("YYYYMMdd-HHmmssSSS");
    
    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) {
        
        // Process all initializing actions
        PerseusInstance.Initialize();

        // Initialize each Perseus HW to a firmware
        Set<PerseusInstance> ps = PerseusInstance.getPerseusInstances();
        if (!ps.isEmpty()) {
            boolean first = true;
            for (PerseusInstance perseus : ps) {
                try {
                    perseus.open();
                    Firmware firmware = first ? Firmware.WB : Firmware.FX2;
                    first = false;
                    System.out.println("Perseus being set on Firmware '"+firmware.name()+"'");
                    perseus.setFirmware(firmware);
                } catch (LibUsbException ex) {
                    Logger.getLogger(PerseusTest.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        perseus.close();
                    } catch (LibUsbException ex1) {
                        Logger.getLogger(PerseusTest.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
            
            // Wait 4 seconds so that the PC hardware finds the PErseus hardwares again... (because of the USB reinitialization)
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ex) {
                Logger.getLogger(PerseusTest.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Reinitialize the Perseus library (looks for updated Perseus HW and reassigns their previously uploaded firmware)
            PerseusInstance.Reinitialize();

            // In this loop, we are going to process each connected Perseus
            ps = PerseusInstance.getPerseusInstances();
            if (!ps.isEmpty()) {
                int PERSEUS_COUNT = 0;
                for (PerseusInstance perseus : ps) {
                    BufferCounter = 0;
                    try {
                        // Opens the PErseus HW
                        perseus.open();

                        // Read EEPROM and print out information
                        perseus.readEEPROM();
                        System.out.println("Perseus SN#"+perseus.getEeprom().getSerialNumber()+" set on Firmware '"+perseus.getFirmware().name()+"'");

                        // Set FPGA to a firmware
                        if (perseus.getFirmware() == Firmware.FX2) {
                            perseus.setFPGA(FPGA.PERSEUS_DDC_2M);   // Can be any other firmware (different than WB)
                        } else if (perseus.getFirmware() == Firmware.WB) {
                            perseus.setFPGA(FPGA.PERSEUS_DDC_WB);
                        }

                        String datetime = SDF_DATE_TIME.format(Calendar.getInstance().getTime());
                        File f_raw_out = new File(System.getProperty("user.home") + File.separator + datetime + "_" + perseus.getFpga().name() + ".raw.txt");
                        try (FileWriter fw_raw_out = new FileWriter(f_raw_out, true)) {
                            if (f_raw_out.exists()) {
                                f_raw_out.delete();
                            }
                            
                            // Cycle attenuators LEDs on the receiver front panel for test purposes
                            perseus.setAttenuator(Attenuator._00DB);
                            Thread.sleep(500);
                            perseus.setAttenuator(Attenuator._10DB);
                            Thread.sleep(500);
                            perseus.setAttenuator(Attenuator._20DB);
                            Thread.sleep(500);
                            perseus.setAttenuator(Attenuator._30DB);
                            Thread.sleep(500);
                            perseus.setAttenuator(Attenuator._00DB);
                            Thread.sleep(500);
                            
                            // Set ADC to disable dither and disable preamplification
                            perseus.setADC(true, false);
                            
                            // The frequency is set such that each additional Perseus HW (set to narrowband) will record a different 2-MHz subband between 2 and 30 MHz.
                            // The first Perseus found is always set to WB.
                            int frequency = (3+((PERSEUS_COUNT-1)%14)*2)*1000000;
                            
                            // Cycle Wideband LED ON/OFF for test purposes
                            if (perseus.getFpga() == FPGA.PERSEUS_DDC_WB) {
                                perseus.setDDCCenterFrequency(frequency, false);
                                Thread.sleep(500);
                            }
                            perseus.setDDCCenterFrequency(frequency, true);
                            Thread.sleep(500);
                            perseus.setDDCCenterFrequency(frequency, false);
                            Thread.sleep(500);
                            if (perseus.getFpga() != FPGA.PERSEUS_DDC_WB) {
                                perseus.setDDCCenterFrequency(frequency, true);
                            }
                            
                            
                            if (perseus.getFpga() != FPGA.PERSEUS_DDC_WB) {
                                Map<String, FileWriter> map = new HashMap<>(2);
                                map.put("raw", fw_raw_out);
                                
                                // Start Async input recording
                                FPGA fpga = perseus.getFpga();
                                int bufferLength = fpga.getNbBytesPerSample() * fpga.getNbComponentsPerSample() * fpga.getNbSamplesPerFrame() * fpga.getNbFrames();
                                Date dateStart = Calendar.getInstance().getTime();
                                fw_raw_out.append("## RecordingDateTimeStart=" + SDF_DATE_TIME.format(dateStart) + "\r\n");
                                fw_raw_out.append("## Rate=" + fpga.getRateInSamplesPerSecond() + "\r\n");
                                fw_raw_out.append("## BufferLength=" + bufferLength + "\r\n");
                                fw_raw_out.append("## NbBytesPerSample=" + fpga.getNbBytesPerSample() + "\r\n");
                                fw_raw_out.append("## NbComponentsPerSample=" + fpga.getNbComponentsPerSample() + "\r\n");
                                fw_raw_out.append("## NbSamplesPerFrame=" + fpga.getNbSamplesPerFrame() + "\r\n");
                                fw_raw_out.append("## NbFramesPerBuffer=" + fpga.getNbFrames() + "\r\n");
                                EXECUTOR.execute(perseus);
                                perseus.startAsyncInput(bufferLength, perseus, map);
                                
                                // Record for 'TEST_DURATION_MS' milliseconds
                                Thread.sleep(TEST_DURATION_MS);
                                
                                // Stop Async input recording
                                perseus.stopAsyncInput();
                                Date dateEnd = Calendar.getInstance().getTime();
                                fw_raw_out.append("## RecordingDateTimeStop=" + SDF_DATE_TIME.format(dateEnd) + "\r\n");
                                fw_raw_out.append("## MeanRate=" + (int) SamplesCounter/((dateEnd.getTime() - dateStart.getTime()) / 1000) + "\r\n");
                                EXECUTOR.shutdown();
                            } else if (perseus.getFpga() == FPGA.PERSEUS_DDC_WB) {
                                // Enable FPGA FIFO
                                perseus.getSioControl().enableFIFOEN(true);
                                if (perseus.setFX2SIO(true) < 0) {
                                    Logger.getLogger(PerseusTest.class.getName()).log(Level.WARNING, "Cannot get input data for Perseus #{0} : FIFO enabling failed.", new Object[] { perseus.getEeprom().getSerialNumber()});
                                }
                                FPGA fpga = perseus.getFpga();
                                int bufferLength = fpga.getNbBytesPerSample() * fpga.getNbComponentsPerSample() * fpga.getNbSamplesPerFrame() * fpga.getNbFrames();
                                Date dateStart = Calendar.getInstance().getTime();
                                long t0 = dateStart.getTime();
                                fw_raw_out.append("## RecordingDateTimeStart=" + SDF_DATE_TIME.format(dateStart) + "\r\n");
                                fw_raw_out.append("## Rate=" + fpga.getRateInSamplesPerSecond() + "\r\n");
                                fw_raw_out.append("## BufferLength=" + bufferLength + "\r\n");
                                fw_raw_out.append("## NbBytesPerSample=" + fpga.getNbBytesPerSample() + "\r\n");
                                fw_raw_out.append("## NbComponentsPerSample=" + fpga.getNbComponentsPerSample() + "\r\n");
                                fw_raw_out.append("## NbSamplesPerFrame=" + fpga.getNbSamplesPerFrame() + "\r\n");
                                fw_raw_out.append("## NbFramesPerBuffer=" + fpga.getNbFrames() + "\r\n");
                                while ((System.currentTimeMillis()-t0) < TEST_DURATION_MS) {
                                    String dt = SDF_DATE_TIME.format(Calendar.getInstance().getTime());
                                    ByteBuffer buffer = perseus.getInputData(bufferLength);
                                    fw_raw_out.append("## BufferIndex="+BufferCounter + "\r\n");
                                    fw_raw_out.append("## BufferReceiveTime="+dt + "\r\n");
                                    for (int i = 0 ; i < fpga.getNbSamplesPerFrame() * fpga.getNbFrames() ; i++) {
                                        if (buffer.hasRemaining()) {
                                            fw_raw_out.append(buffer.getShort()+"\r\n");
                                            SamplesCounter++;
                                        }
                                    }
                                    BufferCounter++;
                                }
                                perseus.getSioControl().enableFIFOEN(false);
                                if (perseus.setFX2SIO(true) < 0) {
                                    Logger.getLogger(PerseusTest.class.getName()).log(Level.WARNING, "Cannot get input data for Perseus #{0} : FIFO disabling failed.", new Object[] { perseus.getEeprom().getSerialNumber()});
                                }
                                Logger.getLogger(PerseusTest.class.getName()).log(Level.INFO, "Writing finished.");
                                Date dateEnd = Calendar.getInstance().getTime();
                                fw_raw_out.append("## RecordingDateTimeStop=" + SDF_DATE_TIME.format(dateEnd) + "\r\n");
                                fw_raw_out.append("## MeanRate=" + (int) SamplesCounter/((dateEnd.getTime() - dateStart.getTime()) / 1000) + "\r\n");
                            }
                            fw_raw_out.flush();
                        }
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(PerseusTest.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            // Close the Perseus HW
                            perseus.close();
                        } catch (LibUsbException ex1) {
                            Logger.getLogger(PerseusTest.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                    }
                    PERSEUS_COUNT++;
                }
            } else {
                Logger.getLogger(PerseusTest.class.getName()).log(Level.INFO, "No Perseus HW found after reinitialization.");
            }
        } else {
            Logger.getLogger(PerseusTest.class.getName()).log(Level.INFO, "No Perseus HW found.");
        }

        // Process all finalizing actions
        PerseusInstance.Stop();
    }

}