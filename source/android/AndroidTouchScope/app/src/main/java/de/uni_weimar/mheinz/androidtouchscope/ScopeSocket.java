package de.uni_weimar.mheinz.androidtouchscope;


import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScopeSocket
{
    /*
     * Size of driver internal IO buffer. Must be multiple of 4 and at least as
     * large as wMaxPacketSize (which is usually 512 bytes).
     */
    private final int USBTMC_SIZE_IOBUFFER = 2048;

    /* Default USB timeout (in milliseconds) */
    private final int USBTMC_TIMEOUT = 10;
    private static final String TAG = "Scope";


    UsbDeviceConnection mConnection = null;
    UsbEndpoint mEndpointOut = null;
    UsbEndpoint mEndpointIn = null;

    private final Object io_lock = new Object();
    private final ReaderThread mReaderThread = new ReaderThread();

    private byte mTag = (byte)1;

    public ScopeSocket(UsbDeviceConnection connection, UsbInterface usbInterface)
    {
        mConnection = connection;

        if(usbInterface != null)
        {
            for (int i = 0; i < usbInterface.getEndpointCount(); i++)
            {
                UsbEndpoint end = usbInterface.getEndpoint(i);
                if (end.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
                {
                    if (end.getDirection() == UsbConstants.USB_DIR_OUT)
                        mEndpointOut = end;
                    else
                        mEndpointIn = end;
                }
            }
        }

        if(mEndpointIn == null || mEndpointOut == null)
            throw new IllegalArgumentException("not all endpoints found");
    }

    public void start()
    {
        mReaderThread.start();
    }

    public void stop()
    {
        synchronized(mReaderThread)
        {
            mReaderThread.postStop();
        }
    }

    public int write(String command)
    {
        byte[] buf = command.getBytes();
        int remaining = buf.length;
        int done = 0;
        int this_part;

        ByteBuffer buffer = ByteBuffer.allocate(USBTMC_SIZE_IOBUFFER);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        synchronized(io_lock)
        {
            while(remaining > 0)
            {
                if(remaining > USBTMC_SIZE_IOBUFFER - 12)
                {
                    this_part = USBTMC_SIZE_IOBUFFER - 12;
                    buffer.put(8,(byte)0);
                }
                else
                {
                    this_part = remaining;
                    buffer.put(8,(byte)1);
                }

                buffer.put(0, (byte) 1);
                buffer.put(1, mTag);
                buffer.put(2, (byte) (~mTag));
                buffer.put(3, (byte) 0);
                buffer.putInt(4, this_part); //bytes 4-7
                // byte 8 set above
                buffer.put(9,(byte)0);
                buffer.put(10,(byte)0);
                buffer.put(11,(byte)0);

                // copy command into buffer starting at pos 12
                for(int i = done, j = 12; i < done + this_part; ++i, ++j)
                    buffer.put(j,buf[i]);

                // must be a multiple of 4, pad with zeros
                int n_bytes = ((12 + this_part) % 4) + 12 + this_part;
                for(int i = 12 + this_part; i < n_bytes; ++i)
                    buffer.put(i,(byte)0);

                int retval = mConnection.bulkTransfer(
                        mEndpointOut, buffer.array(), n_bytes, USBTMC_TIMEOUT);

                mTag++;
                if(mTag == 0)
                    mTag++;

                if(retval < 0)
                {
                    //TODO:  some error handle
                    return retval;
                }

                remaining -= this_part;
                done += this_part;
            }
        }

        return buf.length;
    }

    public int read(int length)
    {
        int remaining = length;
        int done = 0;
        int this_part;


        synchronized(io_lock)
        {
            while(remaining > 0)
            {
                if(remaining > USBTMC_SIZE_IOBUFFER - 12 - 3)
                {
                    this_part = USBTMC_SIZE_IOBUFFER - 12 - 3;
                }
                else
                {
                    this_part = remaining;
                }

                ByteBuffer buffer = ByteBuffer.allocate(USBTMC_SIZE_IOBUFFER);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                buffer.put(0, (byte) 2);
                buffer.put(1, mTag);
                buffer.put(2, (byte) (~mTag));
                buffer.put(3, (byte) 0);
                buffer.putInt(4, this_part); //bytes 4-7
                buffer.put(8, (byte) (0 * 2)); // unsure, may be (1 * 2)
                buffer.putChar(9, '\n'); // check spec.
                buffer.put(10, (byte) 0);
                buffer.put(11, (byte) 0);

                int retval = mConnection.bulkTransfer(mEndpointOut, buffer.array(), 12, USBTMC_TIMEOUT);

                mTag++;
                if(mTag == 0)
                    mTag++;

                if(retval < 0)
                {
                    //TODO:  some error handle
                    return retval;
                }

                UsbRequest request = new UsbRequest();
                request.initialize(mConnection, mEndpointIn);
                request.setClientData(buffer);
                if(!request.queue(buffer, USBTMC_SIZE_IOBUFFER))
                {
                    //TODO: some error handle
                    return 0;
                }

                remaining -= this_part;
                done += this_part;
            }
        }

        return length;
    }

    private class ReaderThread extends Thread
    {
        private boolean mStop;

        public void run()
        {
            mStop = false;

            while(true)
            {
                synchronized(this)
                {
                    if(mStop)
                        return;
                }

                UsbRequest request = mConnection.requestWait();

                Log.i(TAG,"got request");

                if(request != null)
                {
                    ByteBuffer buffer = (ByteBuffer)request.getClientData();
                    request.setClientData(null);

                    if(buffer != null)
                    {
                        int n_characters = buffer.getInt(4);
                    }
                }
            }

        }

        public void postStop()
        {
            mStop = true;
        }
    }
}
