package de.uni_weimar.mheinz.androidtouchscope;

public interface BaseScope
{
    static final int SAMPLE_LENGTH = 610;
    static final int POOL_SIZE = 4;

    public void open();
    public void close();
    public String getName();
    public void start();
    public void stop();
    public boolean isConnected();
    public WaveData getWave(int chan);

    /**
     *
     * @param command
     * @param channel
     * @param force
     * @return data can be returned here if expecting an int
     */
    public int doCommand(Command command, int channel, boolean force);
    //public byte[] doCommand(Command command, int channel, boolean force);

    public enum Command
    {
        IS_CHANNEL_ON,
        NO_COMMAND
    }
}
