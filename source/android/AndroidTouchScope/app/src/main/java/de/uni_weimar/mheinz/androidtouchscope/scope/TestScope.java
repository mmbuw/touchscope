package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.os.Handler;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class TestScope implements BaseScope
{
    private static final int READ_RATE = 100;

   // private LimitedByteDeque mSampleList1 = new LimitedByteDeque(POOL_SIZE);
    private WaveRequestPool mWaves1 = new WaveRequestPool(POOL_SIZE);

    private Handler mReadHandler = new Handler();
    private int[] mBuffer;
    private OnReceivedName mOnReceivedName;

    public TestScope()
    {
        mBuffer = new int[SAMPLE_LENGTH];
    }

    public void open(OnReceivedName onReceivedName)
    {
        mOnReceivedName = onReceivedName;
        doCommand(Command.GET_NAME,0,false);
    }

    public void close()
    {
        stop();
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
        return true;
    }

    private String getName()
    {
        return "Test Scope";
    }

    public int doCommand(Command command, int channel, boolean force)
    {
        int val = 0;
        switch (command)
        {
            case IS_CHANNEL_ON:
                if(channel == 1)
                    val = 1;
            case GET_NAME:
                String name = getName();
                if(mOnReceivedName != null)
                    mOnReceivedName.returnName(name);
                break;
        }
        return val;
    }

    public WaveData getWave(int chan)
    {
        return mWaves1.peek();
    }

    private void generateTone()
    {
        WaveData waveData = mWaves1.requestWaveData();

        float sampleRate = 10.0F; // Allowable 8000,11025,16000,22050,44100
       // double freq = 250;//arbitrary frequency
        double freq = Math.random() * 80000 + 10000;

        for(int cnt = 0; cnt < mBuffer.length; cnt++)
        {
            double time = cnt/sampleRate;
            double sinValue =
                    (Math.sin(2*Math.PI*freq*time) +
                            Math.sin(2*Math.PI*(freq/1.8)*time) +
                            Math.sin(2*Math.PI*(freq/1.5)*time))/3.0;
            mBuffer[cnt] = (byte)(125*sinValue  + 125);
            mBuffer[cnt] = (mBuffer[cnt] & 0xFF);
        }//end for loop

        waveData.data = mBuffer;
        waveData.timeOffset = 1.0f;
        waveData.voltageScale = 1.0f;
        waveData.timeScale = 1.0f;
        waveData.voltageOffset = 0.0f;

        mWaves1.add(waveData);
    }

    private Runnable mReadRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            generateTone();
            mReadHandler.postDelayed(this, READ_RATE);
        }
    };
}
