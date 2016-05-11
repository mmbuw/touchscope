package de.uni_weimar.mheinz.androidtouchscope.scope.wave;

public class WaveData
{
    public int[] data;
    public double voltageScale;
    public double voltageOffset;
    public String coupling;
    public int probe;

    public WaveData()
    {
        data = null;
        voltageScale = 1.0;
        voltageOffset = 0.0;
        coupling = "DC";
        probe = 1;
    }

    public WaveData(int[] data, double voltageScale, double voltageOffset, String coupling, int probe)
    {
        this.data = data;
        this.voltageScale = voltageScale;
        this.voltageOffset = voltageOffset;
        this.coupling = coupling;
        this.probe = probe;
    }
}
