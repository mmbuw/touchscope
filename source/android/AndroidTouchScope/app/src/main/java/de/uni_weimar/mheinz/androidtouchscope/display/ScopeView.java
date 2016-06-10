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
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import de.uni_weimar.mheinz.androidtouchscope.CursorStruct;
import de.uni_weimar.mheinz.androidtouchscope.display.handler.OnDataChangedInterface;
import de.uni_weimar.mheinz.androidtouchscope.display.handler.ScaleGestureDetector;
import de.uni_weimar.mheinz.androidtouchscope.scope.ScopeInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.BaseScope;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class ScopeView extends ViewGroup
{
    private static final String TAG = "ScopeView";
    private static final float NUM_COLUMNS = 12f;
    private static final float NUM_ROWS = 8f;
    //scope returns 12 columns worth, this is displayed ratio
    private static final float DISPLAY_RATIO = NUM_COLUMNS / 12f;

    private int mContentWidth = 0;
    private int mContentHeight = 0;
    private double mContentCenterY = 0;
    private double mContentCenterX = 0;

    private int mSelectedPath = -1; // -1 if not selected

    private double mTimeScreenOffset = 0;
    private double mChan1ScreenOffset = 0;
    private double mChan2ScreenOffset = 0;
    private double mTriggerScreenOffset = 0;

    private WaveData mPrevChan1 = null;
    private WaveData mPrevChan2 = null;
    private TimeData mPrevTime = null;
    private TriggerData mPrevTrig = null;
    private OnDataChangedInterface.OnDataChanged mOnDataChanged = null;

    /****  Drawing  ****/
    private final ShapeDrawable mDrawableChan1 = new ShapeDrawable();
    private final ShapeDrawable mDrawableChan2 = new ShapeDrawable();
    private final ShapeDrawable mDrawableGridH = new ShapeDrawable();
    private final ShapeDrawable mDrawableGridV = new ShapeDrawable();

    private final Path mPathChan1 = new Path();
    private final Path mPathChan2 = new Path();
    private final Path mPathGridH = new Path();
    private final Path mPathGridV = new Path();

    private final Paint mChan1TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mChan2TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTimeOffsetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTriggerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCursorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final String TIME_SCALE_TEXT = "Time: ";
    private static final String TIME_OFFSET_TEXT = "T-> ";
    private static final String TRIGGER_OFFSET_TEXT = "Trig LVL: ";
    private static final String CHAN1_SCALE_TEXT = "Chan1: ";
    private static final String CHAN2_SCALE_TEXT = "Chan2: ";
    private String mChan1Text = "";
    private String mChan1OffsetText = "";
    private String mChan2Text = "";
    private String mChan2OffsetText = "";
    private String mTimeText = "";
    private String mTimeOffsetText = "";
    private String mTriggerText = "";
    private String mCursorText = "";

    private Point mTextPos;

    /****  Gestures  ****/
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;

    private PointF mFirstTouch = null;
    private boolean mInMovement = false;
    private boolean mInScaling = false;
    private boolean mCursorUpdateNeeded = false;
    private int mChangeDelay = 0;
    private int mHitCursorId = -1;

    /**** Cursors ****/
    private final ArrayMap<Integer, CursorView> mCursorArray = new ArrayMap<>();


    public ScopeView(Context context)
    {
        super(context);
        initGestures(context);
        init();
    }

    public ScopeView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initGestures(context);
        init();
    }

    public ScopeView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initGestures(context);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        mDrawableChan1.getPaint().clearShadowLayer();
        mDrawableChan1.getPaint().setStrokeWidth(1);
        mDrawableChan2.getPaint().clearShadowLayer();
        mDrawableChan2.getPaint().setStrokeWidth(1);

        switch(mSelectedPath)
        {
            case 1:
                mDrawableChan1.getPaint().setShadowLayer(10f,0f,0f,HostView.CHAN1_COLOR);
                mDrawableChan1.getPaint().setStrokeWidth(1.5f);
                break;
            case 2:
                mDrawableChan2.getPaint().setShadowLayer(10f,0f,0f,HostView.CHAN2_COLOR);
                mDrawableChan2.getPaint().setStrokeWidth(1.5f);
                break;
            default:
                break;
        }

        mDrawableChan1.draw(canvas);
        mDrawableChan2.draw(canvas);
        mDrawableGridH.draw(canvas);
        mDrawableGridV.draw(canvas);
        canvas.drawText(mChan1Text, mTextPos.x, mTextPos.y, mChan1TextPaint);
        canvas.drawText(mChan1OffsetText, mTextPos.x, mTextPos.y - 20, mChan1TextPaint);
        canvas.drawText(mChan2Text, mTextPos.x + 150, mTextPos.y, mChan2TextPaint);
        canvas.drawText(mChan2OffsetText, mTextPos.x + 150, mTextPos.y - 20, mChan2TextPaint);
        canvas.drawText(mTimeOffsetText, mContentWidth - 5, mTextPos.y, mTimeOffsetTextPaint);
        canvas.drawText(mTimeText, mContentWidth - 100, mTextPos.y, mTimeTextPaint);

        if(mPrevTrig != null)
        {
            if(mPrevTrig.source == TriggerData.TriggerSrc.CHAN1)
                mTriggerTextPaint.setColor(HostView.CHAN1_COLOR);
            else if(mPrevTrig.source == TriggerData.TriggerSrc.CHAN2)
                mTriggerTextPaint.setColor(HostView.CHAN2_COLOR);
            else
                mTriggerTextPaint.setColor(HostView.TRIGGER_COLOR);
        }
        canvas.drawText(mTriggerText, mContentWidth - 5, 20, mTriggerTextPaint);

        canvas.drawText(mCursorText, mTextPos.x, 20, mCursorTextPaint);

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldWidth, int oldHeight)
    {
        init();
    }

    public void setOnDoCommand(OnDataChangedInterface.OnDataChanged onDataChanged)
    {
        mOnDataChanged = onDataChanged;
    }


    //////////////////////////////////////////////////////////////////////////
    //
    // Init functions
    //
    //////////////////////////////////////////////////////////////////////////

    private void initGestures(Context context)
    {
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScopeScaleListener());
        mGestureDetector = new GestureDetectorCompat(context, new ScopeGestureListener());
    }

    private void init()
    {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mContentWidth = getWidth() - paddingLeft - paddingRight;
        mContentHeight = getHeight() - paddingTop - paddingBottom;
        mContentCenterY = mContentHeight / 2.0;
        mContentCenterX = mContentWidth / 2.0;
        mTextPos = new Point(5,mContentHeight - 5);

        initDrawable(mDrawableChan1, mPathChan1, HostView.CHAN1_COLOR, mContentWidth, mContentHeight);
        initDrawable(mDrawableChan2, mPathChan2, HostView.CHAN2_COLOR, mContentWidth, mContentHeight);
        initGridH(mDrawableGridH, mPathGridH, Color.GRAY, mContentWidth, mContentHeight);
        initGridV(mDrawableGridV, mPathGridV, Color.GRAY, mContentWidth, mContentHeight);
        initText(mChan1TextPaint, 15, HostView.CHAN1_COLOR, Paint.Align.LEFT);
        initText(mChan2TextPaint, 15, HostView.CHAN2_COLOR, Paint.Align.LEFT);
        initText(mTimeTextPaint, 15, Color.WHITE, Paint.Align.RIGHT);
        initText(mTimeOffsetTextPaint, 15, HostView.TRIGGER_COLOR, Paint.Align.RIGHT);
        initText(mTriggerTextPaint, 15, HostView.TRIGGER_COLOR, Paint.Align.RIGHT);
        initText(mCursorTextPaint, 15, Color.GREEN, Paint.Align.LEFT);

        for(CursorView cursorView : mCursorArray.values())
        {
            cursorView.layout(0, 0, mContentWidth, mContentHeight);
            cursorView.update();
        }
    }

    private void initDrawable(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.moveTo(0, 0);
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.setBounds(0, 0, width, height);

        setLayerType(LAYER_TYPE_SOFTWARE, drawable.getPaint());
    }

    private void initGridV(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.rewind();
        float cellWidth = width / NUM_COLUMNS;
        for(int i = 0; i < NUM_COLUMNS; ++i)
        {
            path.moveTo(i * cellWidth,0);
            path.lineTo(i * cellWidth,height);
        }

        float offset = NUM_ROWS * 5;
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.getPaint().setPathEffect(new DashPathEffect(new float[]{1, (height - offset) / offset}, 0));
        drawable.setBounds(0, 0, width, height);
    }

    private void initGridH(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.rewind();
        float cellHeight = height / NUM_ROWS;
        for(int i = 0; i < NUM_ROWS; ++i)
        {
            path.moveTo(0,i * cellHeight);
            path.lineTo(width,i * cellHeight);
        }

        float offset = NUM_COLUMNS * 5;
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.getPaint().setPathEffect(new DashPathEffect(new float[]{1, (width - offset) / offset}, 0));
        drawable.setBounds(0, 0, width, height);
    }

    private void initText(Paint paint, int height, int color, Paint.Align align)
    {
        paint.setColor(color);
        paint.setTextSize(height);
        paint.setTextAlign(align);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Update View functions
    //
    //////////////////////////////////////////////////////////////////////////

    public void setInMovement(boolean moving)
    {
        mInMovement = moving;
        if(moving)
        {
            int numOn = channelOnCount();
            mChangeDelay = 4 * numOn;
            mCursorUpdateNeeded = true;
        }
    }

    public void setChannelData(int channel, WaveData waveData, TimeData timeData, TriggerData trigData)
    {
        // being updated by user
        if(mInMovement || mInScaling)
            return;

        switch(channel)
        {
            case 1:
            {
                mPrevChan1 = waveData;
                int retValue = updatePath(mPathChan1, waveData);
                if(retValue > 0)
                    mChan1Text = updateVoltText(CHAN1_SCALE_TEXT, waveData.voltageScale);
                else if(retValue < 0)
                    mChan1Text = "";
                break;
            }
            case 2:
            {
                mPrevChan2 = waveData;
                int retValue = updatePath(mPathChan2, waveData);
                if(retValue > 0)
                    mChan2Text = updateVoltText(CHAN2_SCALE_TEXT, waveData.voltageScale);
                else if(retValue < 0)
                    mChan2Text = "";
                break;
            }
        }

        if(mOnDataChanged != null && mChangeDelay <= 0)
        {
            mPrevTime = timeData;
            if(timeData != null)
            {
                mTimeScreenOffset = toScreenPosH(timeData.timeScale, timeData.timeOffset);
                mOnDataChanged.moveTime((float) mTimeScreenOffset, false);

                mTimeText = updateTimeText(TIME_SCALE_TEXT, timeData.timeScale);
                mTimeOffsetText = updateTimeText(TIME_OFFSET_TEXT, timeData.timeOffset);
            }

            mPrevTrig = trigData;
            if (trigData != null && waveData != null &&
                    ((trigData.source == TriggerData.TriggerSrc.CHAN1 && channel == 1) ||
                            trigData.source == TriggerData.TriggerSrc.CHAN2 && channel == 2))
            {
                mTriggerScreenOffset = (float) toScreenPosV(waveData.voltageScale,
                        waveData.voltageOffset + trigData.level);
                mOnDataChanged.moveTrigger((float) mTriggerScreenOffset, false);

                mTriggerText = updateTriggerText(trigData);
            }

            if(waveData != null && waveData.data != null)
            {
                double cursorPos = toScreenPosV(waveData.voltageScale, waveData.voltageOffset);
                if(channel == 1)
                    mChan1ScreenOffset = cursorPos;
                else
                    mChan2ScreenOffset = cursorPos;
                mOnDataChanged.moveWave(channel, (int)cursorPos, false);
            }

            if(mCursorUpdateNeeded)
            {
                mCursorUpdateNeeded = false;

                for(CursorView cursorView : mCursorArray.values())
                {
                    cursorView.update();
                }
            }
        }

        // Auto select channel (if only one on, it is selected)
        if(channelOnCount() > 1)
        {
            if(mSelectedPath == -1)
                mSelectedPath = 1;
        }
        else if(new PathMeasure(mPathChan1,false).getLength() > 0)
            mSelectedPath = 1;
        else if(new PathMeasure(mPathChan2,false).getLength() > 0)
            mSelectedPath = 2;

        postInvalidate();
    }

    private int updatePath(Path path, WaveData waveData)
    {
        int retValue = -1;
        if(waveData == null || waveData.data == null || waveData.data.length == 0)
        {
            path.rewind();
            return retValue;
        }

        retValue = 0;
        int length = Math.min(BaseScope.SAMPLE_LENGTH, waveData.data.length) - 10;
        float widthRatio = (float)(mContentWidth) / (length * DISPLAY_RATIO);
        double vScale = waveData.voltageScale;
        if(vScale == 0)
            vScale = 1.0f;

        Path newPath = new Path();
        double point = manipulatePoint(waveData.voltageOffset, vScale, waveData.data[10]);

        float j = -(length * (1 - DISPLAY_RATIO)) / 2;
        newPath.moveTo(j++  * widthRatio, (float)point);

        for(int i = 11; i < waveData.data.length; ++i, ++j)
        {
            point = manipulatePoint(waveData.voltageOffset, vScale, waveData.data[i]);
            newPath.lineTo(j * widthRatio, (float)point);
        }

        if(new PathMeasure(path,false).getLength() == 0)
        {
            path.set(newPath);
            return retValue;
        }

        if(mChangeDelay <= 0)
        {
            path.set(newPath);
            retValue = 1;
        }
        else
        {
            mChangeDelay--;
        }

        return retValue;
    }

    private String updateVoltText(String startText, double volt)
    {
        double value;
        String end;
        double absVolt = Math.abs(volt);

        if (absVolt < 1)
        {
            value = volt * 1e3;
            end = "mV";
        }
        else
        {
            value = volt;
            end = "V";
        }

        return String.format(Locale.getDefault(),"%s%.2f%s", startText, value, end);
    }

    private String updateTimeText(String startText, double time)
    {
        double value;
        String end;
        double absTime = Math.abs(time);
        if(absTime < 1e-6)
        {
            value = (time * 1e9);
            end = "nS";
        }
        else if(absTime < 1e-3)
        {
            value = time * 1e6;
            end = "uS";
        }
        else if(absTime < 1)
        {
            value = time * 1e3;
            end = "mS";
        }
        else
        {
            value = time;
            end = "S";
        }
        return String.format(Locale.getDefault(),"%s%.2f%s",startText, value, end);
    }

    private String updateTriggerText(TriggerData trigData)
    {
        if(trigData == null)
            return "";

        double value;
        String end;

        if (Math.abs(trigData.level) < 1)
        {
            value = trigData.level * 1e3;
            end = "mV";
        }
        else
        {
            value = trigData.level;
            end = "V";
        }

        return String.format(Locale.getDefault(),"%s%.2f%s", TRIGGER_OFFSET_TEXT, value, end);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Helper functions
    //
    //////////////////////////////////////////////////////////////////////////

    private double toScreenPosV(double voltScale, double data)
    {
        float heightRatio = (mContentHeight / NUM_ROWS) / (float)voltScale;
        return mContentCenterY - (data * heightRatio);

     /*   if(screenData < 0)
            screenData = 0;
        else if(screenData > mContentHeight)
            screenData = mContentHeight;*/

    //    return screenData;
    }

    private double toScreenPosH(double timeScale, double data)
    {
        float widthRatio = (mContentWidth / NUM_COLUMNS) / (float)timeScale;
        return mContentCenterX - (data * widthRatio);

     /*   if(screenData < 0)
            screenData = 0;
        else if(screenData > mContentWidth)
            screenData = mContentWidth;*/

    //    return screenData;
    }

    private double fromScreenPosV(double voltScale, double screenData)
    {
        float heightRatio = (mContentHeight / NUM_ROWS) / (float)voltScale;
        return (mContentCenterY - screenData) / heightRatio;
    }

    private double fromScreenPosH(double timeScale, double screenData)
    {
        float widthRatio = (mContentWidth / NUM_COLUMNS) / (float)timeScale;
        return (mContentCenterX - screenData) / widthRatio;
    }

    private double manipulatePoint(double voltOffset, double voltScale, int data)
    {
        double point = BaseScope.actualVoltage(voltOffset, voltScale, data);
        return toScreenPosV(voltScale, point + (float)voltOffset);
    }

    private int channelOnCount()
    {
        int count = 0;

        PathMeasure measure = new PathMeasure(mPathChan1,false);
        count += measure.getLength() > 0 ? 1 : 0;
        measure.setPath(mPathChan2,false);
        count += measure.getLength() > 0 ? 1 : 0;

        return count;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Path Selection
    //
    //////////////////////////////////////////////////////////////////////////

    public void setSelectedPath(int path)
    {
        mSelectedPath = path;
    }

    private boolean touchSelectPath(MotionEvent event)
    {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final float x = MotionEventCompat.getX(event, pointerIndex);
        final float y = MotionEventCompat.getY(event, pointerIndex);

        int hit = pathHitTest(x, y, 15f);
        if(hit == -1 && mSelectedPath == -1)
            return false;

        if(mSelectedPath == hit)
            mSelectedPath = -1;
        else
            mSelectedPath = hit;

     /*   mDrawableChan1.getPaint().clearShadowLayer();
        mDrawableChan1.getPaint().setStrokeWidth(1);
        mDrawableChan2.getPaint().clearShadowLayer();
        mDrawableChan2.getPaint().setStrokeWidth(1);

        switch(mSelectedPath)
        {
            case 1:
                mDrawableChan1.getPaint().setShadowLayer(10f,0f,0f,HostView.CHAN1_COLOR);
                mDrawableChan1.getPaint().setStrokeWidth(1.5f);
                break;
            case 2:
                mDrawableChan2.getPaint().setShadowLayer(10f,0f,0f,HostView.CHAN2_COLOR);
                mDrawableChan2.getPaint().setStrokeWidth(1.5f);
                break;
            default:
                break;
        }*/
        return true;
    }

    private float smallestDistanceToPath(Path path, float x, float y)
    {
        PathMeasure measure = new PathMeasure(path,false);
        float pos[] = {0f, 0f};
        float minDist = 1000f;
        float dist;

        for(int i = 0; i < measure.getLength(); ++i)
        {
            measure.getPosTan(i, pos, null);
            dist = (float)Math.hypot(x - pos[0], y - pos[1]);
            if(dist < minDist)
                minDist = dist;
        }
        return minDist;
    }

    private int pathHitTest(float x, float y, float threshold)
    {
        int selected = -1;
        float minDist = threshold;

        float dist = smallestDistanceToPath(mPathChan1,x,y);
        if(dist <= minDist)
        {
            selected = 1;
            minDist = dist;
        }

        dist = smallestDistanceToPath(mPathChan2,x,y);
        if(dist < minDist)
        {
            selected = 2;
        }

        return selected;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Methods related to moving waves
    //
    //////////////////////////////////////////////////////////////////////////

    public void moveWave(int channel, float dist, boolean endMove)
    {
        if(endMove)
        {
            float move = dist / (mContentHeight / NUM_ROWS);
            mOnDataChanged.doCommand(
                    ScopeInterface.Command.SET_VOLTAGE_OFFSET,
                    channel,
                    move);
            mChan1OffsetText = "";
            mChan2OffsetText = "";
        }
        else
        {
            if(channel == 1)
            {
                mPathChan1.offset(0, dist);

                if(mPrevChan1 != null)
                {
                    mChan1ScreenOffset = mChan1ScreenOffset + dist;
                    double move = fromScreenPosV(mPrevChan1.voltageScale, mChan1ScreenOffset);
                    double rounded = BaseScope.roundValue(move, mPrevChan1.voltageScale, 2);
                    mChan1OffsetText = updateVoltText("Offset: ", rounded);
                }
            }
            else if(channel == 2)
            {
                mPathChan2.offset(0, dist);

                if(mPrevChan2 != null)
                {
                    mChan2ScreenOffset = mChan2ScreenOffset + dist;
                    double move = fromScreenPosV(mPrevChan2.voltageScale, mChan2ScreenOffset);
                    double rounded = BaseScope.roundValue(move, mPrevChan2.voltageScale, 2);
                    mChan2OffsetText = updateVoltText("Offset: ", rounded);
                }
            }
        }
        invalidate();
    }

    public void moveTime(float dist, boolean endMove)
    {
        if(endMove)
        {
            float move = dist / (mContentWidth / NUM_COLUMNS);
            mOnDataChanged.doCommand(
                    ScopeInterface.Command.SET_TIME_OFFSET,
                    0,
                    move);
        }
        else
        {
            mPathChan1.offset(dist, 0);
            mPathChan2.offset(dist, 0);
            if(mPrevTime != null)
            {
                mTimeScreenOffset += dist;
                double move = fromScreenPosH(mPrevTime.timeScale, mTimeScreenOffset);
                double rounded = BaseScope.roundValue(move, mPrevTime.timeScale, 4);
                mTimeOffsetText = updateTimeText(TIME_OFFSET_TEXT, rounded);
            }
        }
        invalidate();
    }

    public void moveTrigger(float dist, boolean endMove)
    {
        if(endMove)
        {
            mOnDataChanged.doCommand(
                    ScopeInterface.Command.SET_TRIGGER_LEVEL,
                    0,
                    (dist / (mContentHeight / NUM_ROWS)));
        }
        else
        {
            if(mPrevTrig != null)
            {
                mTriggerScreenOffset += dist;
                double move, rounded = 0;
                if(mPrevChan1 != null && mPrevTrig.source == TriggerData.TriggerSrc.CHAN1)
                {
                    move = fromScreenPosV(mPrevChan1.voltageScale, mTriggerScreenOffset) - mPrevChan1.voltageOffset;
                    rounded = BaseScope.roundValue(move, mPrevChan1.voltageScale, 2);
                }
                else if(mPrevChan2 != null)
                {
                    move = fromScreenPosV(mPrevChan2.voltageScale, mTriggerScreenOffset) - mPrevChan2.voltageOffset;
                    rounded = BaseScope.roundValue(move, mPrevChan2.voltageScale, 2);
                }

                mTriggerText = updateVoltText(TRIGGER_OFFSET_TEXT, rounded);

                mOnDataChanged.doAnimation(LearningView.Controls.TRIGGER_KNOB);
            }
        }
        invalidate();
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Methods related to gesture handling
    //
    //////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP ||
                mScaleGestureDetector.isInProgress())
        {
            if(mInMovement && mFirstTouch != null)
            {
                int pointerIndex = MotionEventCompat.getActionIndex(event);
                float x = MotionEventCompat.getX(event, pointerIndex);
                float y = MotionEventCompat.getY(event, pointerIndex);

                if(mOnDataChanged != null)
                {
                    if(mSelectedPath > 0)
                    {
                        float distY = mFirstTouch.y - y;
                        moveWave(mSelectedPath, distY, true);
                    }
                    float distX = mFirstTouch.x - x;
                    moveTime(distX, true);
                }
                mFirstTouch = null;
            }

            setInMovement(false);
            mHitCursorId = -1;
        }

        return true;
    }

    private class ScopeGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mFirstTouch != null)
            {
                if(mHitCursorId > 0)
                {
                    int index = MotionEventCompat.getActionIndex(e2);
                    float x = MotionEventCompat.getX(e2, index);
                    float y = MotionEventCompat.getY(e2, index);
                    mCursorArray.get(mHitCursorId).changeLocation(x,y);

                    mOnDataChanged.doAnimation(LearningView.Controls.DIAL_KNOB);
                }
                else
                {
                    setInMovement(true);

                    if(mSelectedPath > 0)
                        mOnDataChanged.doAnimation(LearningView.Controls.BOTH_POS_KNOBS);
                    else
                        mOnDataChanged.doAnimation(LearningView.Controls.HORZ_POS_KNOB);

                    // must have a selected channel for voltage offset
                    switch (mSelectedPath)
                    {
                        case 1:
                            moveWave(1, -distanceY, false);
                            if (mOnDataChanged != null)
                            {
                                mOnDataChanged.moveWave(1, (float) mChan1ScreenOffset, true);
                            }
                            break;
                        case 2:
                            moveWave(2, -distanceY, false);
                            if (mOnDataChanged != null)
                            {
                                mOnDataChanged.moveWave(2, (float) mChan2ScreenOffset, true);
                            }
                            break;
                    }

                    moveTime(-distanceX, false);

                    if (mOnDataChanged != null)
                    {
                        mOnDataChanged.moveTime((float) mTimeScreenOffset, true);
                    }

                    invalidate();
                }
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event)
        {
            int index = MotionEventCompat.getActionIndex(event);
            float x = MotionEventCompat.getX(event, index);
            float y = MotionEventCompat.getY(event, index);
            mFirstTouch = new PointF(x,y);

            for(CursorView cursorView : mCursorArray.values())
            {
                if (cursorView.hitTest(x, y))
                {
                    mHitCursorId = cursorView.getIndex();
                }
            }

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            touchSelectPath(event);
            return true;
        }
    }

    private class ScopeScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        private float mFirstSpanX;
        private float mFirstSpanY;

        private double mOrgScaleY = 0;
        private double mOrgScaleX = 0;

        private final int VERTICAL = 0;
        private final int HORIZONTAL = 1;
        private final int BOTH = 2;
        private int mScaleDir = BOTH;

        private int getMajorScaleDirection(float xDir, float yDir)
        {
            double ratio = Math.min(xDir,yDir) / Math.max(xDir,yDir);
            Log.d(TAG, "ratio " + ratio);

            if(ratio > 0.66)
                return BOTH;
            else if(xDir > yDir)
                return HORIZONTAL;
            else
                return VERTICAL;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            Log.d(TAG, "onScaleBegin");

            mInScaling = true;
            mFirstSpanX = detector.getCurrentSpanX();
            mFirstSpanY = detector.getCurrentSpanY();
            mScaleDir = getMajorScaleDirection(mFirstSpanX, mFirstSpanY);

            if(mSelectedPath == 1 && mPrevChan1 != null)
            {
                mOrgScaleY = mPrevChan1.voltageScale;
            }
            else if(mSelectedPath == 2 && mPrevChan2 != null)
            {
                mOrgScaleY = mPrevChan2.voltageScale;
            }
            if(mPrevTime != null)
                mOrgScaleX = mPrevTime.timeScale;

            // stop dragging while zooming
            mFirstTouch = null;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            float spanX = detector.getCurrentSpanX();
            float spanY = detector.getCurrentSpanY();
            float previousSpanX = detector.getPreviousSpanX();
            float previousSpanY = detector.getPreviousSpanY();

            float scaleX = spanX / previousSpanX;
            float scaleY = spanY / previousSpanY;
            //float scaleY = (float)Math.pow(spanY / previousSpanY, 2);

            Matrix scaleMatrix = new Matrix();

            if(mScaleDir == VERTICAL || mScaleDir == BOTH)
            {
                RectF rectF = new RectF();
                switch(mSelectedPath)
                {
                    case 1:
                        mPathChan1.computeBounds(rectF, true);
                        if(mPrevChan1.coupling.compareToIgnoreCase("AC") == 0)
                            scaleMatrix.setScale(1, scaleY, rectF.centerX(), rectF.centerY());
                        else
                            scaleMatrix.setScale(1, scaleY, rectF.centerX(), rectF.bottom);
                        mPathChan1.transform(scaleMatrix);

                        mOrgScaleY = mOrgScaleY / scaleY;
                        mChan1Text = updateVoltText(CHAN1_SCALE_TEXT, mOrgScaleY);

                        break;
                    case 2:
                        mPathChan2.computeBounds(rectF, true);
                        if(mPrevChan2.coupling.compareToIgnoreCase("AC") == 0)
                            scaleMatrix.setScale(1, scaleY, rectF.centerX(), rectF.centerY());
                        else
                            scaleMatrix.setScale(1, scaleY, rectF.centerX(), rectF.bottom);
                        mPathChan2.transform(scaleMatrix);

                        mOrgScaleY = mOrgScaleY / scaleY;
                        mChan2Text = updateVoltText(CHAN2_SCALE_TEXT, mOrgScaleY);

                        break;
                }
            }

            if(mScaleDir == HORIZONTAL || mScaleDir == BOTH)
            {
                scaleMatrix = new Matrix();
                Rect drawRect = new Rect();
                getDrawingRect(drawRect);
                scaleMatrix.setScale(scaleX, 1, drawRect.centerX(), drawRect.centerY());

                mPathChan1.transform(scaleMatrix);
                mPathChan2.transform(scaleMatrix);

                mOrgScaleX = mOrgScaleX / scaleX;
                mTimeText = updateTimeText(TIME_SCALE_TEXT, mOrgScaleX);
            }

            if(mScaleDir == BOTH)
                mOnDataChanged.doAnimation(LearningView.Controls.BOTH_SCALE_KNOBS);
            else if(mScaleDir == HORIZONTAL)
                mOnDataChanged.doAnimation(LearningView.Controls.HORZ_SCALE_KNOB);
            else if(mScaleDir == VERTICAL)
                mOnDataChanged.doAnimation(LearningView.Controls.VERT_SCALE_KNOB);

            invalidate();

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector)
        {
            if(mInScaling)
            {
                float spanX = detector.getCurrentSpanX();
                float spanY = detector.getCurrentSpanY();

                float scaleX = spanX / mFirstSpanX;
                float scaleY = spanY / mFirstSpanY;
               // float scaleY = (float)Math.pow(spanY / mFirstSpanY, 2);

                Log.d(TAG, "onScaleEnd::x:" + scaleX + " y:" + scaleY);

                if (mOnDataChanged != null)
                {
                    if(mScaleDir == VERTICAL || mScaleDir == BOTH)
                    {
                        if(mSelectedPath > 0 && scaleY != 1.0)
                        {
                            mOnDataChanged.doCommand(
                                    ScopeInterface.Command.SET_VOLTAGE_SCALE,
                                    mSelectedPath,
                                    scaleY);
                        }
                    }

                    if(mScaleDir == HORIZONTAL || mScaleDir == BOTH)
                    {
                        if(scaleX != 1.0)
                        {
                            mOnDataChanged.doCommand(ScopeInterface.Command.SET_TIME_SCALE, 0, scaleX);
                        }
                    }
                }

                int numOn = channelOnCount();
                mChangeDelay = 4 * numOn;

                mChan1OffsetText = "";
                mChan2OffsetText = "";
            }
            for(CursorView cursorView : mCursorArray.values())
            {
                cursorView.update();
            }
            mInScaling = false;
        }
    }


    //////////////////////////////////////////////////////////////////////////
    //
    // Measurement Cursors
    //
    //////////////////////////////////////////////////////////////////////////

    public void setCursorsState(CursorStruct cursorStruct)
    {
        if(cursorStruct.cursorMode == CursorStruct.CursorMode.MANUAL)
        {
            if(mCursorArray.size() == 0)
            {
                int index = 1;
                CursorView cursorView = new CursorView(getContext());
                addView(cursorView);
                cursorView.layout(0, 0, mContentWidth, mContentHeight);

                cursorView.setCursorStruct(cursorStruct);
                cursorView.changeLocation(250, 150);
                cursorView.setIndex(index++);
                mCursorArray.put(cursorView.getIndex(), cursorView);


                cursorView = new CursorView(getContext());
                addView(cursorView);
                cursorView.layout(0, 0, mContentWidth, mContentHeight);

                cursorView.setCursorStruct(cursorStruct);
                cursorView.changeLocation(300, 200);
                cursorView.setIndex(index);
                mCursorArray.put(cursorView.getIndex(), cursorView);
            }
            else
            {
                for(CursorView cursorView : mCursorArray.values())
                {
                    cursorView.setCursorStruct(cursorStruct);
                }
            }
        }
        else if(cursorStruct.cursorMode == CursorStruct.CursorMode.OFF)
        {
            for(CursorView cursorView : mCursorArray.values())
            {
                removeView(cursorView);
            }
            mCursorArray.clear();
            mCursorText = "";
        }
    }

    private void updateCursorDifferenceText()
    {
        if(mCursorArray.size() == 2)
        {
            double firstVal = 0, secondVal = 0;
            boolean first = true;
            CursorStruct cursorStruct = null;
            for(CursorView cursorView : mCursorArray.values())
            {
                if(first)
                {
                    firstVal = cursorView.getValue();
                    cursorStruct = cursorView.getCursorStruct();
                    first = false;
                }
                else
                {
                    secondVal = cursorView.getValue();
                }
            }

            assert cursorStruct != null;
            if(cursorStruct.cursorType == CursorStruct.CursorType.Y)
            {
                double dist = Math.abs(secondVal - firstVal);
                mCursorText = updateVoltText("|ΔY| = ", dist);
            }
            else
            {
                double dist = Math.abs(secondVal - firstVal);
                mCursorText = updateTimeText("|ΔX| = ", dist);
            }
        }
        else
            mCursorText = "";
    }

    private class CursorView extends View
    {
        private static final int TOUCH_RADIUS = 30;

        private float mPosX = 0;
        private float mPosY = 0;
        private float mWidth = 0;
        private float mHeight = 0;
        private double mValue = 0;
        private String mText = "";
        private int mIndex = -1;
        private CursorStruct mCursorStruct = new CursorStruct();

        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CursorView(Context context)
        {
            super(context);
            init();
        }

        public CursorView(Context context, AttributeSet attrs)
        {
            super(context, attrs);
            init();
        }

        public CursorView(Context context, AttributeSet attrs, int defStyleAttr)
        {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init()
        {
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(1);
            mPaint.setColor(Color.GREEN);

            mTextPaint.setColor(Color.BLACK);
            mTextPaint.setTextSize(12);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
        }

        public void setCursorStruct(CursorStruct cursorStruct)
        {
            mCursorStruct = cursorStruct;
            update();
        }

        public CursorStruct getCursorStruct()
        {
            return mCursorStruct;
        }

        public double getValue()
        {
            return mValue;
        }

        public void setIndex(int index)
        {
            mIndex = index;
        }

        public int getIndex()
        {
            return mIndex;
        }

        public void changeLocation(float x, float y)
        {
            mPosX = Math.min(Math.max(x,0), mWidth);
            mPosY = Math.min(Math.max(y,0), mHeight);

            update();
        }

        public void update()
        {
            if(mCursorStruct.cursorType == CursorStruct.CursorType.X && mPrevTime != null)
            {
                mValue = mPrevTime.timeOffset - fromScreenPosH(mPrevTime.timeScale, mPosX);
                mValue = BaseScope.roundValue(mValue, mPrevTime.timeScale, 4);
                mText = updateTimeText("", mValue);
            }
            else if(mCursorStruct.cursorSource == CursorStruct.CursorSource.CH1 && mPrevChan1 != null)
            {
                mValue = fromScreenPosV(mPrevChan1.voltageScale, mPosY) - mPrevChan1.voltageOffset;
                mValue = BaseScope.roundValue(mValue, mPrevChan1.voltageScale, 2);
                mText = updateVoltText("", mValue);
            }
            else if(mCursorStruct.cursorSource == CursorStruct.CursorSource.CH2 && mPrevChan2 != null)
            {
                mValue = fromScreenPosV(mPrevChan2.voltageScale, mPosY) - mPrevChan2.voltageOffset;
                mValue = BaseScope.roundValue(mValue, mPrevChan2.voltageScale, 2);
                mText = updateVoltText("", mValue);
            }

            updateCursorDifferenceText();
            invalidate();
        }

        public boolean hitTest(float x, float y)
        {
            return (x < mPosX + TOUCH_RADIUS && x > mPosX - TOUCH_RADIUS &&
                    y < mPosY + TOUCH_RADIUS && y > mPosY - TOUCH_RADIUS);
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
            if(mCursorStruct.cursorType == CursorStruct.CursorType.X)
            {
                canvas.drawLine(mPosX, 0, mPosX, mHeight, mPaint);
            }
            else
            {
                canvas.drawLine(0, mPosY, mWidth, mPosY, mPaint);
            }
            canvas.drawCircle(mPosX, mPosY, TOUCH_RADIUS, mPaint);
            canvas.drawText(mText, mPosX, mPosY + 6, mTextPaint);

            super.onDraw(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            return false;
        }
    }
}
