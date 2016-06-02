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

import android.os.Handler;
import android.util.Log;

import java.math.BigDecimal;

import de.uni_weimar.mheinz.androidtouchscope.TouchScopeActivity;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.MeasureData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveRequestPool;

public class BaseScope implements ScopeInterface
{
    private static final String TAG = "BaseScope";

    protected static final int READ_RATE = TouchScopeActivity.REFRESH_RATE - 10;
    public static final int SAMPLE_LENGTH = 610;
    protected static final int POOL_SIZE = 1;

    protected final WaveRequestPool mWaves1 = new WaveRequestPool(POOL_SIZE);
    protected final WaveRequestPool mWaves2 = new WaveRequestPool(POOL_SIZE);
    protected final TimeData mTimeData = new TimeData();
    protected final TriggerData mTrigData = new TriggerData();
    protected MeasureData mMeasureData = new MeasureData();

    protected final Object mControllerLock = new Object();
    protected boolean mIsConnected = false;

    private OnReceivedName mOnReceivedName;
    private final Handler mReadHandler = new Handler();

    @Override
    public void open(OnReceivedName onReceivedName)
    {
        Log.d(TAG, "open");
        mOnReceivedName = onReceivedName;
    }

    @Override
    public void close()
    {
        Log.d(TAG, "close");

        stop();
    }

    @Override
    public void start()
    {
        Log.d(TAG, "start");

        stop();
        if(mIsConnected)
            mReadHandler.postDelayed(mReadRunnable, 0);
    }

    @Override
    public void stop()
    {
        Log.d(TAG, "stop");

        mReadHandler.removeCallbacks(mReadRunnable);
    }

