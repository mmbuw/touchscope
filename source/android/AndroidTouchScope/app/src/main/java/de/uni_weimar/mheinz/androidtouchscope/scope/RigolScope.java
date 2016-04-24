package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.app.Activity;
import android.graphics.RectF;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class RigolScope extends BaseScope
{
    private static final int RIGOL_VENDOR_ID = 6833;
    private static final int RIGOL_PRODUCT_ID = 1416;

    private static final String CHAN_1 = "CHAN1";
    private static final String CHAN_2 = "CHAN2";
    private static final String CHAN_MATH = "MATH";

    private final Activity mActivity;
    private UsbController mUsbController = null;

    public RigolScope(Activity activity)
    {
        mActivity = activity;
    }

    public void open(OnReceivedName onReceivedName)
    {
        super.open(onReceivedName);

        final RigolScope scope = this;

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
        super.close();

        if (mUsbController != null)
            mUsbController.close();
        mUsbController = null;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Scope Functions
    //
    //////////////////////////////////////////////////////////////////////////

    public int doCommand(Command command, int channel, boolean force, Object specialData)
    {
        int val = 0;

        if (mUsbController == null)
            return val;

        return super.doCommand(command, channel, force, specialData);
    }

    private void initSettings()
    {
        if (mUsbController == null)
            return;

        synchronized (mControllerLock)
        {
            mUsbController.write(":WAV:POIN:MODE NOR");
            setRunStop(true);
            forceCommand();
        }
    }

    protected String getName()
    {
        mUsbController.write("*IDN?");
        int[] data = mUsbController.read(300);
        String name = new String(intArrayToByteArray(data));
        String[] parts = name.split(",");

        return parts[0] + " " + parts[1];
    }

    protected boolean isChannelOn(int channel)
    {
        mUsbController.write(":" + getChannelName(channel) + ":DISP?");
        int[] on = mUsbController.read(20);
        return on != null && on.length > 0 && on[0] == 49;
    }

    protected void setVoltageOffset(int channel, float value)
    {
        WaveData data = getWave(channel);
        double offset = (value * data.voltageScale) + data.voltageOffset;
        String command = String.format(Locale.getDefault(), ":%s:OFFS %f", getChannelName(channel), offset);

        mUsbController.write(command);
    }

    protected void setTimeOffset(float value)
    {
        double offset = (value * mTimeData.timeScale) + mTimeData.timeOffset;
        String command = String.format(Locale.getDefault(), ":TIM:OFFS %.10f",offset);

        mUsbController.write(command);
    }

    protected void setVoltageScale(int channel, float value)
    {
        WaveData data = getWave(channel);
        double scale = value * data.voltageScale;
    }

    protected void setTimeScale(RectF value)
    {
      //  mTimeData.timeScale = value;
    }

    protected void setChannelState(int channel, boolean state)
    {
        String onOff = state ? "ON" : "OFF";
        String command = String.format(":%s:DISP %s", getChannelName(channel), onOff);

        mUsbController.write(command);
    }

    protected void setRunStop(boolean run)
    {
        String command = run ? ":RUN" : ":STOP";

        mUsbController.write(command);
    }

    protected void doAuto()
    {
        mUsbController.write(":AUTO");
        try
        {
            Thread.sleep(5000,0);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    // use to re-allow human actions with the scope
    protected void forceCommand()
    {
        mUsbController.write(":KEY:FORC");
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Helper utility functions
    //
    //////////////////////////////////////////////////////////////////////////

    private String getChannelName(int chan)
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
        if(data != null)
        {
            try
            {
                String strValue = new String(intArrayToByteArray(data), "UTF-8");
                value = Double.parseDouble(strValue);
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }

        return value;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Collect Wave Data at timed intervals
    //
    //////////////////////////////////////////////////////////////////////////

    protected void readWave(int channel)
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
                waveData = mWaves3.requestWaveData();
                break;
        }

        synchronized(mControllerLock)
        {
            if(isChannelOn(channel))
            {
                mUsbController.write(":WAV:DATA? " + getChannelName(channel));
                waveData.data = mUsbController.read(SAMPLE_LENGTH);

                //Get the voltage scale
                mUsbController.write(":" + getChannelName(channel) + ":SCAL?");
                waveData.voltageScale = bytesToDouble(mUsbController.read(20));

                // And the voltage offset
                mUsbController.write(":" + getChannelName(channel) + ":OFFS?");
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
                mWaves3.add(waveData);
                break;
        }
    }

    protected void readTimeData()
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
}
