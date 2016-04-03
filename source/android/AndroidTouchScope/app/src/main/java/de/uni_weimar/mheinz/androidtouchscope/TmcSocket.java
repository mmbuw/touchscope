package de.uni_weimar.mheinz.androidtouchscope;


import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;

public class TmcSocket
{
    /*
     * Size of driver internal IO buffer. Must be multiple of 4 and at least as
     * large as wMaxPacketSize (which is usually 512 bytes).
     */
    private final int USBTMC_SIZE_IOBUFFER = 2048;

    /* Default USB timeout (in milliseconds) */
    private final int USBTMC_TIMEOUT = 5000;
    /*private final int USBTMC_MAX_READS_TO_CLEAR_BULK_IN = 100;
    private final int USBTMC_REQUEST_INITIATE_CLEAR = 5;
    private final int USB_RECIP_INTERFACE = 0x01;
    private final int USBTMC_STATUS_SUCCESS = 0x01;
    private final int USBTMC_REQUEST_CHECK_CLEAR_STATUS = 6;
    private final int USBTMC_STATUS_PENDING = 0x02;*/

    private static final String TAG = "TmcSocket";

    private UsbDeviceConnection mConnection = null;
    private UsbEndpoint mEndpointOut = null;
    private UsbEndpoint mEndpointIn = null;
    private final LinkedList<UsbRequest> mInRequestPool = new LinkedList<UsbRequest>();

    private final Object io_lock = new Object();
    private byte mTag = (byte)1;

    public TmcSocket(UsbDeviceConnection connection, UsbInterface usbInterface)
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

    public void close()
    {
        synchronized(mInRequestPool)
        {
            while(!mInRequestPool.isEmpty())
            {
                UsbRequest request = mInRequestPool.removeFirst();
                request.cancel();
                request.close();
            }
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

    public byte[] read(int length)
    {
        int remaining = length;
        int this_part;
        int done = 0;

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
                    return null;
                }

                UsbRequest request = getInRequest();
                request.setClientData(buffer);

                if(!request.queue(buffer, USBTMC_SIZE_IOBUFFER))
                {
                    //TODO: some error handle
                    return null;
                }

                request = mConnection.requestWait();
                buffer = (ByteBuffer) request.getClientData();
                request.setClientData(null);

                if(buffer != null)
                {
                    int n_characters = buffer.getInt(4);
                    if(n_characters > this_part)
                        n_characters = this_part;
                    for(int j = 12; j < n_characters + 12; j++, done++)
                        wholeBuffer.put(done, buffer.get(j));

                    if(USBTMC_SIZE_IOBUFFER >= n_characters + 12 &&
                            buffer.get(8) == 0x01) // end of message bit
                    {
                        remaining = 0;
                    }
                    else
                    {
                        remaining -= n_characters;
                    }
                }

                returnInRequest(request);
            }

            /*try
            {
                Log.i(TAG, new String(wholeBuffer.array(), "UTF-8"));
            }
            catch(UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }*/
        }

        return Arrays.copyOfRange(wholeBuffer.array(),0,done);
    }

    /*public int clear()
    {
        int rv = 0;
        int max_size = 0;

        ByteBuffer buffer = ByteBuffer.allocate(USBTMC_SIZE_IOBUFFER);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        rv = mConnection.controlTransfer(
                UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE,
                USBTMC_REQUEST_INITIATE_CLEAR,
                0, 0, buffer.array(), 1, USBTMC_TIMEOUT);

        if(rv < 0)
        {
            //TODO: error handle
            return rv;
        }

        if(buffer.get(0) != USBTMC_STATUS_SUCCESS)
        {
            //TODO: error handle
            return -1;
        }

        max_size = mEndpointIn.getMaxPacketSize();
        if(max_size == 0)
        {
            // TODO: error handle
            return -1;
        }

        // check clear status
        while(true)
        {
            rv = mConnection.controlTransfer(
                    UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_CLASS | USB_RECIP_INTERFACE,
                    USBTMC_REQUEST_CHECK_CLEAR_STATUS,
                    0, 0, buffer.array(), 2, USBTMC_TIMEOUT);

            if(rv < 0)
            {
                //TODO: error handle
                return rv;
            }

            if(buffer.get(0) == USBTMC_STATUS_SUCCESS)
            {
                break;
            }

            if(buffer.get(0) != USBTMC_STATUS_PENDING)
            {
                //TODO: error handle
                return -1;
            }

            if(buffer.get(1) == 1)
            {
                int n = 0;
                do
                {
                    UsbRequest request = getInRequest();
                    request.setClientData(buffer);

                    if(!request.queue(buffer, USBTMC_SIZE_IOBUFFER))
                    {
                        //TODO: some error handle
                        return -1;
                    }
                    request = mConnection.requestWait();
                    n++;
                    returnInRequest(request);
                } while(rv == max_size && n < USBTMC_MAX_READS_TO_CLEAR_BULK_IN);
            }
            if(rv == max_size)
            {
                // TODO: error handle
                return 0;
            }
        }

        /// clear halt
        int endp = mEndpointOut.getEndpointNumber();//.getAddress();
        endp = ((endp) >> 15) & 0xf;
        if((endp & UsbConstants.USB_DIR_IN) > 0)
            endp |= UsbConstants.USB_DIR_IN;

        rv = mConnection.controlTransfer(
                0x02, // USB_RECIP_ENDPOINT
                0x01, // USB_REQ_CLEAR_FEATURE
                0, //USB_ENDPOINT_HALT
                endp,
                null, 0, USBTMC_TIMEOUT);

        if(rv < 0)
            return rv;


        return rv;
    }*/
}
