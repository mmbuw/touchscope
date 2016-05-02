package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.app.Activity;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Locale;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class RigolScope extends BaseScope
{
    private static final int RIGOL_VENDOR_ID = 6833;
    private static final int RIGOL_PRODUCT_ID = 1416;

    private static final String CHAN_1 = "CHAN1";
    private static final String CHAN_2 = "CHAN2";
  //  private static final String CHAN_MATH = "MATH";

    /*private static final double[] POSSIBLE_VOLT_VALUES =
            {2E-3, 5E-3, 10E-3, 20E-3, 50E-3, 100E-3, 200E-3, 500E-3, 1, 2, 5, 10};*/

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
                scope.start();
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
            mUsbController.write("TRIG:MODE EDGE");

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
        double scale = data.voltageScale / value;
        scale = getClosestVoltValue(scale);
        String command = String.format(Locale.getDefault(), ":%s:SCAL %f", getChannelName(channel), scale);

        mUsbController.write(command);
    }

    protected void setTimeScale(float value)
    {
        double scale = mTimeData.timeScale / value;
        String command = String.format(Locale.getDefault(), ":TIM:SCAL %.10f",scale);

        mUsbController.write(command);
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
        String channel = "";
        switch (chan)
        {
            case 1:
                channel = CHAN_1;
                break;
            case 2:
                channel = CHAN_2;
                break;
          //  case 3:
          //      channel = CHAN_MATH;
          //      break;

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

    private double getClosestVoltValue(double value)
    {
        int scale;
        if(value < 10E-3)
            scale = 3;
        else if(value < 100E-3)
            scale = 2;
        else if(value < 1)
            scale = 1;
        else
            scale = 0;

        BigDecimal number = BigDecimal.valueOf(value);
        number = number.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
        return number.doubleValue();




        /*double minDist = Math.abs(POSSIBLE_VOLT_VALUES[0] - value);
        int minIndex = 0;
        for(int i = 1; i < POSSIBLE_VOLT_VALUES.length; i++)
        {
            double dist = Math.abs(POSSIBLE_VOLT_VALUES[i] - value);
            if(dist < minDist)
            {
                minDist = dist;
                minIndex = i;
            }
        }

        return POSSIBLE_VOLT_VALUES[minIndex];*/
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Collect Wave Data at timed intervals
    //
    //////////////////////////////////////////////////////////////////////////

    protected void readWave(int channel)
    {
        WaveData waveData = null;
        switch(channel)
        {
            case 1:
                waveData = mWaves1.requestWaveData();
                break;
            case 2:
                waveData = mWaves2.requestWaveData();
                break;
           // case 3:
           // default:
           //     waveData = mWaves3.requestWaveData();
           //     break;
        }

        if(waveData == null)
            return;

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
           // case 3:
           //     mWaves3.add(waveData);
           //     break;
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
        }
    }

    protected void readTriggerData()
    {
        synchronized(mControllerLock)
        {
            // get the trigger source
            mUsbController.write(":TRIG:EDGE:SOUR?");
            try
            {
                String strValue = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");
                mTrigData.mSource = TriggerData.TriggerSrc.toTriggerSrc(strValue);
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            mUsbController.write(":TRIG:EDGE:SLOP?");
            try
            {
                String strValue = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");
                mTrigData.mEdge = TriggerData.TriggerEdge.toTriggerEdge(strValue);
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            mUsbController.write(":TRIG:EDGE:LEV?");
            mTrigData.mLevel = bytesToDouble(mUsbController.read(20));
        }
    }
}
