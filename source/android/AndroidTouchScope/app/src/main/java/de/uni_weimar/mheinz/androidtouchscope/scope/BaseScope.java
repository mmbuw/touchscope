package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveRequestPool;

public class BaseScope implements ScopeInterface
{
    private static final String TAG = "BaseScope";

    protected static final int READ_RATE = 100;
    public static final int SAMPLE_LENGTH = 610;
    protected static final int POOL_SIZE = 2;

    protected static final float MAX_VOLTAGE_SCALE = 10f;
    protected static final float MIN_VOLTAGE_SCALE = 2.0E-3f;
    protected static final float MAX_TIME_SCALE = 50f;
    protected static final float MIN_TIME_SCALE = 2.0E-9f;

    protected WaveRequestPool mWaves1 = new WaveRequestPool(POOL_SIZE);
    protected WaveRequestPool mWaves2 = new WaveRequestPool(POOL_SIZE);
    protected WaveRequestPool mWaves3 = new WaveRequestPool(POOL_SIZE);
    protected TimeData mTimeData = new TimeData();

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
            case 3:
                waveData = mWaves3.peek();
                break;
        }
        return waveData;
    }

    public TimeData getTimeData()
    {
        return mTimeData;
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
                    RectF scale = (RectF) specialData;
                    setVoltageScale(channel,scale);
                    break;
                }
                case SET_TIME_SCALE:
                {
                    RectF scale = (RectF) specialData;
                    setTimeScale(scale);
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
                case NO_COMMAND:
                default:
                    break;
            }

            if (force)
                forceCommand();
        }

        return val;
    }

    protected void setTimeScale(RectF scale)
    {
        Log.d(TAG, "setTimeScale");
    }

    protected void setVoltageScale(int channel, RectF scale)
    {
        Log.d(TAG, "setVoltageScale");
    }

    protected void forceCommand()
    {
        Log.d(TAG, "forceCommand");
    }

    protected void doAuto()
    {
        Log.d(TAG, "doAuto");
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

    protected Runnable mReadRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            readWave(1);
            readWave(2);
            readWave(3);
            readTimeData();

            mReadHandler.postDelayed(this, READ_RATE);
        }
    };

    protected void readWave(int channel)
    {
    }

    protected void readTimeData()
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
}
