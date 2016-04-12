package de.uni_weimar.mheinz.androidtouchscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.view.View;

import de.uni_weimar.mheinz.androidtouchscope.scope.RigolScope;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class ScopeView extends View
{
    private ShapeDrawable mDrawableChan1 = new ShapeDrawable();
    private ShapeDrawable mDrawableChan2 = new ShapeDrawable();
    private ShapeDrawable mDrawableMath = new ShapeDrawable();
    private ShapeDrawable mDrawableGridH = new ShapeDrawable();
    private ShapeDrawable mDrawableGridV = new ShapeDrawable();

    private Path mPathChan1 = new Path();
    private Path mPathChan2 = new Path();
    private Path mPathMath = new Path();
    private Path mPathGridH = new Path();
    private Path mPathGridV = new Path();

    private Paint mChan1TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mChan2TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mMathTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String mChan1Text = "Chan1";
    private String mChan2Text = "Chan2";
    private String mMathText = "Math";
    private String mTimeText = "Time";

    private Point mTextPos;

    private int mContentWidth = 0;
    private int mContentHeight = 0;

    public ScopeView(Context context)
    {
        super(context);
        init(null, 0);
    }

    public ScopeView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs, 0);
    }

    public ScopeView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle)
    {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mContentWidth = getWidth() - paddingLeft - paddingRight;
        mContentHeight = getHeight() - paddingTop - paddingBottom;
        mTextPos = new Point(5,mContentHeight - 5);

        initDrawable(mDrawableChan1, mPathChan1, Color.YELLOW, mContentWidth, mContentHeight);
        initDrawable(mDrawableChan2, mPathChan2, Color.BLUE, mContentWidth, mContentHeight);
        initDrawable(mDrawableMath, mPathMath, Color.MAGENTA, mContentWidth, mContentHeight);
        initGridH(mDrawableGridH, mPathGridH, Color.GRAY, mContentWidth, mContentHeight);
        initGridV(mDrawableGridV, mPathGridV, Color.GRAY, mContentWidth, mContentHeight);
        initText(mChan1TextPaint, 15, Color.YELLOW);
        initText(mChan2TextPaint, 15, Color.BLUE);
        initText(mMathTextPaint, 15, Color.MAGENTA);
        initText(mTimeTextPaint, 15, Color.WHITE);
    }

    private void initDrawable(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.moveTo(0, 0);
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.setBounds(0, 0, width, height);
    }

    private void initGridV(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.rewind();
        float cellWidth = width / 12.0f;
        for(int i = 0; i < 12; ++i)
        {
            path.moveTo(i * cellWidth,0);
            path.lineTo(i * cellWidth,height);
        }

        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.getPaint().setPathEffect(new DashPathEffect(new float[]{1, (height - 40) / 40.0f}, 0));
        drawable.setBounds(0, 0, width, height);
    }

    private void initGridH(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.rewind();
        float cellHeight = height / 8.0f;
        for(int i = 0; i < 8; ++i)
        {
            path.moveTo(0,i * cellHeight);
            path.lineTo(width,i * cellHeight);
        }

        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.getPaint().setPathEffect(new DashPathEffect(new float[]{1, (width - 60) / 60.0f}, 0));
        drawable.setBounds(0, 0, width, height);
    }

    private void initText(Paint paint, int height, int color)
    {
        paint.setColor(color);
        paint.setTextSize(height);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    public void setChannelData(int channel, WaveData waveData)
    {
        switch(channel)
        {
            case 1:
                updatePath(mPathChan1, waveData);
                mChan1Text = updateText(waveData ,"Chan1", true);
                break;
            case 2:
                updatePath(mPathChan2, waveData);
                mChan2Text = updateText(waveData ,"Chan2", true);
                break;
            case 3:
                updatePath(mPathMath, waveData);
                mMathText = updateText(waveData ,"Math", true);
                break;
        }
        if(waveData != null)
            mTimeText = updateText(waveData ,"Time", false);
     //   else
     //       mTimeText = "Time";

        postInvalidate();
    }

    private void updatePath(Path path, WaveData waveData)
    {
        path.rewind();
        if(waveData == null || waveData.data == null || waveData.data.length == 0)
            return;

        double vScale = waveData.voltageScale;
        if(vScale == 0)
            vScale = 1.0f;

        float widthRatio = (float)(mContentWidth) / (waveData.data.length - 11);

        float point = manipulatePoint(waveData.voltageOffset, vScale, waveData.data[11]);
        path.moveTo(0, point);
        for(int i = 12, j = 1; i < waveData.data.length; ++i, ++j)
        {
            point = manipulatePoint(waveData.voltageOffset, vScale, waveData.data[i]);
            path.lineTo(j * widthRatio, point);
        }
    }

    private String updateText(WaveData waveData, String chan, boolean isVolt)
    {
        double value = 0.0;
        String end = "";
        if(waveData != null)
        {
            if(isVolt)
            {
                if(waveData.voltageScale < 1)
                {
                    value = waveData.voltageScale * 1e3;
                    end = "mV";
                }
                else
                {
                    value = waveData.voltageScale;
                    end = "V";
                }
            }
            else
            {
                double time = waveData.timeScale;
                if(time < 1e-6)
                {
                    value = (time * 1e9);
                    end = "nS";
                }
                else if(time < 1e-3)
                {
                    value = time * 1e6;
                    end = "uS";
                }
                else if(time < 1)
                {
                    value = time * 1e3;
                    end = "mS";
                }
                else
                {
                    value = time;
                    end = "S";
                }
            }
        }
        return String.format("%s: %.2f%s",chan,value,end);
    }

    private float manipulatePoint(double voltOffset, double voltScale, int data)
    {
        float heightRatio = (mContentHeight / 8.0f) / (float)voltScale;
        float mid = (mContentHeight / 2.0f);

        float point = (float)RigolScope.actualVoltage(voltOffset, voltScale, data);
        point = mid - ((point + (float)voltOffset) * heightRatio);
        if(point < 0)
            point = 0;
        else if(point > mContentHeight)
            point = mContentHeight;

        return point;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        mDrawableChan1.draw(canvas);
        mDrawableChan2.draw(canvas);
        mDrawableMath.draw(canvas);
        mDrawableGridH.draw(canvas);
        mDrawableGridV.draw(canvas);
        canvas.drawText(mChan1Text, mTextPos.x, mTextPos.y, mChan1TextPaint);
        canvas.drawText(mChan2Text,mTextPos.x + 150,mTextPos.y,mChan2TextPaint);
        canvas.drawText(mMathText,mTextPos.x + 300,mTextPos.y,mMathTextPaint);
        canvas.drawText(mTimeText,mTextPos.x + 450,mTextPos.y,mTimeTextPaint);

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        init(null,0);
    }
}
