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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class RigolScope extends BaseScope
{
    private static final int RIGOL_VENDOR_ID = 6833;
    private static final int RIGOL_PRODUCT_ID = 1416;

    private static final String CHAN_1 = "CHAN1";
    private static final String CHAN_2 = "CHAN2";

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
        double rounded = roundValue(offset, data.voltageScale, 2);
        String command = String.format(Locale.getDefault(), ":%s:OFFS %f", getChannelName(channel), rounded);

        mUsbController.write(command);
    }

    protected void setTimeOffset(float value)
    {
        double offset = (value * mTimeData.timeScale) + mTimeData.timeOffset;
        double rounded = roundValue(offset, mTimeData.timeScale, 4);
        String command = String.format(Locale.getDefault(), ":TIM:OFFS %.10f", rounded);

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

        double newScale = getClosestTimeValue(mTimeData.timeScale, scale);

        String command = String.format(Locale.getDefault(), ":TIM:SCAL %.10f", newScale);

        mUsbController.write(command);
    }

    protected void setTriggerLevel(float level)
    {
        int channel = 1;
        if(mTrigData.source == TriggerData.TriggerSrc.CHAN1)
            channel = 1;
        else if(mTrigData.source == TriggerData.TriggerSrc.CHAN2)
            channel = 2;

        WaveData data = getWave(channel);
        double value = (level * data.voltageScale) + mTrigData.level;
        double rounded = roundValue(value, data.voltageScale, 2);
        String command = String.format(Locale.getDefault(), ":TRIG:EDGE:LEV %.10f", rounded);

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

    protected void doTrig50()
    {
        mUsbController.write(":Trig%50");
    }

    protected void setChannelCoupling(int channel, String coupling)
    {
        String command = String.format(Locale.getDefault(), ":%s:COUP %s", getChannelName(channel), coupling);
        mUsbController.write(command);
    }

    protected void setChannelProbe(int channel, int probe)
    {
        String command = String.format(Locale.getDefault(), ":%s:PROB %d", getChannelName(channel), probe);
        mUsbController.write(command);
    }

    protected void setTriggerSource(String source)
    {
        String command = String.format(Locale.getDefault(), ":TRIG:EDGE:SOUR %s", source);
        mUsbController.write(command);
    }

    protected void setTriggerSlope(String slope)
    {
        String command = String.format(Locale.getDefault(), ":TRIG:EDGE:SLOP %s", slope);
        mUsbController.write(command);
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
        if(value < 20E-3)
            scale = 3;
        else if(value < 200E-3)
            scale = 2;
        else if(value < 2)
            scale = 1;
        else
            scale = 0;

        BigDecimal number = BigDecimal.valueOf(value);
        number = number.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
        return number.doubleValue();
    }

    private double getClosestTimeValue(double oldValue, double newValue)
    {
        NumberFormat numberFormat = new DecimalFormat("0.#E0");
        String asText = numberFormat.format(newValue);
        String powerStr = asText.substring(asText.indexOf('E'));

        double divisor = Double.valueOf("1.0" + powerStr);
        double base = newValue / divisor;
        double closeValue;

        if(newValue < oldValue)
        {
            if(base >= 1 && base < 2)
            {
                if(base < 1.75) //three fourths between
                    closeValue = 1;
                else
                    closeValue = 2;
            }
            else if(base >= 2 && base < 5)
            {
                if(base < 4.25)
                    closeValue = 2;
                else
                    closeValue = 5;
            }
            else //(base >= 5)
            {
                if(base < 8.75)
                    closeValue = 5;
                else
                    closeValue = 10;
            }
        }
        else
        {
            if(base >= 1 && base < 2)
            {
                if(base < 1.25) //three fourths between
                    closeValue = 1;
                else
                    closeValue = 2;
            }
            else if(base >= 2 && base < 5)
            {
                if(base < 2.75)
                    closeValue = 2;
                else
                    closeValue = 5;
            }
            else //(base >= 5)
            {
                if(base < 6.25)
                    closeValue = 5;
                else
                    closeValue = 10;
            }
        }

        closeValue *= divisor;

        return closeValue;
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

                // get coupling
                mUsbController.write(":" + getChannelName(channel) + ":COUP?");
                try
                {
                    waveData.coupling = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

                // get probe
                mUsbController.write(":" + getChannelName(channel) + ":PROB?");
                waveData.probe = (int)bytesToDouble(mUsbController.read(20));
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
                mTrigData.source = TriggerData.TriggerSrc.toTriggerSrc(strValue);
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            mUsbController.write(":TRIG:EDGE:SLOP?");
            try
            {
                String strValue = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");
                mTrigData.edge = TriggerData.TriggerEdge.toTriggerEdge(strValue);
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            mUsbController.write(":TRIG:EDGE:LEV?");
            mTrigData.level = bytesToDouble(mUsbController.read(20));
        }
    }

    public MeasureData getMeasureData(int channel)
    {
        synchronized(mControllerLock)
        {
            MeasureData measureData = new MeasureData();
            String name = getChannelName(channel);

            try
            {
                mUsbController.write(":MEAS:VPP? " + name);
                measureData.mVPP = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VMAX? " + name);
                measureData.mVMax = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VMIN? " + name);
                measureData.mVMin = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VAMP? " + name);
                measureData.mVAmp = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VTOP? " + name);
                measureData.mVTop = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VBAS? " + name);
                measureData.mVBase = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VAV? " + name);
                measureData.mVAvg = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:VRMS? " + name);
                measureData.mVRMS = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:OVER? " + name);
                measureData.mOver = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:PRES? " + name);
                measureData.mPre = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:FREQ? " + name);
                measureData.mFreq = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:RIS? " + name);
                measureData.mRise = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:FALL? " + name);
                measureData.mFall = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:PER? " + name);
                measureData.mPeriod = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:PWID? " + name);
                measureData.mPWidth = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:NWID? " + name);
                measureData.mNWidth = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:PDUT? " + name);
                measureData.mPDuty = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");

                mUsbController.write(":MEAS:NDUT? " + name);
                measureData.mNDuty = new String(intArrayToByteArray(mUsbController.read(20)), "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            mMeasureData = measureData;
        }
        return mMeasureData;
    }
}
