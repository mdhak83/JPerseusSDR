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

import java.io.FileWriter;
import perseus.callback.InputCallback;
import perseus.callback.InputQueue;
import perseus.circuits.EEPROM;
import static perseus.circuits.EEPROM.ADDR_EEPROM_PRODID;
import static perseus.circuits.EEPROM.ADDR_EEPROM_PRODID_SIZE;
import perseus.circuits.FPGA;
import perseus.circuits.Firmware;
import perseus.circuits.FirmwareBlock;
import perseus.circuits.Preselector;
import perseus.circuits.Attenuator;
import perseus.circuits.PreselectorFilter;
import perseus.circuits.SIOControl;
import static perseus.utils.Definitions.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.usb4java.ConfigDescriptor;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.Interface;
import org.usb4java.InterfaceDescriptor;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

/**
 * Class used to represent an instance of a Perseus SDR hardware connected to the computer.
 * 
 * @author Mehdi DHAKOUANI
 * @version 1.0.1
 */
public class PerseusInstance {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PERSEUS SDR static members
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected static final Context CONTEXT = null;
    protected static boolean LIBUSB_POLL_THREAD_FLAG = false;
    protected static boolean LIBUSB_POLL_THREAD_STOP = false;
    protected static LibUsbPollThread LIBUSB_POLL_THREAD = null;
    protected static final Set<PerseusInstance> PERSEUS_INSTANCES = new HashSet<>();
    private static final DeviceList USB_LIST = new DeviceList();
    private static final Map<String, Firmware> INITIATED_FIRMWARES = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PERSEUS SDR non-static members
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String name = null;
    private Device device = null;
    private DeviceHandle handle = null;
    private byte interfaceNumber;
    private DeviceDescriptor deviceDescriptor = null;
    private ConfigDescriptor configDescriptor = null;
    private boolean firmwareLoaded = false;
    private boolean cypressEZUSB = false;
    private Firmware firmware = null;
    private FPGA fpga = null;
    private EEPROM eeprom = null;
    private boolean kernelAttached = false;
    private boolean preserie = false;
    private byte frontendControl = 0;
    private final Preselector preselector = new Preselector();
    private final SIOControl sioControl = new SIOControl();
    private final InputQueue inputQueue = new InputQueue();
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PERSEUS SDR static methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     *  Initialize LibUsb
     *  Set LibUsb Debug level to INFO
     *  Look for each connected Perseus device
     */
    public static void Initialize() {
        int result = LibUsb.init(null);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to initialize libusb.", result);
        }
        Logger.getGlobal().setLevel(Level.WARNING);
        LibUsb.setDebug(null, LibUsb.LOG_LEVEL_WARNING);
        FindDevices();
    }
    
    /**
     *  Process again to the research of each Perseus device and reassociate their previously loaded firmware
     */
    public static void Reinitialize() {
        FindDevices();
        for (PerseusInstance p : PERSEUS_INSTANCES) {
            String id = String.format("BUS#%03d-PORT#%03d", LibUsb.getBusNumber(p.getDevice()), LibUsb.getPortNumber(p.getDevice()));
            if (INITIATED_FIRMWARES.containsKey(id)) {
                Firmware f = INITIATED_FIRMWARES.get(id);
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Reminding firmware loaded ('" + f + "') for device " + id, PERSEUS_MAX_NR);
                p.setFirmware(f, false);
            }
        }
    }
    
    /**
     *  - Finds all connected Perseus instances and stores them
     *  - Read their device descriptor
     *  - Start the LibUsb Poll thread
     */
    private static void FindDevices() {
        PERSEUS_INSTANCES.clear();
        
        // Read the USB device list
        int result = LibUsb.getDeviceList(CONTEXT, USB_LIST);
        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        // Iterate over all devices and scan for the right ones
        for (Device device : USB_LIST) {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to read device descriptor", result);
            }
            if (descriptor.idVendor() == PERSEUS_VENDOR_ID) {
                if (descriptor.idProduct() == PERSEUS_PRODUCT_ID || descriptor.idProduct() == PERSEUS_PID_BLANKEEPROM) {
                    PerseusInstance pi = new PerseusInstance(device);
                    if (descriptor.idProduct() == PERSEUS_PID_BLANKEEPROM) {
                        pi.setCypressEZUSB(true);
                    }
                    pi.setDeviceDescriptor(descriptor);
                    PERSEUS_INSTANCES.add(pi);
                    if (PERSEUS_INSTANCES.size() == PERSEUS_MAX_NR) {
                        Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Maximum number of {0} Perseus HW reached. Ignoring others.", PERSEUS_MAX_NR);
                        break;
                    }
                }
            }
        }
        
        // Ensure the allocated device list is freed
        LibUsb.freeDeviceList(USB_LIST, true);
        
        if (PERSEUS_INSTANCES.size() > 0 && !LIBUSB_POLL_THREAD_FLAG) {
            LIBUSB_POLL_THREAD_STOP = false;
            LIBUSB_POLL_THREAD = LibUsbPollThread.GetInstance();
            LIBUSB_POLL_THREAD.start();
            LIBUSB_POLL_THREAD_FLAG = true;
        }
    }
    
    /**
     *  - Removes all Perseus instances
     *  - Wait for all threads to terminate
     *  - Exit LibUsb
     */
    public static void Stop() {
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Stopping Perseus.");
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Removing all Perseus instances from list...");
        PERSEUS_INSTANCES.clear();

        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "LIBUSB_POLL_THREAD_FLAG={0}", LIBUSB_POLL_THREAD_FLAG);
	if (LIBUSB_POLL_THREAD_FLAG) {
            LIBUSB_POLL_THREAD_STOP = true;
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Waiting for LibUsb Poll Thread to stop...");
            try {
                LIBUSB_POLL_THREAD.join();
                LIBUSB_POLL_THREAD_FLAG = false;
            } catch (InterruptedException ex) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "LIBUSB_POLL_THREAD interrupted...", ex);
            }
	}
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Exiting LibUsb...");
	LibUsb.exit(CONTEXT);
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "LibUsb exited.");
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PERSEUS SDR instance methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creates an instance of a Perseus SDR Device
     *  - initialize its members
     *  - reads the configuration descriptor
     *  - reads the interface number
     *  - references the device to LibUsb
     * @param device The device
     * @throws LibUsbException when the @Device parameter is NULL
     */
    public PerseusInstance(Device device) {
        if (device == null) {
            throw new LibUsbException("Provided device is NULL.", LibUsb.ERROR_NO_DEVICE);
        }
        this.device = device;
        this.preserie = false;
        this.fpga = null;
        this.readConfigDescriptor();
        this.readInterfaceNumber();
        LibUsb.refDevice(device);
    }
    
    /*
     * Open the Perseus instance HW through the device reference
     *  - detaches it from the Kernel's driver if necessary and
     *  - sets its configuration
     *  - claim its interface
     *  - sets the interface's alternate settings
     *  - clears Halt on CMD, STATUS and DATAIN interfaces
     */
    public boolean open() {
        boolean ret = false;
        this.handle = new DeviceHandle();
        if (LibUsb.open(this.device, this.handle) == LibUsb.SUCCESS) {
            String sDevice = String.format("Found Device (Product: 0x%04X  / Vendor: 0x%04X) on Bus#%03d - Address #%03d - Port #%d", this.deviceDescriptor.idProduct(), this.deviceDescriptor.idVendor(), LibUsb.getBusNumber(device), LibUsb.getDeviceAddress(device), LibUsb.getPortNumber(device));
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, sDevice);
            
            // Check if the driver's interface is attached tot he kernel
            this.kernelAttached = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER) && (LibUsb.kernelDriverActive(this.handle, this.interfaceNumber) == 1);

            // Detach the kernel driver
            if (this.kernelAttached) {
                LibUsb.detachKernelDriver(handle,  this.interfaceNumber);
            }

            int result;
            if ((result = LibUsb.setConfiguration(this.handle, 1)) != LibUsb.SUCCESS) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to set device to configuration #0");
            }
            
            if ((result = LibUsb.claimInterface(this.handle, this.interfaceNumber)) != LibUsb.SUCCESS) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to claim interface {0}", this.interfaceNumber);
            }
            
            if ((result = LibUsb.setInterfaceAltSetting(this.handle, 0, 0)) != LibUsb.SUCCESS) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to set interface to Alternate Settings 0/0");
            }

            int r1, r2, r3;
            if ((r1 = LibUsb.clearHalt(this.handle, PERSEUS_EP_CMD)) != LibUsb.SUCCESS) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to clear halt on CMD interface : PERSEUS_EP_CMD");
            }
            if ((r2 = LibUsb.clearHalt(this.handle, PERSEUS_EP_STATUS)) != LibUsb.SUCCESS) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to clear halt on STATUS interface : PERSEUS_EP_STATUS");
            }
            if ((r3 = LibUsb.clearHalt(this.handle, PERSEUS_EP_DATAIN)) != LibUsb.SUCCESS) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to clear halt on DATAIN interface : PERSEUS_EP_DATAIN");
            }
            this.firmwareLoaded = (r1 == 0 && r2 == 0 && r3 == 0);
            
            ret = true;
        }
        return ret;
    }
    
    /*
     * Close the Perseus instance HW through its handle, unreferences it from LibUsb, reattach it to the Kernel's driver if necessary and
     *  free all resources associated to it
     */
    public void close() {
        if (this.kernelAttached) {
            LibUsb.attachKernelDriver(this.handle, 0);
        }
        LibUsb.releaseInterface(this.handle, this.interfaceNumber);
        LibUsb.unrefDevice(this.device);
        LibUsb.freeConfigDescriptor(this.configDescriptor);
        this.configDescriptor = null;
        LibUsb.close(this.handle);
        this.handle = null;
    }
    
    /*
     * Read the configuration descriptor out of the device and associates it with the Perseus
     */
    private void readConfigDescriptor() {
        ConfigDescriptor cd = new ConfigDescriptor();
        LibUsb.getConfigDescriptor(this.device, (byte) 0, cd);
        this.configDescriptor = cd;
    }
    
    /*
     * Read the interface number out of the configuration descriptor and associates it with the Perseus
     */
    private void readInterfaceNumber() {
        if (this.configDescriptor != null) {
            Interface[] iface = this.configDescriptor.iface();
            if (iface.length != 0) {
                InterfaceDescriptor[] ids = iface[0].altsetting();
                if (ids.length != 0 && ids[0] != null) {
                    this.interfaceNumber = ids[0].bInterfaceNumber();
                } else {
                    throw new LibUsbException("Interface number for device " + device.toString() + "not available", LibUsb.ERROR_NOT_FOUND);
                }
            } else {
                throw new LibUsbException("Interface number for device " + device.toString() + "not available", LibUsb.ERROR_NOT_FOUND);
            }
        } else {
            throw new LibUsbException("Configuration descriptor unavailable for device " + device.toString() + ".", LibUsb.ERROR_NOT_FOUND);
        }
    }
    
    /* Control the Fx2 CPU Reset line
     * Issue a FX2_BM_VENDOR_REQUEST requestType, FX2_REQUEST_FIRMWARE_LOAD request with wValue FX2_ADDR_CPUCS
     * and data = reset line status (1:hold CPU reset, 0: Leave CPU reset)
     */
    public boolean fx2Reset(boolean status) {
        int ret;
        ByteBuffer buffer = ByteBuffer.allocateDirect(1);
        buffer.put((byte) (status ? 1 : 0));
        if ((ret = LibUsb.controlTransfer(this.handle, FX2_BM_VENDOR_REQUEST, FX2_REQUEST_FIRMWARE_LOAD, FX2_ADDR_CPUCS, (short) 0, buffer, FX2_TIMEOUT)) != 1) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Impossible to set reset line to {0}", status);
        }
        return (ret == 1);
    }
    
    /* Control the Fx2 Porte line
     * Issue a command to control the Attenuator and Preselector filter
     * @param porte A byte representing the values of attenuators and preselector filter
     */
    public int setFX2Porte(byte porte) {
        int ret;
        ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        buffer.put(PERSEUS_CMD_FX2PORTE);
        buffer.put(porte);
        IntBuffer transferred = IntBuffer.allocate(1);
        if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_CMD, buffer, transferred, FX2_TIMEOUT)) != LibUsb.SUCCESS || transferred.get() != 2) {
            throw new LibUsbException("Setting Fx2 Porte failed.", ret);
        }
        return ret;
    }
    
    /* Control the Fx2 SIO line
     * Issue a command to enable/disable the Preamp, Dithering & FIFO, and to control to center frequency (when not in wideband mode)
     */
    public int setFX2SIO(boolean wb) {
        ByteBuffer buffer = this.sioControl.getValue(wb);
        IntBuffer transferred = IntBuffer.allocate(1);
        int ret;
        if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_CMD, buffer, transferred, FX2_TIMEOUT)) != LibUsb.SUCCESS) {
            throw new LibUsbException("FPGA command failed", ret);
        }
        if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_STATUS, buffer, transferred, FX2_TIMEOUT)) != LibUsb.SUCCESS) {
            throw new LibUsbException("FPGA read failed", ret);
        }
        //TODO read buffer and set it back into sioControl
        return ret;
    }
    
    /* Get Input Data directly from PERSEUS_EP_DATAIN
     * @param bufferSize the size of the input buffer
     */
    public ByteBuffer getInputData(int bufferSize) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.clear();
        IntBuffer transferred = IntBuffer.allocate(1);
        int ret;
        if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_DATAIN, buffer, transferred, 0)) != LibUsb.SUCCESS) {
            throw new LibUsbException("Getting Input data failed", ret);
        }
        return buffer;
    }
    
    /**
     * Read the EEPROM containing information about the device (SN, HW/SW version, etc.)
     * Use the bulk transfer method
     * @param fw_log The log output file to write to.
     */
    public void readEEPROM(FileWriter fw_log) {
        if (this.firmwareLoaded) {
            int ret;
            ByteBuffer eeprom_read_cmd = ByteBuffer.allocateDirect(4);
            eeprom_read_cmd.order(ByteOrder.LITTLE_ENDIAN);
            eeprom_read_cmd.put(PERSEUS_CMD_EEPROMREAD);
            eeprom_read_cmd.putShort(ADDR_EEPROM_PRODID);
            eeprom_read_cmd.put(ADDR_EEPROM_PRODID_SIZE);
            IntBuffer transferred = IntBuffer.allocate(1);
            try {
                if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_CMD, eeprom_read_cmd, transferred, FX2_TIMEOUT)) == LibUsb.SUCCESS) {
                    ByteBuffer response = ByteBuffer.allocateDirect(ADDR_EEPROM_PRODID_SIZE + 2);
                    response.order(ByteOrder.LITTLE_ENDIAN);
                    transferred.clear();
                    if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_STATUS, response, transferred, FX2_TIMEOUT)) == LibUsb.SUCCESS) {
                        this.eeprom = new EEPROM(response);
                        if (this.eeprom.getProductCode().equals("0x8014")) {
                            if (fw_log != null) {
                                try {
                                    this.eeprom.dump(fw_log);
                                } catch (IOException ex) {
                                    Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        } else {
                            this.preserie = true;
                            Logger.getLogger(PerseusInstance.class.getName()).log(Level.FINE, "Pre-production unit found.");
                        }
                        Logger.getLogger(PerseusInstance.class.getName()).log(Level.FINE, "ReadEPPROM Successful.");
                    } else {
                        throw new LibUsbException("Unable to retrieve EEPROM's content.", ret);
                    }
                } else {
                    throw new LibUsbException("Unable to retrieve EEPROM's content.", ret);
                }
            } catch (LibUsbException ex) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            throw new LibUsbException("You must load a firmware first.", LibUsb.ERROR_OTHER);
        }
    }
    
    /**
     * Download the given @Firmware into this Perseus HW
     * All USB command are issued using bmRequestType=0x40; bRequest=0xA0
     * @param firmware The @Firmware to download into the Perseus
     */
    public void setFirmware(Firmware firmware) {
        this.setFirmware(firmware, true);
    }
    
    /**
     * Set the given @Firmware associated to this Perseus HW
     * All USB command are issued using bmRequestType=0x40; bRequest=0xA0
     * @param firmware The @Firmware to download into the Perseus
     * @param load True if the firmware should be uploaded to the Perseus HW, false otherwise.
     */
    public void setFirmware(Firmware firmware, boolean load) {
        int ret;
        if (!load) {
            this.firmwareLoaded = true;
            this.firmware = firmware;
        } else {
            if (this.fx2Reset(true)) {
                INITIATED_FIRMWARES.put(String.format("BUS#%03d-PORT#%03d", LibUsb.getBusNumber(this.device), LibUsb.getPortNumber(this.device)), firmware);
                for (FirmwareBlock block : firmware.getBlocks()) {
                    ByteBuffer blockBuffer = ByteBuffer.allocateDirect(block.getContent() != null ? block.getContent().length : 0);
                    if (block.getContent() != null) {
                        blockBuffer.put(block.getContent());
                    }
                    if ((ret = LibUsb.controlTransfer(this.handle, FX2_BM_VENDOR_REQUEST, FX2_REQUEST_FIRMWARE_LOAD, (short) block.getRequestValue(), (short) 0, blockBuffer, 1000)) == 0) {
                        throw new LibUsbException("Unable to control transfer : " + block.toString(), ret);
                    }
                }
                this.fx2Reset(false);
                this.firmwareLoaded = true;
                this.firmware = firmware;
            } else {
                throw new LibUsbException("Unable to initiate Firmware Download", LibUsb.ERROR_OTHER);
            }
        }
    }
    
    /**
     * Download the given @FPGA into this Perseus HW
     * All USB command are issued using bulk
     * @param fpga The @FPGA to download into the Perseus
     * @throws java.io.IOException if the file attached to this FPGA configuration is unavailable
     */
    public void setFPGA(FPGA fpga) throws IOException {
        if (this.getFirmware() == null) {
            throw new RuntimeException("A firmware must be set before loading the FPGA.");
        } else if (this.getFirmware() != fpga.getAssociatedFirmware()) {
            throw new RuntimeException("The proposed FPGA does not fit the loaded firmware.");
        }
        int ret;
        ByteBuffer openFPGAChannel = ByteBuffer.allocateDirect(1);
        openFPGAChannel.put(new byte[] { PERSEUS_CMD_FPGARESET });
        IntBuffer transferred = IntBuffer.allocate(1);
        // Issue a FPGA reset command
        if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_CMD, openFPGAChannel, transferred, FX2_TIMEOUT)) == LibUsb.SUCCESS && transferred.get() == 1) {
            byte[] fpgaData;
            try {
                while ((fpgaData = fpga.getNextBlock()) != null) {
                    ByteBuffer fpgacontent = ByteBuffer.allocateDirect(fpgaData.length);
                    fpgacontent.put(fpgaData);
                    transferred.clear();
                    if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_CMD, fpgacontent, transferred, FX2_TIMEOUT)) != LibUsb.SUCCESS || transferred.get() != fpgaData.length) {
                        throw new LibUsbException("Unable to upload an FPGA packet.", ret);
                    }
                }
            } catch (IOException ex) {
                throw ex;
            }
            
            ByteBuffer checkFPGA = ByteBuffer.allocateDirect(1);
            checkFPGA.put(new byte[] { PERSEUS_CMD_FPGACHECK });
            transferred.clear();
            if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_CMD, checkFPGA, transferred, FX2_TIMEOUT)) != LibUsb.SUCCESS || transferred.get() != 1) {
                throw new LibUsbException("Unable to finalize FPGA upload.", ret);
            }
            
            // Wait 50 ms
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Read FPGA check status
            ByteBuffer fpgaStatus = ByteBuffer.allocateDirect(2);
            transferred.clear();
            if ((ret = LibUsb.bulkTransfer(this.handle, PERSEUS_EP_STATUS, fpgaStatus, transferred, FX2_TIMEOUT)) != LibUsb.SUCCESS || transferred.get() != 2) {
                throw new LibUsbException("Unable to check status of FPGA.", ret);
            }

            // Check if FPGA DONE line is 'high'
            if (fpgaStatus.get(1) != 1) {
                throw new LibUsbException(String.format("Configuration of FPGA failed with RC=[%02X,%02X].", new Object[] {fpgaStatus.get(0), fpgaStatus.get(1)}), ret);
            }
        } else {
            throw new LibUsbException("Unable to initiate FPGA upload.", ret);
        }
        this.fpga = fpga;
    }
    
    public int setAttenuator(Attenuator att) {
        int ret = -1;
        if (att != null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Setting Attenuator on Perseus #{0} to attenuation {1}", new Object[]{this.eeprom.getSerialNumber(), att.getName()});
            if (this.device == null) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided Attenuator for Perseus #{0} : device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
            } else if (this.handle == null) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided Attenuator for Perseus #{0} : handle to device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
            } else if (!this.firmwareLoaded) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided Attenuator for Perseus #{0} : firmware not loaded.", new Object[]{this.eeprom.getSerialNumber()});
            } else {
                if (this.preserie) {
                    Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Perseus #{0} is a pre-serie : attenuator forced to 30 dB.", new Object[]{this.eeprom.getSerialNumber()});
                    att = Attenuator._30DB;
                }
                byte fec = this.frontendControl;
                fec &= (~Attenuator.GetMask());
                fec |= att.getMaskedValue();
                if ((ret = this.setFX2Porte(fec)) == LibUsb.SUCCESS) {
                    this.frontendControl = fec;
                } else {
                    throw new LibUsbException("Set attenuator for Perseus #"+this.eeprom.getSerialNumber()+"  failed.", ret);
                }
            }
        } else {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Provided attenuator for Perseus #{0} is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        }
        return ret;
    }
    
    public boolean setADC(boolean enableDither, boolean enablePreamp) {
        boolean ret = false;
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "SetADC called for Perseus #{0} with enableDither={1} and enablePreamp={2}", new Object[]{this.eeprom.getSerialNumber(), enableDither, enablePreamp});
        if (this.device == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided enableDither & enablePreamp for Perseus #{0} : device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.handle == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided enableDither & enablePreamp for Perseus #{0} : handle to device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (!this.firmwareLoaded) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided enableDither & enablePreamp for Perseus #{0} : firmware not loaded.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.fpga == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided enableDither & enablePreamp for Perseus #{0} : FPGA not configured.", new Object[]{this.eeprom.getSerialNumber()});
        } else {
            boolean ditherEnabled = this.sioControl.isDitherEnabled();
            boolean preampEnabled = this.sioControl.isGainHighEnabled();
            this.sioControl.enableDither(enableDither);
            this.sioControl.enablePreamp(enablePreamp);
            int result;
            if ((result = this.setFX2SIO(this.firmware == Firmware.WB)) != LibUsb.SUCCESS) {
                // Reset values in memory to previous values in case of failure
                this.sioControl.enableDither(ditherEnabled);
                this.sioControl.enablePreamp(preampEnabled);
                throw new LibUsbException("Set ADC for Perseus #"+this.eeprom.getSerialNumber()+" with enableDither="+enableDither+" and enablePreamp="+enablePreamp+" failed.", result);
            }
            ret = (result == LibUsb.SUCCESS);
        }
        return ret;
    }

    public boolean setDDCCenterFrequency(double centerFrequencyHz, boolean enablePreselector) {
        boolean ret = false;
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "SetDDCCenterFrequency called for Perseus #{0} with centerFrequencyHz={1} and enablePreselector={2}", new Object[]{this.eeprom.getSerialNumber(), centerFrequencyHz, enablePreselector});
        if (this.device == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided centerFrequencyHz & enablePreselector for Perseus #{0} : device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.handle == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided centerFrequencyHz & enablePreselector for Perseus #{0} : handle to device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (!this.firmwareLoaded) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided centerFrequencyHz & enablePreselector for Perseus #{0} : firmware not loaded.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.fpga == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided centerFrequencyHz & enablePreselector for Perseus #{0} : FPGA not configured.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (centerFrequencyHz < PERSEUS_DDC_FREQ_MIN || centerFrequencyHz > PERSEUS_DDC_FREQ_MAX) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot set provided centerFrequencyHz & enablePreselector for Perseus #{0} : Center frequency out of range.", new Object[]{this.eeprom.getSerialNumber()});
        } else {
            double old_freq = this.sioControl.getFrequency();
            this.sioControl.setFrequencyHz(centerFrequencyHz);
            int result;
            if ((result = this.setFX2SIO(this.firmware == Firmware.WB)) != LibUsb.SUCCESS) {
                this.sioControl.setFrequencyHz(old_freq);
                throw new LibUsbException("Setting center frequency for Perseus #"+this.eeprom.getSerialNumber()+" with frequency="+centerFrequencyHz+" Hz failed.", result);
            } else {
                boolean modified;
                PreselectorFilter old_pf = this.preselector.getFilter();
                if (!enablePreselector) {
                    modified = this.preselector.set(PreselectorFilter.FILTER_WB);
                } else {
                    modified = this.preselector.setFrequency(centerFrequencyHz);
                }
                if (modified) {
                    if ((result = this.setPreselector()) != LibUsb.SUCCESS) {
                        // Reset value in memory to previous value in case of failure
                        this.preselector.resetTo(old_pf);
                        throw new LibUsbException("Setting Preselector for Perseus #"+this.eeprom.getSerialNumber()+" failed.", result);
                    }
                    ret = (result == LibUsb.SUCCESS);
                } else {
                    ret = true;
                }
            }
        }
        return ret;
    }
    
    public int setPreselector() {
        int ret;
        byte fec = this.frontendControl;
        fec &= (~PreselectorFilter.GetMask());
        fec |= this.preselector.getFilter().getMaskedValue();
        if ((ret = this.setFX2Porte(fec)) == LibUsb.SUCCESS) {
            this.frontendControl = fec;
        } else {
            throw new LibUsbException("Modifying preselector filter for Perseus #"+this.eeprom.getSerialNumber()+" failed.", ret);
        }
        return ret;
    }

    public void startAsyncInput(int bufferSize, InputCallback callback, Object extraParams) {
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Starting Async Input for Perseus #{0} with bufferSize={1}", new Object[]{this.eeprom.getSerialNumber(), bufferSize});
        if (this.device == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.handle == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : handle to device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (!this.firmwareLoaded) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : firmware not loaded.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.fpga == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : FPGA not configured.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.inputQueue.isStarted()) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : InputQueue already started.", new Object[]{this.eeprom.getSerialNumber()});
        } else {
            int maxPacketSize = LibUsb.getMaxPacketSize(this.device, PERSEUS_EP_DATAIN);
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Maximum packet size on endpoint {0} for Perseus #{1} is {2}", new Object[] { PERSEUS_EP_DATAIN, this.eeprom.getSerialNumber(), maxPacketSize });
            boolean ok = true;
            if (ok) {
                // Create and submit the data in transfer queue
                if (this.inputQueue.create(this, bufferSize, callback, extraParams)) {
                    // Enable FPGA FIFO
                    this.sioControl.enableFIFOEN(true);
                    if (this.setFX2SIO(false) < 0) {
                        Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : FIFO enabling failed.", new Object[] { this.eeprom.getSerialNumber()});
                    }
                } else {
                    Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot start Async input for Perseus #{0} : Input transfer queue creation failed.", new Object[] { this.eeprom.getSerialNumber()});
                }
            }
        }
    }
    
    public void stopAsyncInput() {
        Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Stopping Async Input for Perseus #{0}.", new Object[]{this.eeprom.getSerialNumber()});
        if (this.device == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot stop Async input for Perseus #{0} : device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.handle == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot stop Async input for Perseus #{0} : handle to device is NULL.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (!this.firmwareLoaded) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot stop Async input for Perseus #{0} : firmware not loaded.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (this.fpga == null) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot stop Async input for Perseus #{0} : FPGA not configured.", new Object[]{this.eeprom.getSerialNumber()});
        } else if (!this.inputQueue.isStarted()) {
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Cannot stop Async input for Perseus #{0} : InputQueue not started.", new Object[]{this.eeprom.getSerialNumber()});
        } else {
            // Cancel the input transfer queue
            if (!this.inputQueue.cancel()) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "Input queue cancelling failed for Perseus #{0}.", new Object[]{this.eeprom.getSerialNumber()});
            }
            
            // Cancel the input transfer queue
            while (!this.inputQueue.isComplete()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
                
            // Print some statistics...
            double elapsed = 1.0E-3 * (this.inputQueue.getStopTime() - this.inputQueue.getStartTime());
            Logger.getLogger(PerseusInstance.class.getName()).log(Level.INFO, "Elapsed time : {0} s - kSamples read: {1} - Rate: {2} kS/s", new Object[]{elapsed, this.inputQueue.getNbBytesReceived()/6000, this.inputQueue.getNbBytesReceived()/6000/elapsed});

            // Free the input transfer queue
            if (!this.inputQueue.free()) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.WARNING, "Input Queue memory freeing failed for Perseus #{0}.", new Object[]{this.eeprom.getSerialNumber()});
            }

            // Disable the FPGA fifo
            this.sioControl.enableFIFOEN(false);
            if (this.setFX2SIO(false) < 0) {
                Logger.getLogger(PerseusInstance.class.getName()).log(Level.SEVERE, "FPGA FIFO disabling failed for Perseus #{0}.", new Object[]{this.eeprom.getSerialNumber()});
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // PERSEUS SDR instance Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * @return the device
     */
    public Device getDevice() {
        return device;
    }

    /**
     * @param device the device to set
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the descriptor
     */
    public ConfigDescriptor getConfigDescriptor() {
        return configDescriptor;
    }

    /**
     * @param descriptor the descriptor to set
     */
    public void setConfigDescriptor(ConfigDescriptor descriptor) {
        this.configDescriptor = descriptor;
    }
    
    /**
     * @return the descriptor
     */
    public DeviceDescriptor getDeviceDescriptor() {
        return deviceDescriptor;
    }

    /**
     * @param descriptor the descriptor to set
     */
    public void setDeviceDescriptor(DeviceDescriptor descriptor) {
        this.deviceDescriptor = descriptor;
    }
    
    /**
     * @return the handle
     */
    public DeviceHandle getHandle() {
        return handle;
    }

    /**
     * @param handle the handle to set
     */
    public void setHandle(DeviceHandle handle) {
        this.handle = handle;
    }

    /**
     * @return the cypressEZUSB value
     */
    public boolean getCypressEZUSB() {
        return this.cypressEZUSB;
    }

    /**
     * @param val the value to set for CypressEZUSB driver
     */
    public void setCypressEZUSB(boolean val) {
        this.cypressEZUSB = val;
    }
    
    /**
     * @return the eeprom
     */
    public EEPROM getEeprom() {
        return eeprom;
    }

    /**
     * @return the firmware
     */
    public Firmware getFirmware() {
        return this.firmware;
    }

    /**
     * @return the PERSEUS_INSTANCES
     */
    public static Set<PerseusInstance> getPerseusInstances() {
        return PERSEUS_INSTANCES;
    }

    /**
     * @return the fpga
     */
    public FPGA getFpga() {
        return fpga;
    }

    /**
     * @return the sioControl
     */
    public SIOControl getSioControl() {
        return sioControl;
    }

}
