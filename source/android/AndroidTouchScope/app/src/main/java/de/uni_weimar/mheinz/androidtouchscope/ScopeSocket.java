package de.uni_weimar.mheinz.androidtouchscope;


import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

public class ScopeSocket
{
    UsbDeviceConnection mConnection = null;
    UsbEndpoint mEndpointOut = null;
    UsbEndpoint mEndpointIn = null;

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

    public void close()
    {

    }

    public int write(String command)
    {

        return 0;
    }

    /*
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
                if(request != null)
                {

                }
            }

        }

        public void postStop()
        {
            mStop = true;
        }
    }
    */
}
