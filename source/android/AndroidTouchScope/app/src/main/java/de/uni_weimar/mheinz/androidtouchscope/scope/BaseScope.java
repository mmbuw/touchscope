package de.uni_weimar.mheinz.androidtouchscope.scope;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public interface BaseScope
{
    int SAMPLE_LENGTH = 610;
    int POOL_SIZE = 2;

    void open(OnReceivedName onReceivedName);
    void close();
    void start();
    void stop();
    boolean isConnected();
    WaveData getWave(int chan);
    TimeData getTimeData();

    int doCommand(Command command, int channel, boolean force, Object specialData);

    enum Command
    {
        IS_CHANNEL_ON,
        GET_NAME,
        SET_ACTIVE_CHANNEL,
        SET_VOLTAGE_OFFSET,
        SET_TIME_OFFSET,
        SET_CHANNEL_STATE,
        SET_RUN_STOP,
        DO_AUTO,
        NO_COMMAND
    }

    interface OnReceivedName
    {
        void returnName(String name);
    }
}