    @Override
    public boolean isConnected()
    {
        Log.d(TAG, "isConnected");
        return mIsConnected;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Get Wave Data
    //
    //////////////////////////////////////////////////////////////////////////

    public WaveData getWave(int chan)
    {
        WaveData waveData = null;
        switch (chan)
        {
            case 1:
                waveData = mWaves1.peek();
                break;
            case 2:
                waveData = mWaves2.peek();
                break;
        }
        return waveData;
    }

    public TimeData getTimeData()
    {
        return mTimeData;
    }

    public TriggerData getTriggerData()
    {
        return mTrigData;
    }

    public MeasureData getMeasureData(int channel)
    {
        return mMeasureData;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Scope Functions
    //
    //////////////////////////////////////////////////////////////////////////

    @Override
    public int doCommand(Command command, int channel, boolean force, Object specialData)
    {
        Log.d(TAG, "doCommand");

        int val = 0;

        if (!isConnected())
            return val;

        synchronized (mControllerLock)
        {
            switch (command)
            {
                case IS_CHANNEL_ON:
                {
                    //noinspection ConstantConditions
                    val = isChannelOn(channel) ? 1 : 0;
                    break;
                }
                case GET_NAME:
                {
                    String name = getName();
                    if (mOnReceivedName != null)
                        mOnReceivedName.returnName(name);
                    break;
                }
                case SET_VOLTAGE_OFFSET:
                {
                    Float off = (Float) specialData;
                    setVoltageOffset(channel, off);
                    break;
                }
                case SET_TIME_OFFSET:
                {
                    Float off = (Float) specialData;
                    setTimeOffset(off);
                    break;
                }
                case SET_VOLTAGE_SCALE:
                {
                    float scale = (Float) specialData;
                    setVoltageScale(channel,scale);
                    break;
                }
                case SET_TIME_SCALE:
                {
                    float scale = (Float) specialData;
                    setTimeScale(scale);
                    break;
                }
                case SET_TRIGGER_LEVEL:
                {
                    float level = (Float) specialData;
                    setTriggerLevel(level);
                    break;
                }
                case SET_CHANNEL_STATE:
                {
                    Boolean state = (Boolean)specialData;
                    setChannelState(channel, state);
                    break;
                }
                case SET_RUN_STOP:
                {
                    Boolean run = (Boolean)specialData;
                    setRunStop(run);
                    break;
                }
                case DO_AUTO:
                {
                    doAuto();
                    break;
                }
                case DO_TRIG_50:
                {
                    doTrig50();
                    break;
                }
                case SET_CHANNEL_COUPLING:
                {
                    setChannelCoupling(channel, (String)specialData);
                    break;
                }
                case SET_CHANNEL_PROBE:
                {
                    setChannelProbe(channel, (Integer)specialData);
                    break;
                }
                case SET_TRIGGER_SOURCE:
                {
                    setTriggerSource((String)specialData);
                    break;
                }
                case SET_TRIGGER_SLOPE:
                {
                    setTriggerSlope((String)specialData);
                    break;
                }
                case NO_COMMAND:
                default:
                    break;
            }

            if (force)
                forceCommand();
        }

        return val;
    }

    protected void setTimeScale(float scale)
    {
        Log.d(TAG, "setTimeScale");
    }

    protected void setVoltageScale(int channel, float scale)
    {
        Log.d(TAG, "setVoltageScale");
    }

    protected void forceCommand() { }

    protected void doAuto()
    {
        Log.d(TAG, "doAuto");
    }

    protected void doTrig50()
    {
        Log.d(TAG, "doTrig50");
    }

    protected void setRunStop(boolean run)
    {
        Log.d(TAG, "setRunStop");
    }

    protected void setChannelState(int channel, boolean state)
    {
        Log.d(TAG, "setChannelState");
    }

    protected void setTimeOffset(float off)
    {
        Log.d(TAG, "setTimeOffset");
    }

    protected void setVoltageOffset(int channel, float off)
    {
        Log.d(TAG, "setVoltageOffset");
    }

    protected void setTriggerLevel(float level)
    {
        Log.d(TAG, "setTriggerLevel");
    }

    protected void setChannelCoupling(int channel, String coupling)
    {
        Log.d(TAG, "setChannelCoupling");
    }

    protected void setChannelProbe(int channel, int probe)
    {
        Log.d(TAG, "setChannelProbe");
    }

    protected void setTriggerSource(String source)
    {
        Log.d(TAG, "setTriggerLevel");
    }

    protected void setTriggerSlope(String slope)
    {
        Log.d(TAG, "setTriggerLevel");
    }

    protected String getName()
    {
        Log.d(TAG, "getName");
        return "Base Scope";
    }

    protected boolean isChannelOn(int channel)
    {
        Log.d(TAG, "isChannelOn");
        return false;
    }

    protected final Runnable mReadRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            readWave(1);
            readWave(2);

           // if(++mSlowerReadCounter % 10 == 0)
            {
                readTimeData();
                readTriggerData();
            }

            forceCommand();

            mReadHandler.postDelayed(this, READ_RATE);
        }
    };

    protected void readWave(int channel)
    {
    }

    protected void readTimeData()
    {
    }

    protected void readTriggerData()
    {
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Public Static Methods
    //
    //////////////////////////////////////////////////////////////////////////

    /**
     * based on the point value, returns the actual voltage with calculations from scale and offset
     * @param offset - voltage offset
     * @param scale - voltage scale
     * @param point - point formatted as a unsigned byte in an int
     * @return - the actual voltage
     */
    public static double actualVoltage(double offset, double scale, int point)
    {
        // Walk through the data, and map it to actual voltages
        // This mapping is from Cibo Mahto
        // First invert the data
        double tPoint = point * -1 + 255;

        // Now, we know from experimentation that the scope display range is actually
        // 30-229.  So shift by 130 - the voltage offset in counts, then scale to
        // get the actual voltage.
        tPoint = (tPoint - 130.0 - (offset / scale * 25)) / 25 * scale;
        return tPoint;
    }

    public static double roundValue(double value, double scale, int minScale)
    {
        BigDecimal number = BigDecimal.valueOf(value);
        BigDecimal scaleNumber = BigDecimal.valueOf(scale);
        number = number.setScale(Math.max(scaleNumber.scale(),minScale), BigDecimal.ROUND_HALF_EVEN);
        return number.doubleValue();
    }
}
