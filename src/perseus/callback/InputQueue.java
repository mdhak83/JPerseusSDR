package perseus.callback;

import perseus.PerseusInstance;
import static perseus.utils.Definitions.PERSEUS_EP_DATAIN;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.usb4java.LibUsb;
import org.usb4java.Transfer;
import org.usb4java.TransferCallback;

/**
 * This class organizes the asynchronous transfers of data from the USB buffers to the application.
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
public class InputQueue implements TransferCallback {
    
    private static final int INPUT_QUEUE_TIMEOUT_MS = 80;
    private static final int QUEUE_SIZE = 8;
    
    private PerseusInstance perseus;
    private boolean started = false;
    private long startTime = -1;
    private long stopTime = -1;
    private int expectedIndex = 0;
    private boolean cancelling = false;
    private int nbBytesReceived = 0;
    private int timeout_ms = 500;
    private TransferInput transferInput = null;
    private ByteBuffer transferBuffer = null;
    
    private InputCallback callback = null;
    private Object parameters = null;
    
    public InputQueue() {
    }
    
    public boolean create(PerseusInstance perseus, int bufferSize, InputCallback callback, Object extraParams) {
        boolean ret = false;
        try {
            if (!this.started) {
                this.startTime = -1;
                this.stopTime = -1;
                this.perseus = perseus;
                this.started = true;
                this.cancelling = false;
                this.expectedIndex = 0;
                this.nbBytesReceived = 0;
                this.callback = callback;
                this.parameters = extraParams;
                
                
                // Allocate and fill the transfers --> TransferInput and ByteBuffer ('bufferSize' bytes wide )
                this.transferInput = new TransferInput();
                this.transferInput.setQueue(this);
                this.transferInput.setCancel(false);
                this.transferInput.setCancelled(false);
                Transfer transfer = LibUsb.allocTransfer(0);
                this.transferBuffer = ByteBuffer.allocateDirect(bufferSize);
                LibUsb.fillBulkTransfer(transfer, this.perseus.getHandle(), PERSEUS_EP_DATAIN, this.transferBuffer, this, this.transferInput, 0);
                this.transferInput.setTransfer(transfer);
                if (this.transferInput == null) {
                    throw new Exception("LibUsb Transfer allocation impossible to assign.");
                }

                // Submit transfer
                LibUsb.submitTransfer(this.transferInput.getTransfer());
            }
            this.startTime = System.currentTimeMillis();
            ret = true;
        } catch (Exception ex) {
            Logger.getLogger(InputQueue.class.getName()).log(Level.WARNING, "Cannot create input queue for Perseus #" + this.perseus.getEeprom().getSerialNumber(), ex);
            // Free allocated transfers
            if (this.transferInput.getTransfer() != null) {
                LibUsb.freeTransfer(this.transferInput.getTransfer());
            }
            // ... and data buffers
            this.transferInput = null;
            this.transferBuffer = null;
        }
        return ret;
    }
    
    public boolean cancel() {
        boolean ret = false;
        Logger.getLogger(InputQueue.class.getName()).log(Level.INFO, "Cancelling all transfers for Perseus #{0}.", new Object[] { this.perseus.getEeprom().getSerialNumber()});
        if (this.transferInput != null) {
            this.stopTime = System.currentTimeMillis();
            this.callback = null;
            if (!this.isComplete()) {
                this.cancelling = true;
                LibUsb.cancelTransfer(this.transferInput.getTransfer());
            }
            ret = true;
        }
        return ret;
    }
    
    public boolean free() {
        boolean ret = true;
        
        if (this.transferInput != null) {
            LibUsb.freeTransfer(this.transferInput.getTransfer());
            this.transferInput = null;
            this.transferBuffer = null;
        }
        
        return ret;
    }
    
    public boolean isComplete() {
        boolean ret = true;
        
        if (this.transferInput != null && !this.transferInput.isCancelled()) {
            ret = false;
        }
        
        if (ret) {
            Logger.getLogger(InputQueue.class.getName()).log(Level.INFO, "All transfers successfully cancelled for Perseus #{0}.", new Object[] { this.perseus.getEeprom().getSerialNumber()});
        }
        
        return ret;
    }

    @Override
    public void processTransfer(Transfer transfer) {
        TransferInput params = (TransferInput) transfer.userData();
        InputQueue queue = params.getQueue();
        
        if (queue.isCancelling()) {
            params.setCancelled(true);
            Logger.getLogger(InputCallback.class.getName()).log(Level.INFO, "Input transfer cancelled.");
            queue.isComplete();
        } else {
            int status = params.getTransfer().status();
            switch(status) {
		case LibUsb.TRANSFER_COMPLETED:
                    // Transfer completed without error.
                    queue.increaseBytesReceivedBy(transfer.actualLength());
                    if (transfer.actualLength() == transfer.length()) {
                        if (queue.hasCallback()) {
                            queue.getCallback().callback(transfer.buffer(), transfer.length(), queue.getParameters());
                        }
                    } else {
                        Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer actual length < expected length : actual={0} and expected={1}.", new Object[] { transfer.actualLength(), transfer.length() });
                    }
                    break;
		case LibUsb.TRANSFER_TIMED_OUT:
                    // Transfer timed out.
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer timed out : actual length={0}.", new Object[] { transfer.actualLength() });
                    break;
		case LibUsb.TRANSFER_ERROR:
                    // Transfer failed.
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer failed.");
                    params.setCancelled(true);
                    queue.isComplete();
                    break;
		case LibUsb.TRANSFER_CANCELLED:
                    // Transfer was cancelled.
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer cancelled.");
                    params.setCancelled(true);
                    queue.isComplete();
                    break;
		case LibUsb.TRANSFER_STALL: 	
                    // Halt condition detected (endpoint stalled) for bulk/interrupt endpoints.
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer stalled.");
                    params.setCancelled(true);
                    queue.isComplete();
                    break;
		case LibUsb.TRANSFER_NO_DEVICE:
                    // Device was disconnected.
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer error : the device was disconnected.");
                    params.setCancelled(true);
                    queue.isComplete();
                    break;
		case LibUsb.TRANSFER_OVERFLOW:
                    // Device sent more data than requested. 
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer overflow.");
                    params.setCancelled(true);
                    queue.isComplete();
                    break;
		default:
                    Logger.getLogger(InputCallback.class.getName()).log(Level.WARNING, "Input transfer unexpected status : #{0}.", new Object[] { status });
                    params.setCancelled(true);
                    queue.isComplete();
                    break;
            }

            // Resubmit transfer
            LibUsb.submitTransfer(transfer);
        }
    }

    public boolean isStarted() {
        return this.started;
    }
    
    public boolean isCancelling() {
        return this.cancelling;
    }
    
    public int increaseBytesReceivedBy(int number) {
        this.nbBytesReceived += number;
        return this.nbBytesReceived;
    }

    /**
     * @return the expectedIndex
     */
    public int getExpectedIndex() {
        return expectedIndex;
    }
    
    public boolean hasCallback() {
        return this.callback != null;
    }

    /**
     * @return the callback
     */
    public InputCallback getCallback() {
        return callback;
    }

    /**
     * @return the parameters
     */
    public Object getParameters() {
        return parameters;
    }

    /**
     * @param expectedIndex the expectedIndex to set
     */
    public void setExpectedIndex(int expectedIndex) {
        this.expectedIndex = expectedIndex;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return the stopTime
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * @return the nbBytesReceived
     */
    public int getNbBytesReceived() {
        return nbBytesReceived;
    }
    
}
