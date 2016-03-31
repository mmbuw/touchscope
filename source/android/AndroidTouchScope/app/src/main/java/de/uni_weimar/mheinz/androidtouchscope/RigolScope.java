package de.uni_weimar.mheinz.androidtouchscope;


import android.app.Activity;

public class RigolScope
{
    private final int RIGOL_VENDOR_ID = 6833;
    private final int RIGOL_PRODUCT_ID = 1416;

    private Scope mScope = null;

    public RigolScope(Activity activity)
    {
        mScope = new Scope(activity, RIGOL_VENDOR_ID, RIGOL_PRODUCT_ID);
    }

    public void close()
    {
        if(mScope != null)
            mScope.close();
    }

    public void terminate()
    {
        if(mScope != null)
            mScope.terminate();
    }

    public void readWave()
    {
        if(mScope == null)
            return;

        mScope.write(":STOP");
        mScope.write(":WAV:POIN:MODE NOR");
        mScope.write(":WAV:DATA? CHAN1");
        byte[] data = mScope.read(600);
        mScope.write(":CHAN1:SCAL?");
        byte[] time = mScope.read(20);
        mScope.write(":RUN");
        mScope.write(":KEY:FORC");
    }
}
