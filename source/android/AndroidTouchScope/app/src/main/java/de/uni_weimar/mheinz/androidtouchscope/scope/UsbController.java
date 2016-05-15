/*
 * MIT License
 *
 * Copyright (c) 2016 Matthew Heinz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.app.Activity;
//import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

class UsbController
{
    private static final String TAG = "UsbController";

    private Activity mActivity = null;
    private int mVendorId = 0;
    private int mProductId = 0;

    private UsbManager mUsbManager = null;
    private UsbDevice mDevice = null;
    private UsbDeviceConnection mConnection = null;
    private UsbInterface mInterface = null;

    private TmcSocket mTmcSocket = null;
    private OnDeviceChange mOnDeviceChange;

    public UsbController(Activity activity, int vendorId, int productId)
    {
        mActivity = activity;
        mVendorId = vendorId;
        mProductId = productId;
    }

    public void open(OnDeviceChange onDeviceChange)
    {
        mOnDeviceChange = onDeviceChange;

        mUsbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);

        try
        {
            for (UsbDevice device : mUsbManager.getDeviceList().values())
            {
                if (isCorrectScope(device))
                {
                    setDevice(device);
                }
            }
        }
        catch (NullPointerException ex)
        {
            ex.printStackTrace();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mActivity.registerReceiver(mUsbReceiver, filter);
    }

    public void close()
    {
        mActivity.unregisterReceiver(mUsbReceiver);
        closeConnection();
    }

    private void closeConnection()
    {
        if(mTmcSocket != null)
        {
            mTmcSocket.close();
            mTmcSocket = null;
        }

        if(mConnection != null)
        {
            if(mInterface != null)
            {
                mConnection.releaseInterface(mInterface);
            }
            mConnection.close();
            mInterface = null;
        }
        mDevice = null;
        mInterface = null;

        mOnDeviceChange.stop();
    }

    private boolean isCorrectScope(UsbDevice device)
    {
        String deviceName = device.getDeviceName();
        int vendorId = device.getVendorId();
        int productId = device.getProductId();

        Log.i(TAG, "Attached device: " + deviceName + "::" + vendorId + "--" + productId);

        return vendorId == mVendorId && productId == mProductId;

    }

    public int write(String command)
    {
        if(mTmcSocket == null)
            return 0;

        return mTmcSocket.write(command);
    }

    public int[] read(int length)
    {
        if(mTmcSocket == null)
            return null;

        return mTmcSocket.read(length);
    }

    private boolean setDevice(UsbDevice device)
    {
        closeConnection();

        if (device == null)
            return false;

        mConnection = mUsbManager.openDevice(device);
        if(mConnection != null)
        {
            UsbInterface usbInterface = device.getInterface(0);
            if (mConnection.claimInterface(usbInterface, true))
            {
                mDevice = device;
                mInterface = usbInterface;

                try
                {
                    mTmcSocket = new TmcSocket(mConnection,mInterface);
                    if(mOnDeviceChange != null)
                        mOnDeviceChange.start();

                    return true;
                }
                catch (IllegalArgumentException ex)
                {
                    Log.d(TAG,"Socket failed");
                    closeConnection();
                }
            }
            else
            {
                Log.d(TAG,"claim interface failed");
                closeConnection();
            }
        }
        else
        {
            Log.d(TAG,"open device failed");
            closeConnection();
        }

        return false;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        private static final String TAG = "mUsbReceiver";

        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (isCorrectScope(device))
                {
                    Log.i(TAG,"attaching device");
                    setDevice(device);
                }
            }
            else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.equals(mDevice))
                {
                    Log.i(TAG,"detaching device");
                    closeConnection();
                }
            }
        }
    };

    public interface OnDeviceChange
    {
        void start();
        void stop();
    }
}
