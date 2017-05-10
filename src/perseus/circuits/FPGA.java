package perseus.circuits;

import static perseus.utils.Definitions.PERSEUS_CMD_FPGACONFIG;
import perseus.utils.XResource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import static perseus.circuits.Firmware.FX2;
import static perseus.circuits.Firmware.WB;

/**
 * This class is used to enumerate all FPGAs' data, each being usable with a specific firmware with the Perseus Hardware.
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
 * Use at your own risk !! Implies modification of the Perseus FPGA !!
 * Any FPGA version delivered after 04/May/2017 might become incompatible with this class.
 * Any error while loading the FPGA might generate a FPGA configuration failure. This might be an indication that the secret key changed for new versions of the FPGA.
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
public enum FPGA {
    
    /* Narrowband 48 kS/s FPGA release v3.1 (48 kHz) */
    PERSEUS_DDC_48K     (0,  "perseus48k24v31",    170, 3, 2,   48000,   1664,  FX2,   new byte[] {(byte) 0xE4, (byte) 0x37, (byte) 0x73, (byte) 0x7B, (byte) 0x9A, (byte) 0x6A }),

    /* Narrowband 96 kS/s FPGA release v3.1 (96 kHz) */
    PERSEUS_DDC_96K     (1,  "perseus96k24v31",    170, 3, 2,   96000,    832,  FX2,   new byte[] {(byte) 0x39, (byte) 0x18, (byte) 0x79, (byte) 0xF5, (byte) 0x90, (byte) 0x8C }),

    /* Narrowband 192 kS/s FPGA release v3.1 (192 kHz) */
    PERSEUS_DDC_192K    (2,  "perseus192k24v31",   170, 3, 2,  192000,    416,  FX2,   new byte[] {(byte) 0x7B, (byte) 0x39, (byte) 0x26, (byte) 0x75, (byte) 0x9F, (byte) 0x7B }),

    /* Narrowband 95 kS/s FPGA release v3.1 (95 kHz) */
    PERSEUS_DDC_95K     (3,  "perseus95k24v31",    170, 3, 2,   95000,    842,  FX2,   new byte[] {(byte) 0x22, (byte) 0x33, (byte) 0xB8, (byte) 0xEA, (byte) 0x2C, (byte) 0xBC }),

    /* Narrowband 125 kS/s FPGA release v2.1 (125 kHz) */
    PERSEUS_DDC_125K    (4,  "perseus125k24v21",   170, 3, 2,  125000,    640,  FX2,   new byte[] {(byte) 0x45, (byte) 0x7C, (byte) 0xD4, (byte) 0x3A, (byte) 0x0E, (byte) 0xD4 }),

    /* Narrowband 250 kS/s FPGA release v2.1 (250 kHz) */
    PERSEUS_DDC_250K    (5,  "perseus250k24v21",   170, 3, 2,  250000,    320,  FX2,   new byte[] {(byte) 0xCF, (byte) 0x4B, (byte) 0x9F, (byte) 0x02, (byte) 0xB2, (byte) 0x1E }),

    /* Narrowband 500 kS/s FPGA release v2.1 (500 kHz) */
    PERSEUS_DDC_500K    (6,  "perseus500k24v21",   170, 3, 2,  500000,    160,  FX2,   new byte[] {(byte) 0xF2, (byte) 0x9E, (byte) 0x67, (byte) 0x49, (byte) 0x51, (byte) 0x6B }),

    /* Narrowband 1 MS/s FPGA release v2.1 (1 MHz) */
    PERSEUS_DDC_1M      (7,  "perseus1m24v21",     170, 3, 2, 1000000,     80,  FX2,   new byte[] {(byte) 0xC5, (byte) 0xC1, (byte) 0x96, (byte) 0x4D, (byte) 0xA8, (byte) 0x05 }),

    /* Narrowband 2 MS/s FPGA release v2.1 (2 MHz) */
    PERSEUS_DDC_2M      (8,  "perseus2m24v21",     170, 3, 2, 2000000,     40,  FX2,   new byte[] {(byte) 0xDE, (byte) 0xEA, (byte) 0x57, (byte) 0x52, (byte) 0x14, (byte) 0x35 }),

    /* Wideband 6.4 MS/s FPGA release v1.0 (40 MHz) */
    PERSEUS_DDC_WB      (10, "perseusds16v10",     512, 2, 1, 6553600,     10,  WB,    new byte[] {(byte) 0x1E, (byte) 0x8A, (byte) 0x3B, (byte) 0x9B, (byte) 0xBE, (byte) 0xD0 });
    
    private static final int SCRAMBLE_TABLE_SIZE = 256;
    private static final int[] PRESCRAMBLE_TABLE = new int[] {0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13, 3, 11, 7, 15 };
    private static final int[] SCRAMBLE_TABLE = new int[SCRAMBLE_TABLE_SIZE];
    private static final int BLOCK_SIZE = 63;
    private static final byte[] SECRET_XOR_KEY = new byte[] {(byte) 0x81, (byte) 0xB2, (byte) 0x25, (byte) 0xAA, (byte) 0x75, (byte) 0xAA};
    
    public static final int PERSEUS_BASE_SAMPLING_RATE = 80000000;
    
    static {
        // Compute the scramble table
        for (int i = 0 ; i < SCRAMBLE_TABLE_SIZE ; i++) {
            int x = PRESCRAMBLE_TABLE[i % 16];
            int y = PRESCRAMBLE_TABLE[(i >> 4) % 16];
            SCRAMBLE_TABLE[i] = (y | (x << 4)) & 0x00FF;
        }
    }
    
    private final int index;
    private final String filename;
    private final int nbFrames = 8;
    private final int nbSamplesPerFrame;
    private final int nbBytesPerSample;
    private final int nbComponentsPerSample;
    private final int rateInSamplesPerSecond;
    private final int decimationFactor;
    private final Firmware associatedFirmware;
    private final byte[] signature;
    private Path path;
    private int currentBlockIndex = 0;
    private SeekableByteChannel channel = null;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BLOCK_SIZE);
    private byte[] temporarySignature = new byte[6];
    
    /**
     * Create each enumeration of FPGA which should be found under /resources folder.
     * @param index an index to enumerate the FPGA number
     * @param filename the name of the Perseus FPGA file (ends with ".sbs")
     * @param nbSamplesPerFrame the number of samples provided per 1.024 bytes USB elementary frame
     * @param nbBytesPerSample the number of bytes per sample
     * @param nbComponentsPerSample the number of components per sample (2 for I-Q, 1 for single)
     * @param rateInSamplesPerSecond the rate in samples/second at which the Perseus HW will provide data
     * @param decimationFactor the decimation factor used by Perseus HW to reach that @param rateInSamplesPerSecond
     * @param associatedFirmware the firmware to this FPGA file
     * @param signature the signature of this FPGA file (required to decode it)
     */
    FPGA(int index, String filename, int nbSamplesPerFrame, int nbBytesPerSample, int nbComponentsPerSample, int rateInSamplesPerSecond, int decimationFactor, Firmware associatedFirmware, byte[] signature) {
        if (signature == null || signature.length != 6) {
            throw new IllegalArgumentException("Invalid signature for FPGA");
        }
        this.index = index;
        this.filename = filename;
        this.nbSamplesPerFrame = nbSamplesPerFrame;
        this.nbBytesPerSample = nbBytesPerSample;
        this.nbComponentsPerSample = nbComponentsPerSample;
        this.rateInSamplesPerSecond = rateInSamplesPerSecond;
        this.decimationFactor = decimationFactor;
        this.associatedFirmware = associatedFirmware;
        this.signature = signature;
        this.temporarySignature = Arrays.copyOf(this.signature, 6);
        this.path = null;
        try {
            this.path = Paths.get(XResource.GetInstance().getResourceFile(this.filename+".sbs").getAbsolutePath());
        } catch (IOException ex) {
            Logger.getLogger(FPGA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Rewind the current FPGA
     * Should never be called.
     * @throws IOException if it is impossible to rewind the FPGA file.
     */
    private void rewind() throws IOException {
        this.currentBlockIndex = 0;
        this.temporarySignature = Arrays.copyOf(this.signature, 6);
        if (this.channel != null) {
            this.channel.position(0);
        }
    }

    /**
     * Reads the next block of raw data for the current FPGA file
     * @return the number of bytes which could be read.
     * @throws IOException if the reading of the FPGA file failed.
     */
    private int readBlock() throws IOException {
        if (this.channel == null) {
            this.channel = Files.newByteChannel(this.path, StandardOpenOption.READ);
            this.currentBlockIndex = 0;
        }
        this.buffer.clear();
        int nbBytes = this.channel.read(this.buffer);
        this.currentBlockIndex++;
        return nbBytes;
    }

    /**
     * Provides the next block of decoded data available for the FPGA to load
     * @return an array of FPGA data bytes (unscrambled and decoded)
     * @throws IOException if the reading of the FPGA file failed.
     */
    public byte[] getNextBlock() throws IOException {
        byte[] buf = null;
        int nbBytes = this.readBlock();
        if (nbBytes != -1) {
            buf = new byte[nbBytes+1];
            buf[0] = PERSEUS_CMD_FPGACONFIG;
            this.buffer.rewind();
            this.buffer.get(buf, 1, nbBytes);
            byte[] s = this.temporarySignature;
            int count = 0;
            while (count < nbBytes) {
                int b0 = s[0] & 0x00FF;
                int sign, sign_bit;
                for (int i = 0 ; i < 8 ; i++) {
                    sign_bit = s[0] & 0x0080;
                    s[0] = (byte) ((s[0] * 2) & 0x00FF);

                    sign = (sign_bit == 0 ? 0 : 1);
                    sign_bit = s[1] & 0x0080;
                    s[1] = (byte) (((s[1] * 2) & 0x00FF) | sign);

                    sign = (sign_bit == 0 ? 0 : 1);
                    sign_bit = s[2] & 0x0080;
                    s[2] = (byte) (((s[2] * 2) & 0x00FF) | sign);

                    sign = (sign_bit == 0 ? 0 : 1);
                    sign_bit = s[3] & 0x0080;
                    s[3] = (byte) (((s[3] * 2) & 0x00FF) | sign);

                    sign = (sign_bit == 0 ? 0 : 1);
                    sign_bit = s[4] & 0x0080;
                    s[4] = (byte) (((s[4] * 2) & 0x00FF) | sign);

                    sign = (sign_bit == 0 ? 0 : 1);
                    sign_bit = s[5] & 0x0080;
                    s[5] = (byte) (((s[5] * 2) & 0x00FF) | sign);

                    if (sign_bit != 0) {
                        for (int k = 0 ; k < s.length ; k++) {
                            s[k] ^= SECRET_XOR_KEY[k];
                        }
                    }
                }
                int scrambleIndex = ((buf[count+1] & 0x00FF) ^ ((b0 & 0x00FF)));
                buf[count+1] = (byte) SCRAMBLE_TABLE[scrambleIndex];
                count++;
            }
        }
        return buf;
    }
    
    /**
     * @return the index of this FPGA in the enumeration
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the filename associated to this FPGA
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the signature of this FPGA
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * @return the current FPGA block index available
     */
    public int getCurrentBlockIndex() {
        return currentBlockIndex;
    }

    /**
     * @return the regular number of USB frames to buffer (to compute the buffer's length) before it is reported to the software
     */
    public int getNbFrames() {
        return nbFrames;
    }

    /**
     * @return the rate in samples per second that this Perseus FPGA is capable of providing samples
     */
    public int getRateInSamplesPerSecond() {
        return rateInSamplesPerSecond;
    }

    /**
     * @return the firmware that has to be loaded for this FPGA file to work correctly
     */
    public Firmware getAssociatedFirmware() {
        return associatedFirmware;
    }

    /**
     * @return the number of samples per USB elementary frame (1.024 bytes)
     */
    public int getNbSamplesPerFrame() {
        return nbSamplesPerFrame;
    }

    /**
     * @return the number of bytes per sample
     */
    public int getNbBytesPerSample() {
        return nbBytesPerSample;
    }

    /**
     * @return the number of components per sample
     */
    public int getNbComponentsPerSample() {
        return nbComponentsPerSample;
    }

    /**
     * @return the decimation factor of the Perseus HW to reach this rate (from PERSEUS_BASE_SAMPLING_RATE)
     */
    public int getDecimationFactor() {
        return decimationFactor;
    }
    
}
