package de.uni_weimar.mheinz.androidtouchscope;

import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbController
{
    private static final String TAG = "UsbController";

    private AppCompatActivity mActivity = null;
    private int mVendorId = 0;
    private int mProductId = 0;

    private UsbManager mUsbManager = null;
    private UsbDevice mDevice = null;
    private UsbDeviceConnection mConnection = null;
    private UsbInterface mInterface = null;

    private TmcSocket mTmcSocket = null;
    private OnDeviceChange mOnDeviceChange;

    public UsbController(AppCompatActivity activity, int vendorId, int productId)
    {
        mActivity = activity;
        mVendorId = vendorId;
        mProductId = productId;
    }

    public void open(OnDeviceChange onDeviceChange)
    {
        mOnDeviceChange = onDeviceChange;

        mUsbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);

        for (UsbDevice device : mUsbManager.getDeviceList().values())
        {
            if(isCorrectScope(device))
            {
                setDevice(device);
            }
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

    /*public int clear()
    {
        if(mTmcSocket == null)
            return 0;

        return mTmcSocket.clear();
    }*/

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
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (isCorrectScope(device))
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
