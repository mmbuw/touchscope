package de.uni_weimar.mheinz.androidtouchscope;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;


public class ScopeInterface
{
    UsbManager mUsbManager = null;
    UsbDevice mDevice = null;
 //   UsbInterface mEndpoints = null;
    UsbDeviceConnection mConnection = null;
    UsbEndpoint mEndpointOut;
    UsbEndpoint mEndpointIn;
    UsbEndpoint mEndpointInterupt;

    public ScopeInterface(UsbManager usbManager, UsbDevice device)
    {
        mUsbManager = usbManager;
        mDevice = device;
        UsbInterface usbInt = device.getInterface(0);

        UsbEndpoint endOut, endin, endint;
        endOut = endin = endint = null;
        for(int i = 0; i < usbInt.getEndpointCount(); i++)
        {
            UsbEndpoint end = usbInt.getEndpoint(i);
            if(end.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
            {
                if(end.getDirection() == UsbConstants.USB_DIR_OUT)
                    endOut = end;
                else
                    endin = end;
            }
            else if(end.getType() == UsbConstants.USB_ENDPOINT_XFER_INT)
            {
                int dir = end.getDirection();
                endint = end;
            }
        }

        if (endOut == null || endin == null || endint == null)
        {
            throw new IllegalArgumentException("not all endpoints found");
        }
        mEndpointOut = endOut;
        mEndpointIn = endin;
        mEndpointInterupt = endint;
    }

    public boolean open()
    {
        if(mUsbManager != null)
        {
            mConnection = mUsbManager.openDevice(mDevice);
            mConnection.claimInterface(mDevice.getInterface(0), true);
            return true;
        }
        return false;
    }

    public void close()
    {
        if(mConnection != null)
        {
            mConnection.releaseInterface(mDevice.getInterface(0));
            mConnection.close();
        }
        mConnection = null;
        mDevice = null;
    }

    public boolean isDeviceEqual(UsbDevice device)
    {
        if(mDevice != null)
            return mDevice.equals(device);
        return false;
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
}
