package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.graphics.RectF;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class TestScope extends BaseScope
{
    private FakeWaveData mFakeWave1;
    private FakeWaveData mFakeWave2;
    private FakeWaveData mFakeWave3;

    public TestScope()
    {
        initTestScope();
        mIsConnected = true;
    }

    private void initTestScope()
    {
        mFakeWave1 = new FakeWaveData(59909.986179362626);
        mFakeWave2 = new FakeWaveData(36135.1315588236);
        mFakeWave3 = new FakeWaveData(48039.920311455244);
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
            case 3:
                isOn = mFakeWave3.isOn;
                break;
        }

        return isOn;
    }

    protected void setVoltageOffset(int channel, float value)
    {
        WaveData data = getWave(channel);
        double offset = (-value * 25) + data.voltageOffset;

        switch (channel)
        {
            case 1:
                mFakeWave1.offset = offset;
                break;
            case 2:
                mFakeWave2.offset = offset;
                break;
            case 3:
                mFakeWave3.offset = offset;
                break;
        }
    }

    protected void setTimeOffset(float value)
    {
        mTimeData.timeOffset = (value * 50 * mTimeData.timeScale) + mTimeData.timeOffset;
    }

    protected void setVoltageScale(int channel, RectF value)
    {
        float offset = value.top;
        if(Math.abs(offset) < 1)
            offset = -1.7f;

        setVoltageOffset(channel, offset);

        WaveData data = getWave(channel);
        double scale = value.bottom / data.voltageScale;

        switch (channel)
        {
            case 1:
                mFakeWave1.scale = scale;
                break;
            case 2:
                mFakeWave2.scale = scale;
                break;
            case 3:
                mFakeWave3.scale = scale;
                break;
        }
    }

    protected void setTimeScale(RectF value)
    {
        setTimeOffset(value.left);
        mTimeData.timeScale = mTimeData.timeScale / value.right;
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
            case 3:
                mFakeWave3.isOn = state;
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

    //////////////////////////////////////////////////////////////////////////
    //
    // Collect Wave Data at timed intervals
    //
    //////////////////////////////////////////////////////////////////////////

    protected void readWave(int channel)
    {
        WaveData waveData;
        FakeWaveData fakeWaveData;

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
            case 3:
            default:
                waveData = mWaves3.requestWaveData();
                fakeWaveData = mFakeWave3;
                break;
        }

        synchronized (mControllerLock)
        {
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

    //////////////////////////////////////////////////////////////////////////
    //
    // Class to imitate a scope
    //
    //////////////////////////////////////////////////////////////////////////

    private class FakeWaveData
    {
        final double freq;
        double scale;
        double offset;
        boolean isOn;

        public FakeWaveData(double freq)
        {
            this.freq = freq;
            scale = 1;
            offset = 0;
            isOn = false;
        }
    }
}
