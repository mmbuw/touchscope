package de.uni_weimar.mheinz.androidtouchscope.scope.wave;

public class WaveData
{
    public int[] data;
    public float voltageScale;
    public float voltageOffset;

    public float timeScale;
    public float timeOffset;

    public WaveData()
    {
        data = null;
        voltageScale = 0.0f;
        voltageOffset = 0.0f;
        timeScale = 0.0f;
        timeOffset = 0.0f;
    }

    public WaveData(int[] data,
                    float voltageScale, float voltageOffset,
                    float timeScale,    float timeOffset)
    {
        this.data = data;
        this.voltageScale = voltageScale;
        this.voltageOffset = voltageOffset;
        this.timeScale = timeScale;
        this.timeOffset = timeOffset;
    }
}
