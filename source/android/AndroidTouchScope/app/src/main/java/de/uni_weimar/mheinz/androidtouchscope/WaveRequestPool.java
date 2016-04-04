package de.uni_weimar.mheinz.androidtouchscope;


import java.util.ArrayDeque;

public class WaveRequestPool
{
    private final int mCapacity;
    private final ArrayDeque<WaveData> mWaves;

    public WaveRequestPool(int capacity)
    {
        mCapacity = capacity;
        mWaves = new ArrayDeque<WaveData>(capacity);
    }

    public WaveData requestWaveData()
    {
        synchronized(mWaves)
        {
            WaveData waveData;
            if(mWaves.size() < mCapacity)
            {
                waveData = new WaveData();
            }
            else
            {
                waveData = mWaves.getFirst();
            }
            return waveData;
        }
    }

    public void add(WaveData waveData)
    {
        synchronized(mWaves)
        {
            if(mWaves.size() == mCapacity)
                mWaves.removeFirst();
            mWaves.addLast(waveData);
        }
    }

    public WaveData peek()
    {
        return mWaves.peek();
    }

    public WaveData poll()
    {
        synchronized(mWaves)
        {
            return mWaves.getFirst();
        }
    }
}
