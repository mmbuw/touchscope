package de.uni_weimar.mheinz.androidtouchscope.scope.wave;

public class WaveData
{
    public int[] data;
    public double voltageScale;
    public double voltageOffset;

    public WaveData()
    {
        data = null;
        voltageScale = 1.0;
        voltageOffset = 0.0;
    }

    public WaveData(int[] data, double voltageScale, double voltageOffset)
    {
        this.data = data;
        this.voltageScale = voltageScale;
        this.voltageOffset = voltageOffset;
    }
}
