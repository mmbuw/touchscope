package de.uni_weimar.mheinz.androidtouchscope;

public interface BaseScope
{
    static final int SAMPLE_LENGTH = 600;
    static final int QUEUE_LENGTH = SAMPLE_LENGTH * 4;

    public void close();
    public String getName();
    public void start();
    public void stop();

    /**
     *
     * @param command
     * @param channel
     * @param force
     * @param data initialized byte[], data can be returned here. may be null if not used
     * @return data can be returned here if expecting an int
     */
    public int doCommand(Command command, int channel, boolean force, byte[] data);
    //public byte[] doCommand(Command command, int channel, boolean force);

    public enum Command
    {
        READ_WAVE,
        IS_CHANNEL_ON,
        NO_COMMAND
    }
}
