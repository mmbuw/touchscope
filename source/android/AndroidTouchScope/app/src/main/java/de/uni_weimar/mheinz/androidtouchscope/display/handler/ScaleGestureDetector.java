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

package de.uni_weimar.mheinz.androidtouchscope.display.handler;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import de.uni_weimar.mheinz.androidtouchscope.R;

public class ScaleGestureDetector
{
    public interface OnScaleGestureListener
    {
        boolean onScale(ScaleGestureDetector detector);
        boolean onScaleBegin(ScaleGestureDetector detector);
        void onScaleEnd(ScaleGestureDetector detector);
    }
    public static class SimpleOnScaleGestureListener implements OnScaleGestureListener
    {
        public boolean onScale(ScaleGestureDetector detector)
        {
            return false;
        }
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            return true;
        }
        public void onScaleEnd(ScaleGestureDetector detector)
        {
            // Intentionally empty
        }
    }

    private final OnScaleGestureListener mListener;

    private float mFocusX;
    private float mFocusY;

    private float mCurrSpan;
    private float mPrevSpan;
    private float mInitialSpan;
    private float mCurrSpanX;
    private float mCurrSpanY;
    private float mPrevSpanX;
    private float mPrevSpanY;
    private long mCurrTime;
    private long mPrevTime;
    private boolean mInProgress;
    private int mSpanSlop;
    private int mMinSpan;

    // Bounds for recently seen values
    private float mTouchUpper;
    private float mTouchLower;
    private float mTouchHistoryLastAccepted;
    private int mTouchHistoryDirection;
    private long mTouchHistoryLastAcceptedTime;
    private int mTouchMinMajor;

    private static final long TOUCH_STABILIZE_TIME = 128; // ms

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener)
    {
        mListener = listener;
        mSpanSlop = ViewConfiguration.get(context).getScaledTouchSlop() * 2;

        final Resources res = context.getResources();
        mTouchMinMajor = res.getDimensionPixelSize(R.dimen.config_minScalingTouchMajor);
        mMinSpan = res.getDimensionPixelSize(R.dimen.config_minScalingSpan);
    }

    private void addTouchHistory(MotionEvent ev)
    {
        final long currentTime = SystemClock.uptimeMillis();
        final int count = ev.getPointerCount();
        boolean accept = currentTime - mTouchHistoryLastAcceptedTime >= TOUCH_STABILIZE_TIME;
        float total = 0;
        int sampleCount = 0;
        for (int i = 0; i < count; i++)
        {
            final boolean hasLastAccepted = !Float.isNaN(mTouchHistoryLastAccepted);
            final int historySize = ev.getHistorySize();
            final int pointerSampleCount = historySize + 1;
            for (int h = 0; h < pointerSampleCount; h++)
            {
                float major;
                if (h < historySize)
                {
                    major = ev.getHistoricalTouchMajor(i, h);
                }
                else
                {
                    major = ev.getTouchMajor(i);
                }
                if (major < mTouchMinMajor)
                    major = mTouchMinMajor;
                total += major;

                if (Float.isNaN(mTouchUpper) || major > mTouchUpper)
                {
                    mTouchUpper = major;
                }
                if (Float.isNaN(mTouchLower) || major < mTouchLower)
                {
                    mTouchLower = major;
                }

                if (hasLastAccepted)
                {
                    final int directionSig = (int) Math.signum(major - mTouchHistoryLastAccepted);
                    if (directionSig != mTouchHistoryDirection ||
                            (directionSig == 0 && mTouchHistoryDirection == 0))
                    {
                        mTouchHistoryDirection = directionSig;
                        mTouchHistoryLastAcceptedTime = h < historySize ? ev.getHistoricalEventTime(h)
                                : ev.getEventTime();
                        accept = false;
                    }
                }
            }
            sampleCount += pointerSampleCount;
        }

        if (accept)
        {
            final float avg = total / sampleCount;
            float newAccepted = (mTouchUpper + mTouchLower + avg) / 3;
            mTouchUpper = (mTouchUpper + newAccepted) / 2;
            mTouchLower = (mTouchLower + newAccepted) / 2;
            mTouchHistoryLastAccepted = newAccepted;
            mTouchHistoryDirection = 0;
            mTouchHistoryLastAcceptedTime = ev.getEventTime();
        }
    }

    private void clearTouchHistory()
    {
        mTouchUpper = Float.NaN;
        mTouchLower = Float.NaN;
        mTouchHistoryLastAccepted = Float.NaN;
        mTouchHistoryDirection = 0;
        mTouchHistoryLastAcceptedTime = 0;
    }


    public boolean onTouchEvent(MotionEvent event)
    {
        mCurrTime = event.getEventTime();
        final int action = event.getActionMasked();
        final int count = event.getPointerCount();

        final boolean streamComplete = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;

        if (action == MotionEvent.ACTION_DOWN || streamComplete)
        {
            // Reset any scale in progress with the listener.
            // If it's an ACTION_DOWN we're beginning a new event stream.
            // This means the app probably didn't give us all the events. Shame on it.
            if (mInProgress)
            {
                mListener.onScaleEnd(this);
                mInProgress = false;
                mInitialSpan = 0;
            }

            if (streamComplete)
            {
                clearTouchHistory();
                return true;
            }
        }

        final boolean configChanged = action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_POINTER_DOWN;

        final boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        // Determine focal point
        float sumX = 0, sumY = 0;
        final int div = pointerUp ? count - 1 : count;
        final float focusX;
        final float focusY;

        for (int i = 0; i < count; i++)
        {
            if (skipIndex == i)
                continue;
            sumX += event.getX(i);
            sumY += event.getY(i);
        }

        focusX = sumX / div;
        focusY = sumY / div;

        addTouchHistory(event);

        // Determine average deviation from focal point
        float devSumX = 0, devSumY = 0;
        for (int i = 0; i < count; i++)
        {
            if (skipIndex == i) continue;

            // Convert the resulting diameter into a radius.
            final float touchSize = mTouchHistoryLastAccepted / 2;
            devSumX += Math.abs(event.getX(i) - focusX) + touchSize;
            devSumY += Math.abs(event.getY(i) - focusY) + touchSize;
        }
        final float devX = devSumX / div;
        final float devY = devSumY / div;

        // Span is the average distance between touch points through the focal point;
        // i.e. the diameter of the circle with a radius of the average deviation from
        // the focal point.
        final float spanX = devX * 2;
        final float spanY = devY * 2;
        final float span = (float) Math.hypot(spanX, spanY);

        // Dispatch begin/end events as needed.
        // If the configuration changes, notify the app to reset its current state by beginning
        // a fresh scale event stream.
        final boolean wasInProgress = mInProgress;
        mFocusX = focusX;
        mFocusY = focusY;
        if (mInProgress && (span < mMinSpan || configChanged))
        {
            mListener.onScaleEnd(this);
            mInProgress = false;
            mInitialSpan = span;
        }
        if (configChanged)
        {
            mPrevSpanX = mCurrSpanX = spanX;
            mPrevSpanY = mCurrSpanY = spanY;
            mInitialSpan = mPrevSpan = mCurrSpan = span;
        }

        final int minSpan = mMinSpan;
        if (!mInProgress && span >=  minSpan &&
                (wasInProgress || Math.abs(span - mInitialSpan) > mSpanSlop))
        {
            mPrevSpanX = mCurrSpanX = spanX;
            mPrevSpanY = mCurrSpanY = spanY;
            mPrevSpan = mCurrSpan = span;
            mPrevTime = mCurrTime;
            mInProgress = mListener.onScaleBegin(this);
        }

        // Handle motion; focal point and span/scale factor are changing.
        if (action == MotionEvent.ACTION_MOVE)
        {
            mCurrSpanX = spanX;
            mCurrSpanY = spanY;
            mCurrSpan = span;

            boolean updatePrev = true;

            if (mInProgress)
            {
                updatePrev = mListener.onScale(this);
            }

            if (updatePrev)
            {
                mPrevSpanX = mCurrSpanX;
                mPrevSpanY = mCurrSpanY;
                mPrevSpan = mCurrSpan;
                mPrevTime = mCurrTime;
            }
        }

        return true;
    }

    public boolean isInProgress()
    {
        return mInProgress;
    }

    public float getFocusX()
    {
        return mFocusX;
    }

    public float getFocusY()
    {
        return mFocusY;
    }

    public float getCurrentSpan()
    {
        return mCurrSpan;
    }

    public float getCurrentSpanX()
    {
        return mCurrSpanX;
    }

    public float getCurrentSpanY()
    {
        return mCurrSpanY;
    }

    public float getPreviousSpan()
    {
        return mPrevSpan;
    }

    public float getPreviousSpanX()
    {
        return mPrevSpanX;
    }

    public float getPreviousSpanY()
    {
        return mPrevSpanY;
    }

    public float getScaleFactor()
    {
        return mPrevSpan > 0 ? mCurrSpan / mPrevSpan : 1;
    }

    public long getTimeDelta()
    {
        return mCurrTime - mPrevTime;
    }

    public long getEventTime()
    {
        return mCurrTime;
    }
}