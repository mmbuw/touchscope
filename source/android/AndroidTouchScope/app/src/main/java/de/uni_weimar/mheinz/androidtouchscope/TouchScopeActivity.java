package de.uni_weimar.mheinz.androidtouchscope;

import android.graphics.Color;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.support.v7.widget.Toolbar;

import de.uni_weimar.mheinz.androidtouchscope.scope.*;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class TouchScopeActivity extends AppCompatActivity
{
    private static final String TAG = "TouchScopeActivity";
    private static final int REFRESH_RATE = 100;

    private BaseScope mActiveScope = null;
    private ScopeView mScopeView = null;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList; // temp till done

    private Handler mRefreshHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_scope);

        Toolbar toolbar = (Toolbar)findViewById(R.id.scope_toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
          //  actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mDrawerList = (ListView)findViewById(R.id.right_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[]{"one", "two", "three"}));

        mScopeView = (ScopeView) findViewById(R.id.scopeView);
        mScopeView.setOnDoCommand(new ScopeView.OnDoCommand()
        {
            @Override
            public void doCommand(BaseScope.Command command, int channel, Object specialData)
            {
                if(mActiveScope != null)
                {
                    mActiveScope.doCommand(command, channel, true, specialData);
                }
            }
        });

        ToggleButton readButton = (ToggleButton)findViewById(R.id.testRead);
        readButton.setChecked(false);

        // test if it is emulator
        initScope(!Build.BRAND.contains("generic"));
    }

    private void initScope(boolean doReal)
    {
        if(mActiveScope != null)
        {
            mRefreshHandler.removeCallbacks(mRefreshRunnable);
            mActiveScope.close();

            ToggleButton readButton = (ToggleButton)findViewById(R.id.testRead);
            readButton.setChecked(false);
        }

        if(doReal)
        {
            Log.i(TAG, "Device detected, try to find RigolScope");
            mActiveScope = new RigolScope(this);
        }
        else
        {
            Log.i(TAG, "Emulator detected, using TestScope");
            mActiveScope = new TestScope();
        }
        mActiveScope.open(new BaseScope.OnReceivedName()
        {
            @Override
            public void returnName(String name)
            {
                final String scopeName = name;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toolbar toolbar = (Toolbar) findViewById(R.id.scope_toolbar);
                        ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(scopeName);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy()
    {
        mRefreshHandler.removeCallbacks(mRefreshRunnable);

        if(mActiveScope != null)
            mActiveScope.close();

        super.onDestroy();
    }

    public void onTestRead(View v)
    {
        if(!mActiveScope.isConnected())
        {
            ((ToggleButton)v).setChecked(false);
        }
        if(((ToggleButton)v).isChecked())
        {
            mActiveScope.start();
            new Thread(new Runnable()
            {
                public void run()
                {
                    if (mActiveScope != null)
                    {
                        mRefreshHandler.removeCallbacks(mRefreshRunnable);
                        mRefreshHandler.postDelayed(mRefreshRunnable, 0);
                    }
                }
            }).start();
        }
        else
        {
            mActiveScope.stop();
            mRefreshHandler.removeCallbacks(mRefreshRunnable);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_touch_scope, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.action_rightDrawer)
        {
            mDrawerLayout.openDrawer(GravityCompat.END);
        }
        else
        {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        }

        return super.onOptionsItemSelected(item);
    }

    private Runnable mRefreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            TimeData timeData = mActiveScope.getTimeData();

            WaveData waveData = mActiveScope.getWave(1);
            mScopeView.setChannelData(1, waveData,timeData);

            waveData = mActiveScope.getWave(2);
            mScopeView.setChannelData(2, waveData,timeData);

            waveData = mActiveScope.getWave(3);
            mScopeView.setChannelData(3, waveData,timeData);

            mRefreshHandler.postDelayed(this, REFRESH_RATE);
        }
    };
}
