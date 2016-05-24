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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import de.uni_weimar.mheinz.androidtouchscope.R;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.MeasureData;

public class MeasurementsView extends View
{
    private static final int TEXT_SIZE = 13;
    private static final int TEXT_GAP = 5;
    private static final int BOX_WIDTH = 350;
    private static final int BOX_HEIGHT = 6 * TEXT_SIZE + 7 * TEXT_GAP;

    private Point mTopLeft;
    private float mWidth = 0;
    private float mHeight = 0;

    private MeasureData mMeasureData;

    private Drawable mRectDrawable;
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private GestureDetectorCompat mGestureDetector;
    private boolean mTouched = false;

    public MeasurementsView(Context context)
    {
        super(context);
        init();
    }

    public MeasurementsView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public MeasurementsView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init()
    {
        mGestureDetector = new GestureDetectorCompat(getContext(), new SimpleGestureListener());

        mTopLeft = new Point(0,0);

        mRectDrawable = ContextCompat.getDrawable(getContext(), R.drawable.rounded_rect);
        mMeasureData = new MeasureData();

        mTextPaint.setColor(HostView.CHAN1_COLOR);
        mTextPaint.setTextSize(TEXT_SIZE);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setSource(int source)
    {
        if(source == 1)
        {
            mTextPaint.setColor(HostView.CHAN1_COLOR);
        }
        else if(source == 2)
        {
            mTextPaint.setColor(HostView.CHAN2_COLOR);
        }
        invalidate();
    }

    public void updateMeasurements(MeasureData measureData)
    {
        mMeasureData = measureData;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        mHeight = height;
        mWidth = width;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        mRectDrawable.setBounds(mTopLeft.x, mTopLeft.y, mTopLeft.x + BOX_WIDTH, mTopLeft.y + BOX_HEIGHT);
        mRectDrawable.draw(canvas);

        float xPos = mTopLeft.x + TEXT_GAP;
        float yPos = mTopLeft.y + TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_MAX),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_MIN),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_PP),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_TOP),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_BASE),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_AMP),
                xPos, yPos, mTextPaint);

        xPos = BOX_WIDTH / 3 + mTopLeft.x;
        yPos = mTopLeft.y + TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_AVG),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.V_RMS),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.OVER),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.PRE),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.PERIOD),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.FREQ),
                xPos, yPos, mTextPaint);

        xPos = BOX_WIDTH * 2 / 3 + mTopLeft.x;
        yPos = mTopLeft.y + TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.RISE),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.FALL),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.P_WIDTH),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.N_WIDTH),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.P_DUTY),
                xPos, yPos, mTextPaint);

        yPos += TEXT_GAP + TEXT_SIZE;
        canvas.drawText(mMeasureData.getTypeString(MeasureData.MeasureType.N_DUTY),
                xPos, yPos, mTextPaint);

        super.onDraw(canvas);
    }

    private boolean hitTest(MotionEvent event)
    {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final float x = MotionEventCompat.getX(event, pointerIndex);
        final float y = MotionEventCompat.getY(event, pointerIndex);

        boolean selected = false;
        if(x >= mTopLeft.x  && x <= mTopLeft.x + BOX_WIDTH &&
                y >= mTopLeft.y && y <= mTopLeft.y + BOX_HEIGHT)
        {
            selected = true;
        }

        return selected;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean hit = hitTest(event);
        mGestureDetector.onTouchEvent(event);

        if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP)
        {
            mTouched = false;
        }

        return hit;
    }

    private class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mTouched)
            {
                mTopLeft.y = (int)Math.max(0, Math.min(mTopLeft.y - distanceY, mHeight - BOX_HEIGHT));
                mTopLeft.x = (int)Math.max(0, Math.min(mTopLeft.x - distanceX, mWidth - BOX_WIDTH));
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event)
        {
            mTouched = hitTest(event);
            return true;
        }
    }
}
