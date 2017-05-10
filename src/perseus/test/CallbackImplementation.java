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
package perseus.test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import perseus.PerseusInstance;
import perseus.callback.InputCallback;
import perseus.circuits.FPGA;
import static perseus.test.PerseusTest.SDF_DATE_TIME;

/**
 * Class used as an example of stream recording Callback Implementation
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.1
 */
public class CallbackImplementation extends InputCallback {
    
    private PerseusInstance perseus = null; 
    private Map<String, FileWriter> map = new HashMap<>();
    private Date dateStart = null;
    private FileWriter log = null;
    private int bufferCounter = 0;
    private int samplesCounter = 0;

    public CallbackImplementation(PerseusInstance perseus, FileWriter raw, FileWriter log) throws IOException {
        this.perseus = perseus;
        this.log = log;
        FPGA fpga = this.perseus.getFpga();
        int bufferLength = fpga.getNbBytesPerSample() * fpga.getNbComponentsPerSample() * fpga.getNbSamplesPerFrame() * fpga.getNbFrames();
        this.dateStart = Calendar.getInstance().getTime();
        this.log.append("## RecordingDateTimeStart=" + SDF_DATE_TIME.format(this.dateStart) + "\r\n");
        this.log.append("## Rate=" + fpga.getRateInSamplesPerSecond() + "\r\n");
        this.log.append("## BufferLength=" + bufferLength + "\r\n");
        this.log.append("## NbBytesPerSample=" + fpga.getNbBytesPerSample() + "\r\n");
        this.log.append("## NbComponentsPerSample=" + fpga.getNbComponentsPerSample() + "\r\n");
        this.log.append("## NbSamplesPerFrame=" + fpga.getNbSamplesPerFrame() + "\r\n");
        this.log.append("## NbFramesPerBuffer=" + fpga.getNbFrames() + "\r\n");
        this.map = new HashMap<>(2);
        this.map.put("raw", raw);
        this.map.put("log", log);
    }
    
    @Override
    public void run() {
        this.dateStart = Calendar.getInstance().getTime();
        FPGA fpga = this.perseus.getFpga();
        int bufferLength = fpga.getNbBytesPerSample() * fpga.getNbComponentsPerSample() * fpga.getNbSamplesPerFrame() * fpga.getNbFrames();
        this.samplesCounter = 0;
        this.bufferCounter = 0;
        this.perseus.startAsyncInput(bufferLength, this, map);
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PERSEUS SDR instance async callback
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * PERSEUS SDR instance asynchronous callback
     * @param buffer The buffer of bytes available
     * @param length The size of the provided buffer
     * @param params The params set when doing "startAsyncInput(...)"
     */
    @Override
    public void callback(ByteBuffer buffer, int length, Object params) {
        if (params instanceof Map) {
            try {
                String dt = SDF_DATE_TIME.format(Calendar.getInstance().getTime());
                Map<String, FileWriter> map = (Map) params;
                FileWriter raw = map.get("raw");
                FileWriter log = map.get("log");
                log.append("## BufferIndex="+this.bufferCounter+"\r\n");
                log.append("## BufferReceiveTime="+dt+"\r\n");
                buffer.rewind();
                while (buffer.hasRemaining()) {
                    byte i2 = buffer.get();
                    byte i3 = buffer.get();
                    byte i4 = buffer.get();
                    long i = (i4 << 24 & 0x00FF000000) | (i3 << 16 & 0x0000FF0000) | (i2 << 8 & 0x000000FF00);
                    byte q2 = buffer.get();
                    byte q3 = buffer.get();
                    byte q4 = buffer.get();
                    long q = (q4 << 24 & 0x00FF000000) | (q3 << 16 & 0x0000FF0000) | (q2 << 8 & 0x000000FF00);
                    raw.append(i+ " " + q + "\r\n");
                    this.samplesCounter++;
                }
                this.bufferCounter++;
            } catch (IOException ex) {
                Logger.getLogger(PerseusTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void quit() throws IOException {
        this.perseus.stopAsyncInput();
        Date dateEnd = Calendar.getInstance().getTime();
        this.log.append("## RecordingDateTimeStop=" + SDF_DATE_TIME.format(dateEnd) + "\r\n");
        this.log.append("## MeanRate=" + (int) this.samplesCounter/((dateEnd.getTime() - dateStart.getTime()) / 1000) + "\r\n");
    }
    
}
