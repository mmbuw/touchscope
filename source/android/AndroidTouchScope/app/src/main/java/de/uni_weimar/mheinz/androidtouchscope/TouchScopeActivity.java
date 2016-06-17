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

package de.uni_weimar.mheinz.androidtouchscope;

import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.support.v7.widget.Toolbar;

import de.uni_weimar.mheinz.androidtouchscope.display.HostView;
import de.uni_weimar.mheinz.androidtouchscope.display.LearningView;
import de.uni_weimar.mheinz.androidtouchscope.display.MeasurementsView;
import de.uni_weimar.mheinz.androidtouchscope.display.ScopeView;
import de.uni_weimar.mheinz.androidtouchscope.display.handler.OnDataChangedInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.*;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.MeasureData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class TouchScopeActivity extends AppCompatActivity
{
    private static final String TAG = "TouchScopeActivity";
    public static final int REFRESH_RATE = 60;
    private static final int MEASURE_RATE = 1000;

    private ScopeInterface mActiveScope = null;
    private HostView mHostView = null;
    private ScopeView mScopeView;
    private MeasurementsView mMeasurementView;
    private LearningView mLearningView;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private LinearLayout mRightMenu;
    NavigationView mLeftDrawer;

    private final Handler mRefreshHandler = new Handler();

    final CursorStruct mCursorStruct = new CursorStruct(
            CursorStruct.CursorMode.OFF,
            CursorStruct.CursorType.X,
            CursorStruct.CursorSource.CH1);
    final MeasureStruct mMeasureStruct = new MeasureStruct(
            MeasureStruct.MeasureDisplay.OFF, MeasureStruct.MeasureSource.CH1);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_scope);

        Toolbar toolbar = (Toolbar) findViewById(R.id.scope_toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
   //     assert mDrawerLayout != null;
   //     mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        createDrawerToggle(toolbar);
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // disables touch-to-open
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        assert toolbar != null;
        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        mLeftDrawer = (NavigationView) findViewById(R.id.left_drawer);
        assert mLeftDrawer != null;
        mLeftDrawer.setNavigationItemSelectedListener(mLeftDrawerSelectedListener);

        mRightMenu = (LinearLayout) findViewById(R.id.right_menu);

        mHostView = (HostView) findViewById(R.id.hostView);
        mHostView.setOnDoCommand(new OnDataChangedInterface.OnDataChanged()
        {
            @Override
            public void doCommand(ScopeInterface.Command command, int channel, Object specialData)
            {
                if(mActiveScope != null)
                {
                    mActiveScope.doCommand(command, channel, true, specialData);
                }
            }
        });

        mScopeView = (ScopeView) findViewById(R.id.scopeView);
        mMeasurementView = mHostView.getMeasureView();

        mLearningView = (LearningView)findViewById(R.id.learningView);

        ToggleButton runStopButton = (ToggleButton) findViewById(R.id.buttonRunStop);
        assert runStopButton != null;
        runStopButton.setChecked(true);

        // test if it is emulator
        initScope(!Build.FINGERPRINT.contains("generic"));
    }

    private void initScope(boolean doReal)
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.scope_toolbar);
        assert toolbar != null;
        ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(R.string.app_name);

        if(mActiveScope != null)
        {
            mRefreshHandler.removeCallbacks(mRefreshRunnable);
            mRefreshHandler.removeCallbacks(mMeasureRunnable);
            mActiveScope.close();
        }

        if(doReal)
        {
            Log.i(TAG, "Device detected, try to find RigolScope");
            mActiveScope = new RigolScope(this);
            mLeftDrawer.getMenu().findItem(R.id.navigation_real).setChecked(true);
        }
        else
        {
            Log.i(TAG, "Emulator detected, using TestScope");
            mActiveScope = new TestScope();
            mLeftDrawer.getMenu().findItem(R.id.navigation_test).setChecked(true);
        }
        mActiveScope.open(new ScopeInterface.OnReceivedName()
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
                        assert toolbar != null;
                        ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(scopeName);
                    }
                });
            }
        });
    }

    private void startRunnableAndScope()
    {
        mActiveScope.start();
        new Thread(new Runnable()
        {
            public void run()
            {
                if(mActiveScope != null)
                {
                    mRefreshHandler.removeCallbacks(mRefreshRunnable);
                    mRefreshHandler.postDelayed(mRefreshRunnable, 0);

                    if(mMeasureStruct.measureDisplay == MeasureStruct.MeasureDisplay.ON)
                        mRefreshHandler.postDelayed(mMeasureRunnable, 0);
                }
            }
        }).start();
    }

    @Override
    public void onDestroy()
    {
        mRefreshHandler.removeCallbacks(mRefreshRunnable);
        mRefreshHandler.removeCallbacks(mMeasureRunnable);

        if(mActiveScope != null)
            mActiveScope.close();
        mActiveScope = null;

        super.onDestroy();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mRefreshHandler.removeCallbacks(mRefreshRunnable);
        mRefreshHandler.removeCallbacks(mMeasureRunnable);
        if(mActiveScope != null)
            mActiveScope.stop();//.close();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(mActiveScope == null)
        {
            initScope(!Build.FINGERPRINT.contains("generic"));
        }

        startRunnableAndScope();
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
        if(mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }

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

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed()
    {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else if(mDrawerLayout.isDrawerOpen(GravityCompat.END))
            mDrawerLayout.closeDrawer(GravityCompat.END);
        else
            super.onBackPressed();
    }

    private void setCursorModeState(CursorStruct.CursorMode cursorMode)
    {
        View typeView = findViewById(R.id.cursor_type);
        View sourceView = findViewById(R.id.cursor_source);
        assert typeView != null && sourceView != null;

        if(cursorMode == CursorStruct.CursorMode.OFF)
        {
            typeView.setVisibility(View.INVISIBLE);
            sourceView.setVisibility(View.INVISIBLE);

            TextView textView = (TextView)findViewById(R.id.cursor_mode_subtext);
            assert textView != null;
            textView.setText(R.string.cursor_mode_off);
        }
        else if(cursorMode == CursorStruct.CursorMode.MANUAL)
        {
            typeView.setVisibility(View.VISIBLE);
            sourceView.setVisibility(View.VISIBLE);

            TextView textView = (TextView)findViewById(R.id.cursor_mode_subtext);
            assert textView != null;
            textView.setText(R.string.cursor_mode_manual);
        }
    }

    public void onRunStop(View view)
    {
        final boolean isChecked = ((ToggleButton)view).isChecked();

        if(mActiveScope != null)
            mActiveScope.doCommand(
                    ScopeInterface.Command.SET_RUN_STOP,
                    0,
                    true,
                    isChecked);

        mLearningView.doAnim(LearningView.Controls.RUN_STOP_BUTTON);
    }

    public void onAuto(View view)
    {
        mLearningView.doAnim(LearningView.Controls.AUTO_BUTTON);

        final ToggleButton button = (ToggleButton)view;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if(mActiveScope != null)
                    mActiveScope.doCommand(
                            ScopeInterface.Command.DO_AUTO,
                            0,
                            true,
                            button.isChecked());

                Log.i(TAG, "Auto Completed");
                button.setChecked(false);
            }
        }, 0);

        mCursorStruct.cursorMode = CursorStruct.CursorMode.OFF;
        setCursorModeState(CursorStruct.CursorMode.OFF);
        mScopeView.setCursorsState(mCursorStruct);
    }

    public void onMeasure(View view)
    {
        mRightMenu.findViewById(R.id.measure_options).setVisibility(View.VISIBLE);
        mRightMenu.findViewById(R.id.cursor_options).setVisibility(View.GONE);
        mDrawerLayout.openDrawer(GravityCompat.END);

        mLearningView.doAnim(LearningView.Controls.MEASURE_BUTTON);
    }

    public void onCursor(View view)
    {
        mRightMenu.findViewById(R.id.measure_options).setVisibility(View.GONE);
        mRightMenu.findViewById(R.id.cursor_options).setVisibility(View.VISIBLE);
        mDrawerLayout.openDrawer(GravityCompat.END);

        mLearningView.doAnim(LearningView.Controls.CURSOR_BUTTON);
    }

    public void onCloseMenu(View view)
    {
        mDrawerLayout.closeDrawer(GravityCompat.END);
    }

    public void onCursorMode(View view)
    {
        if (mCursorStruct.cursorMode == CursorStruct.CursorMode.OFF)
        {
            mCursorStruct.cursorMode = CursorStruct.CursorMode.MANUAL;
            setCursorModeState(CursorStruct.CursorMode.MANUAL);
        }
        else
        {
            mCursorStruct.cursorMode = CursorStruct.CursorMode.OFF;
            setCursorModeState(CursorStruct.CursorMode.OFF);
        }
        mScopeView.setCursorsState(mCursorStruct);
    }

    public void onCursorType(View view)
    {
        if(mCursorStruct.cursorType == CursorStruct.CursorType.X)
        {
            mCursorStruct.cursorType = CursorStruct.CursorType.Y;

            TextView textView = (TextView)findViewById(R.id.cursor_type_subtext);
            assert textView != null;
            textView.setText(R.string.cursor_type_y);
        }
        else
        {
            mCursorStruct.cursorType = CursorStruct.CursorType.X;

            TextView textView = (TextView)findViewById(R.id.cursor_type_subtext);
            assert textView != null;
            textView.setText(R.string.cursor_type_x);
        }
        mScopeView.setCursorsState(mCursorStruct);
    }

    public void onCursorSource(View view)
    {
        if(mCursorStruct.cursorSource == CursorStruct.CursorSource.CH1)
        {
            mCursorStruct.cursorSource = CursorStruct.CursorSource.CH2;

            TextView textView = (TextView)findViewById(R.id.cursor_source_subtext);
            assert textView != null;
            textView.setText(R.string.source_ch2);
        }
        else
        {
            mCursorStruct.cursorSource = CursorStruct.CursorSource.CH1;

            TextView textView = (TextView)findViewById(R.id.cursor_source_subtext);
            assert textView != null;
            textView.setText(R.string.source_ch1);
        }
        mScopeView.setCursorsState(mCursorStruct);
    }

    public void onMeasureSource(View view)
    {
        if(mMeasureStruct.measureSource == MeasureStruct.MeasureSource.CH1)
        {
            mMeasureStruct.measureSource = MeasureStruct.MeasureSource.CH2;

            TextView textView = (TextView)findViewById(R.id.measure_source_subtext);
            assert textView != null;
            textView.setText(R.string.source_ch2);
            mMeasurementView.setSource(2);
        }
        else
        {
            mMeasureStruct.measureSource = MeasureStruct.MeasureSource.CH1;

            TextView textView = (TextView)findViewById(R.id.measure_source_subtext);
            assert textView != null;
            textView.setText(R.string.source_ch1);
            mMeasurementView.setSource(1);
        }
    }

    public void onMeasureDisplay(View view)
    {
        mRefreshHandler.removeCallbacks(mMeasureRunnable);

        if(mMeasureStruct.measureDisplay == MeasureStruct.MeasureDisplay.OFF)
        {
            mMeasureStruct.measureDisplay = MeasureStruct.MeasureDisplay.ON;

            TextView textView = (TextView)findViewById(R.id.measure_display_all_subtext);
            assert textView != null;
            textView.setText(R.string.measure_display_on);

            mMeasurementView.setVisibility(View.VISIBLE);

            mRefreshHandler.postDelayed(mMeasureRunnable, 0);
        }
        else
        {
            mMeasureStruct.measureDisplay = MeasureStruct.MeasureDisplay.OFF;

            TextView textView = (TextView)findViewById(R.id.measure_display_all_subtext);
            assert textView != null;
            textView.setText(R.string.measure_display_off);

            mMeasurementView.setVisibility(View.GONE);
        }
    }

    public void onHandClick(View view)
    {
        if(mLeftDrawer.getMenu().findItem(R.id.navigation_learner).isChecked())
        {
            boolean isChecked = ((RadioButton)view).isChecked();
            switch(view.getId())
            {
                case R.id.right_hand:
                    if(isChecked)
                        mLearningView.setGravity(Gravity.START);
                    else
                        mLearningView.setGravity(Gravity.END);
                    break;
                case R.id.left_hand:
                    if(isChecked)
                        mLearningView.setGravity(Gravity.END);
                    else
                        mLearningView.setGravity(Gravity.START);
                    break;
            }
            mHostView.setTop(1); //forces a onSizeChange event
        }
    }

    private void createDrawerToggle(Toolbar toolbar)
    {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close)
        {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onDrawerOpened(View drawerView)
            {
                if(drawerView.getId() == R.id.right_menu)
                {
                    if(findViewById(R.id.cursor_options).getVisibility() == View.VISIBLE)
                    {
                        ((ToggleButton) findViewById(R.id.buttonCursor)).setChecked(true);

                        setCursorModeState(mCursorStruct.cursorMode);
                    }
                    else if(findViewById(R.id.measure_options).getVisibility() == View.VISIBLE)
                    {
                        ((ToggleButton) findViewById(R.id.buttonMeasure)).setChecked(true);
                    }
                }
                super.onDrawerOpened(drawerView);
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onDrawerClosed(View drawerView)
            {
                super.onDrawerClosed(drawerView);

                if(drawerView.getId() == R.id.right_menu)
                {
                    ((ToggleButton) findViewById(R.id.buttonCursor)).setChecked(false);
                    ((ToggleButton) findViewById(R.id.buttonMeasure)).setChecked(false);
                }
                //   mLeftDrawer.getMenu().clear();
                //   mLeftDrawer.inflateMenu(R.menu.drawer_left_menu);
            }
        };
    }

    private final NavigationView.OnNavigationItemSelectedListener mLeftDrawerSelectedListener =
            new NavigationView.OnNavigationItemSelectedListener()
            {
                @Override
                public boolean onNavigationItemSelected(MenuItem item)
                {
                    switch (item.getItemId())
                    {
                        case R.id.navigation_real:
                            mDrawerLayout.closeDrawers();
                            item.setChecked(!item.isChecked());
                            initScope(true);
                            startRunnableAndScope();
                            break;
                        case R.id.navigation_test:
                            mDrawerLayout.closeDrawers();
                            item.setChecked(!item.isChecked());
                            initScope(false);
                            startRunnableAndScope();
                            break;
                        case R.id.navigation_learner:
                            item.setChecked(!item.isChecked());
                            if(item.isChecked())
                            {
                                mLearningView.setVisibility(View.VISIBLE);
                                mLeftDrawer.findViewById(R.id.hand_group).setVisibility(View.VISIBLE);

                                if(((RadioButton)mLeftDrawer.findViewById(R.id.right_hand)).isChecked())
                                    mLearningView.setGravity(Gravity.START);
                                else
                                    mLearningView.setGravity(Gravity.END);
                                item.setTitle(R.string.turn_off);
                            }
                            else
                            {
                                mLearningView.setVisibility(View.GONE);
                                mLeftDrawer.findViewById(R.id.hand_group).setVisibility(View.GONE);
                                item.setTitle(R.string.turn_on);
                            }
                            mHostView.setTop(1); //forces a onSizeChange event
                    }
                    return true;
                }
            };

    private final Runnable mRefreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            TimeData timeData = mActiveScope.getTimeData();
            TriggerData trigData = mActiveScope.getTriggerData();

            WaveData waveData = mActiveScope.getWave(1);
            mHostView.setChannelData(1, waveData,timeData, trigData);

            waveData = mActiveScope.getWave(2);
            mHostView.setChannelData(2, waveData,timeData, trigData);

            mRefreshHandler.postDelayed(this, REFRESH_RATE);
        }
    };

    private final Runnable mMeasureRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            int source = mMeasureStruct.measureSource == MeasureStruct.MeasureSource.CH1 ? 1 : 2;
            MeasureData measureData = mActiveScope.getMeasureData(source);
            mMeasurementView.updateMeasurements(measureData);

            mRefreshHandler.postDelayed(this, MEASURE_RATE);
        }
    };
}
