package de.uni_weimar.mheinz.androidtouchscope;


import android.app.Activity;
import android.os.Handler;

import java.nio.ByteBuffer;

public class RigolScope implements BaseScope
{
    private static final int RIGOL_VENDOR_ID = 6833;
    private static final int RIGOL_PRODUCT_ID = 1416;

    private static final String CHAN_1 = "CHAN1";
    private static final String CHAN_2 = "CHAN2";
    private static final String CHAN_MATH = "MATH";

    private static final int READ_RATE = 100;

    private UsbController mUsbController = null;

    private LimitedByteDeque mChanList1 = new LimitedByteDeque(QUEUE_LENGTH);
    private LimitedByteDeque mChanList2 = new LimitedByteDeque(QUEUE_LENGTH);
    private LimitedByteDeque mChanListM = new LimitedByteDeque(QUEUE_LENGTH);

    private boolean mIsChan1On = false;
    private boolean mIsChan2On = false;
    private boolean mIsChanMOn = false;

    private Handler mReadHandler = new Handler();

    public RigolScope(Activity activity)
    {
        mUsbController = new UsbController(activity, RIGOL_VENDOR_ID, RIGOL_PRODUCT_ID);
        mUsbController.open(new UsbController.OnDeviceStart()
        {
            @Override
            public void start()
            {
                initSettings();
            }
        });
    }

    public void start()
    {
        stop();
        mReadHandler.postDelayed(mReadRunnable, 0);
    }

    public void stop()
    {
        mReadHandler.removeCallbacks(mReadRunnable);
    }

    public void close()
    {
        stop();

        if(mUsbController != null)
            mUsbController.close();
    }

    private void initSettings()
    {
        if(mUsbController == null)
            return;

        mUsbController.write(":WAV:POIN:MODE NOR");
        forceCommand();
    }

    public String getName()
    {
        if(mUsbController == null)
            return null;

        mUsbController.write("*IDN?");
        byte[] data = mUsbController.read(300);
        forceCommand();
        return new String(data);
    }

    public int doCommand(Command command, int channel, boolean force, byte[] data)
    {
        int val = 0;

        if(mUsbController == null)
            return val;

        switch (command)
        {
            case READ_WAVE:
                ByteBuffer buffer = ByteBuffer.wrap(data);
                //readWave(getChannel(channel));
                switch (channel)
                {
                    case 1:
                        buffer.put(mChanList1.peekTo(SAMPLE_LENGTH));
                        break;
                    case 2:
                        buffer.put(mChanList2.peekTo(SAMPLE_LENGTH));
                        break;
                    case 3:
                        buffer.put(mChanListM.peekTo(SAMPLE_LENGTH));
                        break;
                }

                break;
            case IS_CHANNEL_ON:
                val = isChannelOn(channel) ? 1 : 0;
                break;
            case NO_COMMAND:
            default:
                break;
        }

        if(force)
            forceCommand();

        return val;
    }

    private String getChannel(int chan)
    {
        String channel;
        switch (chan)
        {
            case 2:
                channel = CHAN_2;
                break;
            case 3:
                channel = CHAN_MATH;
                break;
            case 1:
            default:
                channel = CHAN_1;
                break;
        }

        return channel;
    }

    private boolean isChannelOn(int channel)
    {
        mUsbController.write(":" + getChannel(channel) + ":DISP?");
        byte[] on = mUsbController.read(20);
        boolean isOn = on.length > 0 && on[0] == 49;

        switch (channel)
        {
            case 1:
                mIsChan1On = isOn;
                break;
            case 2:
                mIsChan2On = isOn;
                break;
            case 3:
                mIsChanMOn = isOn;
        }

        return isOn;
    }

    private void readWave(String channel)
    {
        mUsbController.write(":WAV:DATA? " + channel);
        byte[] data = mUsbController.read(600);

     //   mUsbController.write(":" + channel + ":SCAL?");
     //   byte[] time = mUsbController.read(20);

        mChanListM.addMany(data);
    }

    // use to re-allow human reaction with the scope
    private void forceCommand()
    {
        mUsbController.write(":KEY:FORC");
    }

    private Runnable mReadRunnable = new Runnable()
    {
        int chan = 1;
        @Override
        public void run()
        {
            if(mIsChan1On)
                readWave(CHAN_1);

            if(mIsChan2On)
                readWave(CHAN_2);

            if(mIsChanMOn)
                readWave(CHAN_MATH);

            mReadHandler.postDelayed(this, READ_RATE);
        }
    };
}
