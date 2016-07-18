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

package de.uni_weimar.mheinz.androidtouchscope.display;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.Gravity;

import de.uni_weimar.mheinz.androidtouchscope.R;


public class LearningView extends AppCompatImageView
{
    public enum Controls
    {
        DIAL_KNOB,
        VERT_POS_KNOB,
        VERT_SCALE_KNOB,
        HORZ_POS_KNOB,
        HORZ_SCALE_KNOB,
        BOTH_POS_KNOBS,
        BOTH_SCALE_KNOBS,
        TRIGGER_KNOB,
        RUN_STOP_BUTTON,
        AUTO_BUTTON,
        CURSOR_BUTTON,
        MEASURE_BUTTON,
        TRIGGER_MENU_BUTTON,
        TRIGGER_50_BUTTON,
        CH1_BUTTON,
        CH2_BUTTON,
        OFF_BUTTON
    }

    private static final String TAG = "LearningView";

    private final LearningView mLearningView;
    private Controls mActiveControl = Controls.DIAL_KNOB;
    private final Object mLock = new Object();
    private int mGravity = Gravity.START;

    public LearningView(Context context)
    {
        super(context);

        mLearningView = this;
    }

    public LearningView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mLearningView = this;
    }

    public LearningView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        mLearningView = this;
    }

    public void doAnim(final Controls control)
    {
        new DoAnimation().execute(control);
        new DoAnimation().execute(control);
    }

    public int getGravity()
    {
        return mGravity;
    }

    public void setGravity(int gravity)
    {
        mGravity = gravity;
    }

    private void stopAnim()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            try
            {
                AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) getDrawable();
                drawable.stop();
                return;
            }
            catch(ClassCastException ex)
            {
                Log.d(TAG, "Can't cast AnimatedVectorDrawableCompat to AnimatedVectorDrawable");
            }
        }
        try
        {
            AnimatedVectorDrawableCompat drawable = (AnimatedVectorDrawableCompat) getDrawable();
            drawable.stop();
        }
        catch(ClassCastException ex)
        {
            Log.e(TAG, "AnimatedVectorDrawable not working!");
        }
    }

    private void startAnim()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            try
            {
                AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) getDrawable();
                if(!drawable.isRunning())
                    drawable.start();
                return;
            }
            catch(ClassCastException ex)
            {
                Log.d(TAG, "Can't cast AnimatedVectorDrawableCompat to AnimatedVectorDrawable");
            }
        }
        try
        {
            AnimatedVectorDrawableCompat drawable = (AnimatedVectorDrawableCompat) getDrawable();
            if(!drawable.isRunning())
                drawable.start();
        }
        catch(ClassCastException ex)
        {
            Log.e(TAG, "AnimatedVectorDrawable not working!");
        }
    }

    private class DoAnimation extends AsyncTask<Controls, Void, Controls>
    {
        @Override
        protected Controls doInBackground(Controls... params)
        {
            return params[0];
        }

        @Override
        protected void onPostExecute(Controls control)
        {
            synchronized(mLock)
            {
                if(getVisibility() == VISIBLE)
                {
               //     AnimatedVectorDrawableCompat drawable = (AnimatedVectorDrawableCompat) getDrawable();
                    if(mActiveControl != control)
                    {
                       // drawable.stop();
                        stopAnim();

                        switch(control)
                        {
                            case DIAL_KNOB:
                                mLearningView.setImageResource(R.drawable.avd_dial);
                                break;
                            case VERT_POS_KNOB:
                                mLearningView.setImageResource(R.drawable.avd_vert_pos);
                                break;
                            case HORZ_POS_KNOB:
                                mLearningView.setImageResource(R.drawable.avd_horz_pos);
                                break;
                            case VERT_SCALE_KNOB:
                                mLearningView.setImageResource(R.drawable.avd_vert_scale);
                                break;
                            case HORZ_SCALE_KNOB:
                                mLearningView.setImageResource(R.drawable.avd_horz_scale);
                                break;
                            case TRIGGER_KNOB:
                                mLearningView.setImageResource(R.drawable.avd_trigger);
                                break;
                            case BOTH_POS_KNOBS:
                                mLearningView.setImageResource(R.drawable.avd_both_pos);
                                break;
                            case BOTH_SCALE_KNOBS:
                                mLearningView.setImageResource(R.drawable.avd_both_scale);
                                break;
                            case RUN_STOP_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_run_stop);
                                break;
                            case AUTO_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_auto);
                                break;
                            case CURSOR_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_cursor);
                                break;
                            case MEASURE_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_measure);
                                break;
                            case TRIGGER_MENU_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_trigger_menu);
                                break;
                            case TRIGGER_50_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_fifty);
                                break;
                            case CH1_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_ch1);
                                break;
                            case CH2_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_ch2);
                                break;
                            case OFF_BUTTON:
                                mLearningView.setImageResource(R.drawable.avd_off);
                                break;
                        }
                    }

                    mActiveControl = control;

                    startAnim();
                 //   if(!drawable.isRunning())
                 //       drawable.start();
                }
            }
        }
    }
}
