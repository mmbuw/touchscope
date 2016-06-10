/*
 * MIT License
 *
 * Copyright (c) 2016 Matthew Heinz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.uni_weimar.mheinz.androidtouchscope.scope;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.MeasureData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public interface ScopeInterface
{
    void open(OnReceivedName onReceivedName);
    void close();
    void start();
    void stop();
    boolean isConnected();
    WaveData getWave(int chan);
    TimeData getTimeData();
    TriggerData getTriggerData();
    MeasureData getMeasureData(int channel);
    int doCommand(Command command, int channel, boolean force, Object specialData);

    enum Command
    {
        IS_CHANNEL_ON,
        GET_NAME,
        SET_ACTIVE_CHANNEL,
        SET_VOLTAGE_OFFSET,
        SET_TIME_OFFSET,
        SET_VOLTAGE_SCALE,
        SET_TIME_SCALE,
        SET_TRIGGER_LEVEL,
        SET_CHANNEL_STATE,
        SET_RUN_STOP,
        DO_AUTO,
        DO_TRIG_50,
        SET_CHANNEL_COUPLING,
        SET_CHANNEL_PROBE,
        SET_TRIGGER_SOURCE,
        SET_TRIGGER_SLOPE,
        NO_COMMAND
    }

    interface OnReceivedName
    {
        void returnName(String name);
    }
}
