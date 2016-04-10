package de.uni_weimar.mheinz.androidtouchscope.scope;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public interface BaseScope
{
    final int SAMPLE_LENGTH = 610;
    final int POOL_SIZE = 4;

    void open(OnReceivedName onReceivedName);
    void close();
    void start();
    void stop();
    boolean isConnected();
    WaveData getWave(int chan);

    /**
     *
     * @param command
     * @param channel
     * @param force
     * @return data can be returned here if expecting an int
     */
    int doCommand(Command command, int channel, boolean force);
    //public byte[] doCommand(Command command, int channel, boolean force);

    enum Command
    {
        IS_CHANNEL_ON,
        GET_NAME,
        NO_COMMAND
    }

    interface OnReceivedName
    {
        void returnName(String name);
    }
}
