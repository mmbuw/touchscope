package de.uni_weimar.mheinz.androidtouchscope;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.os.Handler;

public class TouchScopeActivity extends Activity
{
    private static final String TAG = "TouchScopeActivity";
    private final int REFRESH_RATE = 200;

    RigolScope mRigolScope = null;
    ScopeView mScopeView = null;

    private int mIsChan1On = 0;
    private int mIsChan2On = 0;
    private int mIsMathOn = 0;

    private Handler mRefreshHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_scope);

        mScopeView = (ScopeView) findViewById(R.id.scopeView);

        mRigolScope = new RigolScope(this);
    }

    @Override
    public void onDestroy()
    {
        if(mRigolScope != null)
            mRigolScope.close();
        super.onDestroy();
    }

    public void onTestRead(View v)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                if(mRigolScope != null)
                {
                    mRefreshHandler.removeCallbacks(mRefreshRunnable);

                    mIsChan1On = mRigolScope.isChannelOn(RigolScope.CHAN_1) ? 1 : 0;
                    mIsChan2On = mRigolScope.isChannelOn(RigolScope.CHAN_2) ? 1 : 0;
                    mIsMathOn = mRigolScope.isChannelOn(RigolScope.CHAN_MATH) ? 1 : 0;
                    mRigolScope.forceCommand();

                    mRefreshHandler.postDelayed(mRefreshRunnable, 0);
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_touch_scope, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Runnable mRefreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(mIsChan1On == 1)
            {
                byte[] data = mRigolScope.readWave(RigolScope.CHAN_1);
                mScopeView.setChannelData(RigolScope.CHAN_1, data);
                mRigolScope.forceCommand();
            }
            else if(mIsChan1On == 0)
            {
                mScopeView.setChannelData(RigolScope.CHAN_1, null);
                mIsChan1On = -1;
            }

            if(mIsChan2On == 1)
            {
                byte[] data = mRigolScope.readWave(RigolScope.CHAN_2);
                mScopeView.setChannelData(RigolScope.CHAN_2, data);
                mRigolScope.forceCommand();
            }
            else if(mIsChan2On == 0)
            {
                mScopeView.setChannelData(RigolScope.CHAN_2, null);
                mIsChan2On = -1;
            }

            if(mIsMathOn == 1)
            {
                byte[] data = mRigolScope.readWave(RigolScope.CHAN_MATH);
                mScopeView.setChannelData(RigolScope.CHAN_MATH, data);
                mRigolScope.forceCommand();
            }
            else if(mIsMathOn == 0)
            {
                mScopeView.setChannelData(RigolScope.CHAN_MATH, null);
                mIsMathOn = -1;
            }

            mRefreshHandler.postDelayed(this, REFRESH_RATE);
        }
    };
}
