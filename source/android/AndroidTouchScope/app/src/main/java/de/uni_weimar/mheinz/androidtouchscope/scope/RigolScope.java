package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.app.Activity;
import android.os.Handler;

import java.io.UnsupportedEncodingException;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class RigolScope implements BaseScope
{
    private static final int RIGOL_VENDOR_ID = 6833;
    private static final int RIGOL_PRODUCT_ID = 1416;

    private static final String CHAN_1 = "CHAN1";
    private static final String CHAN_2 = "CHAN2";
    private static final String CHAN_MATH = "MATH";

    private static final int READ_RATE = 100;

    private Activity mActivity;
    private final Object mControllerLock = new Object();
    private UsbController mUsbController = null;
    private boolean mIsConnected = false;
    private OnReceivedName mOnReceivedName;

    private WaveRequestPool mWaves1 = new WaveRequestPool(POOL_SIZE);
    private WaveRequestPool mWaves2 = new WaveRequestPool(POOL_SIZE);
    private WaveRequestPool mWavesM = new WaveRequestPool(POOL_SIZE);
    private TimeData mTimeData = new TimeData();

    private int mActiveWave = -1;

    private Handler mReadHandler = new Handler();

    public RigolScope(Activity activity)
    {
        mActivity = activity;
    }

    public void open(OnReceivedName onReceivedName)
    {
        final RigolScope scope = this;
        mOnReceivedName = onReceivedName;

        mUsbController = new UsbController(mActivity, RIGOL_VENDOR_ID, RIGOL_PRODUCT_ID);
        mUsbController.open(new UsbController.OnDeviceChange()
        {
            @Override
            public void start()
            {
                mIsConnected = true;
                doCommand(Command.GET_NAME, 0, false, null);
                initSettings();
            }

            @Override
            public void stop()
            {
                mIsConnected = false;
                scope.stop();
            }
        });
    }

    public void close()
    {
        stop();

        if (mUsbController != null)
            mUsbController.close();
        mUsbController = null;
    }

    public void start()
    {
        stop();
        mReadHandler.postDelayed(mReadRunnable, 0);
    }

    public void stop()
    {
        mReadHandler.removeCallbacks(mReadRunnable);
    }

    public boolean isConnected()
    {
        return mIsConnected;
    }


    //////////////////////////////////////////////////////////////////////////
    //
    // Get Wave Data
    //
    //////////////////////////////////////////////////////////////////////////

    public WaveData getWave(int chan)
    {
        WaveData waveData = null;
        switch (chan)
        {
            case 1:
                waveData = mWaves1.peek();
                break;
            case 2:
                waveData = mWaves2.peek();
                break;
            case 3:
                waveData = mWavesM.peek();
                break;
        }
        return waveData;
    }

    public TimeData getTimeData()
    {
        return mTimeData;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Scope Functions
    //
    //////////////////////////////////////////////////////////////////////////

    public int doCommand(Command command, int channel, boolean force, Object specialData)
    {
        int val = 0;

        if (mUsbController == null || !mIsConnected)
            return val;

        synchronized (mControllerLock)
        {
            switch (command)
            {
                case IS_CHANNEL_ON:
                    val = isChannelOn(channel) ? 1 : 0;
                    break;
                case GET_NAME:
                    String name = getName();
                    if (mOnReceivedName != null)
                        mOnReceivedName.returnName(name);
                    break;
                case SET_ACTIVE_CHANNEL:
                    mActiveWave = channel;
                    break;
                case SET_VOLTAGE_OFFSET:
                    Float off = (Float)specialData;
                    setVoltageOffset(channel,off);
                    break;
                case NO_COMMAND:
                default:
                    break;
            }

            if (force)
                forceCommand();
        }

        return val;
    }

    private void initSettings()
    {
        if (mUsbController == null)
            return;

        synchronized (mControllerLock)
        {
            mUsbController.write(":WAV:POIN:MODE NOR");
            forceCommand();
        }
    }

    private String getName()
    {
        mUsbController.write("*IDN?");
        int[] data = mUsbController.read(300);
        String name = new String(intArrayToByteArray(data));
        String[] parts = name.split(",");

        return parts[0] + " " + parts[1];
    }

    private boolean isChannelOn(int channel)
    {
        mUsbController.write(":" + getChannel(channel) + ":DISP?");
        int[] on = mUsbController.read(20);
        return on != null && on.length > 0 && on[0] == 49;
    }

    private void setVoltageOffset(int channel, float value)
    {
        WaveData data = getWave(channel);
    //    float offset1 = (float)actualVoltage(data.voltageOffset,data.voltageScale, (int)value);
        double offset = (value * data.voltageScale) + data.voltageOffset;

        mUsbController.write(":" + getChannel(channel) + ":OFFS " + (float)offset);
    }

    // use to re-allow human actions with the scope
    private void forceCommand()
    {
        mUsbController.write(":KEY:FORC");
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Helper utility functions
    //
    //////////////////////////////////////////////////////////////////////////

    private String getChannel(int chan)
    {
        String channel;
        switch (chan)
        {
            case 2:
                channel = CHAN_2;
                break;
            case 3:
                channel = CHAN_MATH;
                break;
            case 1:
            default:
                channel = CHAN_1;
                break;
        }

        return channel;
    }

    private byte[] intArrayToByteArray(int[] intArray)
    {
        if(intArray != null)
        {
            byte[] bytes = new byte[intArray.length];
            for(int i = 0; i < intArray.length; ++i)
            {
                bytes[i] = (byte) intArray[i];
            }
            return bytes;
        }
        else
            return new byte[]{};
    }

    private double bytesToDouble(int[] data)
    {
        double value = 0.0;
        try
        {
            String strValue = new String(intArrayToByteArray(data), "UTF-8");
            value = Double.parseDouble(strValue);
        }
        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        return value;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Collect Wave Data at timed intervals
    //
    //////////////////////////////////////////////////////////////////////////

    private Runnable mReadRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            readWave(1);
            readWave(2);
            readWave(3);
            readTimeData();

            mReadHandler.postDelayed(this, READ_RATE);
        }
    };

    private void readWave(int channel)
    {
        WaveData waveData;
        switch(channel)
        {
            case 1:
                waveData = mWaves1.requestWaveData();
                break;
            case 2:
                waveData = mWaves2.requestWaveData();
                break;
            case 3:
            default:
                waveData = mWavesM.requestWaveData();
                break;
        }

        synchronized(mControllerLock)
        {
            if(isChannelOn(channel))
            {
                // get the raw data
                mUsbController.write(":WAV:DATA? " + getChannel(channel));
                waveData.data = mUsbController.read(SAMPLE_LENGTH);

                //Get the voltage scale
                mUsbController.write(":" + getChannel(channel) + ":SCAL?");
                waveData.voltageScale = bytesToDouble(mUsbController.read(20));

                // And the voltage offset
                mUsbController.write(":" + getChannel(channel) + ":OFFS?");
                waveData.voltageOffset = bytesToDouble(mUsbController.read(20));
            }
            else
            {
                waveData.data = null;
            }
        }

        switch (channel)
        {
            case 1:
                mWaves1.add(waveData);
                break;
            case 2:
                mWaves2.add(waveData);
                break;
            case 3:
                mWavesM.add(waveData);
                break;
        }
    }

    private void readTimeData()
    {
        synchronized(mControllerLock)
        {
            // Get the timescale
            mUsbController.write(":TIM:SCAL?");
            mTimeData.timeScale = bytesToDouble(mUsbController.read(20));

            // Get the timescale offset
            mUsbController.write(":TIM:OFFS?");
            mTimeData.timeOffset = bytesToDouble(mUsbController.read(20));

            forceCommand();
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Public Static Methods
    //
    //////////////////////////////////////////////////////////////////////////

    /**
     * based on the point value, returns the actual voltage with calculations from scale and offset
     * @param offset - voltage offset
     * @param scale - voltage scale
     * @param point - point formatted as a unsigned byte in an int
     * @return - the actual voltage
     */
    public static double actualVoltage(double offset, double scale, int point)
    {
        // Walk through the data, and map it to actual voltages
        // This mapping is from Cibo Mahto
        // First invert the data
        double tPoint = point * -1 + 255;

        // Now, we know from experimentation that the scope display range is actually
        // 30-229.  So shift by 130 - the voltage offset in counts, then scale to
        // get the actual voltage.
        tPoint = (tPoint - 130.0 - (offset / scale * 25)) / 25 * scale;
        return tPoint;
    }
}
