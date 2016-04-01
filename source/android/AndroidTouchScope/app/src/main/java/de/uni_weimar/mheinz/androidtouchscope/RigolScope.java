package de.uni_weimar.mheinz.androidtouchscope;


import android.app.Activity;

public class RigolScope
{
    private final int RIGOL_VENDOR_ID = 6833;
    private final int RIGOL_PRODUCT_ID = 1416;

    static public final String CHAN_1 = "CHAN1";
    static public final String CHAN_2 = "CHAN2";
    static public final String CHAN_MATH = "MATH";

    private Scope mScope = null;

    public RigolScope(Activity activity)
    {
        mScope = new Scope(activity, RIGOL_VENDOR_ID, RIGOL_PRODUCT_ID);
        mScope.open(new Scope.OnDeviceStart()
        {
            @Override
            public void start()
            {
                initSettings();
            }
        });
    }

    public void close()
    {
        if(mScope != null)
            mScope.close();
    }

    private void initSettings()
    {
        if(mScope == null)
            return;

        mScope.write(":WAV:POIN:MODE NOR");
        forceCommand();
    }

    public String getName()
    {
        if(mScope == null)
            return null;

        mScope.write("*IDN?");
        byte[] data = mScope.read(300);
        forceCommand();
        return new String(data);
    }

    public boolean isChannelOn(String channel)
    {
        if(mScope == null)
            return false;

        mScope.write(":" + channel + ":DISP?");
        byte[] on = mScope.read(20);
        if(on.length > 0)
            return on[0] == 49 ? true : false;
        else
            return false;
    }

    public byte[] readWave(String channel)
    {
        if(mScope == null)
            return null;

     //   mScope.write(":STOP");
        mScope.write(":WAV:DATA? " + channel);
        byte[] data = mScope.read(600);
     //   mScope.write(":" + channel + ":SCAL?");
     //   byte[] time = mScope.read(20);
     //   mScope.write(":RUN");

        return data;
    }

    // use to re-allow human reaction with the scope
    public void forceCommand()
    {
        if(mScope == null)
            return;

        mScope.write(":KEY:FORC");
    }
}
