package de.uni_weimar.mheinz.androidtouchscope;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.os.Handler;
import android.widget.ToggleButton;

public class TouchScopeActivity extends Activity
{
    private static final String TAG = "TouchScopeActivity";
    private static final int REFRESH_RATE = 100;

    BaseScope mRigolScope = null;
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
        ToggleButton readButton = (ToggleButton)findViewById(R.id.testRead);
        readButton.setChecked(false);

        mRigolScope = new RigolScope(this);
        //mRigolScope = new TestScope();
        mRigolScope.open();
    }

    @Override
    public void onDestroy()
    {
        if(mRigolScope != null)
            mRigolScope.close();

        mRefreshHandler.removeCallbacks(mRefreshRunnable);
        super.onDestroy();
    }

    public void onTestRead(View v)
    {
        if(!mRigolScope.isConnected())
        {
            ((ToggleButton)v).setChecked(false);
        }
        if(((ToggleButton)v).isChecked())
        {
            mRigolScope.start();
            new Thread(new Runnable()
            {
                public void run()
                {
                    if (mRigolScope != null)
                    {
                        mRefreshHandler.removeCallbacks(mRefreshRunnable);

                        mIsChan1On = mRigolScope.doCommand(BaseScope.Command.IS_CHANNEL_ON, 1, false);
                        mIsChan2On = mRigolScope.doCommand(BaseScope.Command.IS_CHANNEL_ON, 2, false);
                        mIsMathOn = mRigolScope.doCommand(BaseScope.Command.IS_CHANNEL_ON, 3, true);

                        //    mIsChan1On = mRigolScope.isChannelOn(RigolScope.CHAN_1) ? 1 : 0;
                        //    mIsChan2On = mRigolScope.isChannelOn(RigolScope.CHAN_2) ? 1 : 0;
                        //    mIsMathOn = mRigolScope.isChannelOn(RigolScope.CHAN_MATH) ? 1 : 0;
                        //    mRigolScope.forceCommand();

                        mRefreshHandler.postDelayed(mRefreshRunnable, 0);
                    }
                }
            }).start();
        }
        else
        {
            mRigolScope.stop();
            mRefreshHandler.removeCallbacks(mRefreshRunnable);
        }
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
                WaveData waveData = mRigolScope.getWave(1);
                mScopeView.setChannelData(1, waveData);
            }
            else if(mIsChan1On == 0)
            {
                mScopeView.setChannelData(1, null);
                mIsChan1On = -1;
            }

            if(mIsChan2On == 1)
            {
                WaveData waveData = mRigolScope.getWave(2);
                mScopeView.setChannelData(2, waveData);
            }
            else if(mIsChan2On == 0)
            {
                mScopeView.setChannelData(2, null);
                mIsChan2On = -1;
            }

            if(mIsMathOn == 1)
            {
                WaveData waveData = mRigolScope.getWave(3);
                mScopeView.setChannelData(3, waveData);
            }
            else if(mIsMathOn == 0)
            {
                mScopeView.setChannelData(3, null);
                mIsMathOn = -1;
            }

            mRefreshHandler.postDelayed(this, REFRESH_RATE);
        }
    };
}
