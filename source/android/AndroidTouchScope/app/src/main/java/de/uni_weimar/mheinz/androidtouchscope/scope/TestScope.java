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

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class TestScope extends BaseScope
{
    private FakeWaveData mFakeWave1;
    private FakeWaveData mFakeWave2;

    public TestScope()
    {
        initTestScope();
        mIsConnected = true;
    }

    private void initTestScope()
    {
        mTimeData.timeOffset = 0;
        mTimeData.timeScale = 1;
        mFakeWave1 = new FakeWaveData();//59909.986179362626);
        mFakeWave2 = new FakeWaveData();//36135.1315588236);
        mFakeWave1.isOn = true;
    }

    public void open(OnReceivedName onReceivedName)
    {
        super.open(onReceivedName);
        doCommand(Command.GET_NAME, 0, false, null);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Scope Functions
    //
    //////////////////////////////////////////////////////////////////////////

    protected String getName()
    {
        return "Test Scope";
    }

    protected boolean isChannelOn(int channel)
    {
        boolean isOn = false;
        switch (channel)
        {
            case 1:
                isOn = mFakeWave1.isOn;
                break;
            case 2:
                isOn = mFakeWave2.isOn;
                break;
        }

        return isOn;
    }

    protected void setVoltageOffset(int channel, float value)
    {
        WaveData data = getWave(channel);
        double offset = (value/* * data.voltageScale*/) + data.voltageOffset;

        switch (channel)
        {
            case 1:
                mFakeWave1.offset = offset;
                break;
            case 2:
                mFakeWave2.offset = offset;
                break;
        }
    }

    protected void setTimeOffset(float value)
    {
        mTimeData.timeOffset = (value/* * mTimeData.timeScale*/) + mTimeData.timeOffset;
    }

    protected void setVoltageScale(int channel, float value)
    {
        WaveData data = getWave(channel);
        double scale = value / data.voltageScale;

        switch (channel)
        {
            case 1:
                mFakeWave1.scale = scale;
                break;
            case 2:
                mFakeWave2.scale = scale;
                break;
        }
    }

    protected void setTimeScale(float value)
    {
        mTimeData.timeScale = mTimeData.timeScale / value;
    }

    protected void setChannelState(int channel, boolean state)
    {
        switch (channel)
        {
            case 1:
                mFakeWave1.isOn = state;
                break;
            case 2:
                mFakeWave2.isOn = state;
                break;
        }
    }

    protected void doAuto()
    {
        initTestScope();
        try
        {
            Thread.sleep(1000,0);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    protected void setChannelCoupling(int channel, String coupling)
    {
        switch (channel)
        {
            case 1:
                mFakeWave1.coupling = coupling;
                break;
            case 2:
                mFakeWave2.coupling = coupling;
                break;
        }
    }

    protected void setTriggerLevel(float level)
    {
        int channel = 1;
        if(mTrigData.source == TriggerData.TriggerSrc.CHAN1)
            channel = 1;
        else if(mTrigData.source == TriggerData.TriggerSrc.CHAN2)
            channel = 2;

        WaveData data = getWave(channel);
        mTrigData.level = (level * data.voltageScale) + mTrigData.level;
    }

    protected void setTriggerSource(String source)
    {
        mTrigData.source = TriggerData.TriggerSrc.toTriggerSrc(source);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Collect Wave Data at timed intervals
    //
    //////////////////////////////////////////////////////////////////////////

  /*  protected void readWave2(int channel)
    {
        WaveData waveData = null;
        FakeWaveData fakeWaveData = null;

        synchronized (mControllerLock)
        {
            switch(channel)
            {
                case 1:
                    waveData = mWaves1.requestWaveData();
                    fakeWaveData = mFakeWave1;
                    break;
                case 2:
                    waveData = mWaves2.requestWaveData();
                    fakeWaveData = mFakeWave2;
                    break;
            }

            if(waveData == null || fakeWaveData == null)
                return;

            int[] buffer = null;
            if (isChannelOn(channel))
            {
                int size = (int) (SAMPLE_LENGTH * mTimeData.timeScale);
                if(size < 50)
                {
                    size = 50;
                    mTimeData.timeScale = 51f / SAMPLE_LENGTH;
                }
                buffer = new int[size];
                double sampleRate = 10.0;
                double freq = fakeWaveData.freq;
                // double freq = Math.random() * 80000 + 10000;
                for (int cnt = (int) mTimeData.timeOffset, i = 0; i < buffer.length; cnt++, i++)
                {
                    double time = cnt / sampleRate;
                    double sinValue =
                            (Math.sin(2 * Math.PI * freq * time) +
                                    Math.sin(2 * Math.PI * (freq / 1.8) * time) +
                                    Math.sin(2 * Math.PI * (freq / 1.5) * time)) / 3.0;
                    int byteValue = (byte) (125 * sinValue + 125);
                    byteValue = (byteValue & 0xFF);
                    byteValue = (int) (byteValue * fakeWaveData.scale + fakeWaveData.offset);
                    if (byteValue > 255)
                        byteValue = 255;
                    else if (byteValue < 0)
                        byteValue = 0;
                    buffer[i] = byteValue;
                }
            }

            waveData.data = buffer;
            waveData.voltageScale = 1.0 / fakeWaveData.scale;
            waveData.voltageOffset = fakeWaveData.offset;

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
    }*/

    protected void readWave(int channel)
    {
        WaveData waveData = null;
        FakeWaveData fakeWaveData = null;

        synchronized (mControllerLock)
        {
            switch (channel)
            {
                case 1:
                    waveData = mWaves1.requestWaveData();
                    fakeWaveData = mFakeWave1;
                    break;
                case 2:
                    waveData = mWaves2.requestWaveData();
                    fakeWaveData = mFakeWave2;
                    break;
            }

            if (waveData == null || fakeWaveData == null)
                return;

            int[] buffer = null;
            if (isChannelOn(channel))
            {
                buffer = new int[SAMPLE_LENGTH];
                double period = 1 / mTimeData.timeScale * (SAMPLE_LENGTH - 10) / 12.0;
                double tOffset = mTimeData.timeOffset * 50;
                double amplitude = fakeWaveData.scale * 25;
                double vOffset = fakeWaveData.offset * 25;
                for(int i = 0; i < SAMPLE_LENGTH; ++i)
                {
                    double value = Math.sin((i + tOffset) * 2 * Math.PI / period)  // time part
                            * amplitude - vOffset; //voltage part
                    int byteValue = (byte)(value + 125.5);
                    byteValue = (byteValue & 0xFF);
                    if (byteValue > 255)
                        byteValue = 255;
                    else if (byteValue < 0)
                        byteValue = 0;
                    buffer[i] = byteValue;
                }
            }

            waveData.data = buffer;
            waveData.voltageScale = 1.0 / fakeWaveData.scale;
            waveData.voltageOffset = fakeWaveData.offset;
            waveData.coupling = fakeWaveData.coupling;

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
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Class to imitate a scope
    //
    //////////////////////////////////////////////////////////////////////////

    private class FakeWaveData
    {
     //   final double freq;
        double scale;
        double offset;
        boolean isOn;
        String coupling;

        public FakeWaveData()
        {
         //   this.freq = freq;
            scale = 1;
            offset = 0;
            isOn = false;
            coupling = "DC";
        }
    }
}
