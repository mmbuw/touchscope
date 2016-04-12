package de.uni_weimar.mheinz.androidtouchscope.scope.wave;

public class TimeData
{
    public double timeScale;
    public double timeOffset;

    public TimeData()
    {
        timeScale = 1.0;
        timeOffset = 0.0;
    }

    public TimeData(double timeScale, double timeOffset)
    {
        this.timeScale = timeScale;
        this.timeOffset = timeOffset;
    }
}
