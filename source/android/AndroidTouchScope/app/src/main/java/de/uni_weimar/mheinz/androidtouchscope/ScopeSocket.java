package de.uni_weimar.mheinz.androidtouchscope;


import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class ScopeSocket
{
    /*
     * Size of driver internal IO buffer. Must be multiple of 4 and at least as
     * large as wMaxPacketSize (which is usually 512 bytes).
     */
    private final int USBTMC_SIZE_IOBUFFER = 2048;

    /* Default USB timeout (in milliseconds) */
    private final int USBTMC_TIMEOUT = 5000;
    private static final String TAG = "Scope";

    private UsbDeviceConnection mConnection = null;
    private UsbEndpoint mEndpointOut = null;
    private UsbEndpoint mEndpointIn = null;
    private final LinkedList<UsbRequest> mInRequestPool = new LinkedList<UsbRequest>();

    private final Object io_lock = new Object();
    private final Object reader_lock = new Object();
    private int read_bits = 0; //locked by reader_lock
    private int read_offset = 0; //locked by reader_lock
    private boolean read_ready = false;

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

    private UsbRequest getInRequest()
    {
        synchronized(mInRequestPool)
        {
            if(mInRequestPool.isEmpty())
            {
                UsbRequest request = new UsbRequest();
                request.initialize(mConnection, mEndpointIn);
                return request;
            }
            else
            {
                return mInRequestPool.removeFirst();
            }
        }
    }

    private void returnInRequest(UsbRequest request)
    {
        synchronized(mInRequestPool)
        {
            mInRequestPool.add(request);
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
                int retval = 0;
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

                do
                {
                    retval = mConnection.bulkTransfer(mEndpointOut, buffer.array(), n_bytes, USBTMC_TIMEOUT);
                    if(retval < 0)
                        break;
                    n_bytes -= retval;
                } while(n_bytes > 0);

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
        int this_part;

        ByteBuffer wholeBuffer = ByteBuffer.allocate(length);

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
                if(mTag <= 0)
                    mTag = 1;

                if(retval < 0)
                {
                    //TODO:  some error handle
                    return retval;
                }

                ReaderMessage message = new ReaderMessage(wholeBuffer, length,
                                                          buffer, this_part);

                UsbRequest request = getInRequest();
                request.setClientData(message);

                while(read_ready == false)
                {
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch(InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }
                }

                if(!request.queue(buffer, USBTMC_SIZE_IOBUFFER))
                {
                    //TODO: some error handle
                    return 0;
                }

                synchronized(reader_lock)
                {
                     if(read_bits == -1 ) //end-fo-message received from device
                        remaining = 0;
                    else
                        remaining -= read_bits;

                    if(remaining <= 0)
                        read_offset = 0;
                }
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

                synchronized(reader_lock)
                {
                    read_ready = true;

                    UsbRequest request = mConnection.requestWait();

                    try
                    {
                        Thread.sleep(500);
                    }
                    catch(InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }


                    if(request == null)
                        break;

                    Log.i(TAG, "got request");

                    ReaderMessage message = (ReaderMessage) request.getClientData();
                    request.setClientData(null);

                    if(message != null)
                    {
                        read_bits = message.partBuffer.getInt(4);
                        int partSize = message.partSize;
                        if(read_bits > partSize)
                            read_bits = partSize;
                        for(int j = 12; j < read_bits + 12; j++, read_offset++)
                            message.wholeBuffer.put(read_offset, message.partBuffer.get(j));

                        if(USBTMC_SIZE_IOBUFFER >= read_bits + 12 &&
                            message.partBuffer.get(8) == 0x01) // end of message bit
                        {
                            read_bits = -1;
                        }

                        try
                        {
                            Log.i(TAG, new String(message.wholeBuffer.array(), "UTF-8"));
                        }
                        catch(UnsupportedEncodingException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    if(request.getEndpoint() == mEndpointIn)
                        returnInRequest(request);

                    read_ready = false;
                }
            }
        }

        public void postStop()
        {
            mStop = true;
        }
    }

    private class ReaderMessage
    {
        public final ByteBuffer wholeBuffer;
        public final ByteBuffer partBuffer;
        public final int wholeSize;
        public final int partSize;

        public ReaderMessage(ByteBuffer wholeBuffer, int wholeSize,
                             ByteBuffer partBuffer, int partSize)
        {
            this.wholeBuffer = wholeBuffer;
            this.partBuffer = partBuffer;
            this.wholeSize = wholeSize;
            this.partSize = partSize;
        }
    }
}
