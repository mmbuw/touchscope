package de.uni_weimar.mheinz.androidtouchscope;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;


public class Scope
{
    private static final String TAG = "Scope";

    Activity mActivity = null;

    UsbManager mUsbManager = null;
    UsbDevice mDevice = null;
    UsbDeviceConnection mConnection = null;
    UsbInterface mInterface = null;

    ScopeSocket mScopeSocket = null;

    public Scope(Activity activity)
    {
        mActivity = activity;

        mUsbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);

        for (UsbDevice device : mUsbManager.getDeviceList().values())
        {
            if(isRigolScope(device))
            {
                setDevice(device);
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mActivity.registerReceiver(mUsbReceiver, filter);
    }

    public void readWave()
    {
        if(mScopeSocket == null)
            return;

        mScopeSocket.write(":STOP");
        mScopeSocket.write(":WAV:POIN:MODE NOR");
        mScopeSocket.write(":WAV:DATA? CHAN1");
        mScopeSocket.read(1000);
    }

    private boolean isRigolScope(UsbDevice device)
    {
        String man = device.getManufacturerName();
        String prod = device.getProductName();
        String deviceName = device.getDeviceName();
        int deviceId = device.getDeviceId();
        Log.i(TAG, "Attached device: " + man + " " + prod + " " + deviceName + "--" + deviceId);

        return man.trim().equalsIgnoreCase("Rigol Technologies") &&
                prod.trim().equalsIgnoreCase("DS1000 SERIES");
    }

    public void close()
    {
        if(mScopeSocket != null)
        {
            mScopeSocket.stop();
            mScopeSocket = null;
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
    }

    private boolean setDevice(UsbDevice device)
    {
        close();

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
                    mScopeSocket = new ScopeSocket(mConnection,mInterface);
                    mScopeSocket.start();
                    return true;
                }
                catch (IllegalArgumentException ex)
                {
                    Log.d(TAG,"Socket failed");
                    close();
                }
            }
            else
            {
                Log.d(TAG,"claim interface failed");
                close();
            }
        }
        else
        {
            Log.d(TAG,"open device failed");
            close();
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
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (isRigolScope(device))
                {
                    Log.i(TAG,"attaching device");
                    setDevice(device);
                }
            }
            else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.equals(mDevice))
                {
                    Log.i(TAG,"detaching device");
                    close();
                }
            }
        }
    };
}
