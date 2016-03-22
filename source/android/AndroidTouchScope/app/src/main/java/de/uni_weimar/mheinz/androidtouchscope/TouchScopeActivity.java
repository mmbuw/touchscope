package de.uni_weimar.mheinz.androidtouchscope;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.Iterator;


public class TouchScopeActivity extends Activity
{
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String TAG = "TouchScopeActivity";

    UsbManager mUsbManager;
    PendingIntent mPermissionIntent;
    ScopeInterface mScopeInterface = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_scope);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);


        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext())
        {
            UsbDevice device = deviceIterator.next();
            String man = device.getManufacturerName();
            String prod = device.getProductName();
            String deviceName = device.getDeviceName();
            int deviceId = device.getDeviceId();
            Log.i(TAG, "Attached device: " + man + " " + prod + " " + deviceName + "--" + deviceId);

            if(man.trim().equalsIgnoreCase("Rigol Technologies") &&
               prod.trim().equalsIgnoreCase("DS1000 SERIES"))
            {
                mUsbManager.requestPermission(device, mPermissionIntent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_touch_scope, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        private static final String TAG = "mUsbReceiver";

        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        if(device != null)
                        {
                            if(mScopeInterface != null)
                                mScopeInterface.close();

                            mScopeInterface = new ScopeInterface(mUsbManager,device);

                        }
                    }
                    else
                    {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
            else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && mScopeInterface != null)
                {
                    if(mScopeInterface.isDeviceEqual(device))
                        mScopeInterface.close();
                }
            }
        }
    };

}
