package de.uni_weimar.mheinz.androidtouchscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import de.uni_weimar.mheinz.androidtouchscope.scope.BaseScope;
import de.uni_weimar.mheinz.androidtouchscope.scope.RigolScope;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
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

    private String mChan1Text = "";
    private String mChan2Text = "";
    private String mMathText = "";
    private String mTimeText = "";

    private Point mTextPos;

    private int mContentWidth = 0;
    private int mContentHeight = 0;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mScopeDetector;

    private int mSelectedPath = -1; // -1 if not selected
    private OnDoCommand mOnDoCommand = null;

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

    public void setOnDoCommand(OnDoCommand onDoCommand)
    {
        mOnDoCommand = onDoCommand;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Init functions
    //
    //////////////////////////////////////////////////////////////////////////

    private void initGestures(Context context)
    {
        mScopeDetector = new GestureDetectorCompat(context,new ScopeGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScopeScaleListener());
    }

    private void init()
    {
       // setLayerType(LAYER_TYPE_SOFTWARE, null);

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

        setLayerType(LAYER_TYPE_SOFTWARE, drawable.getPaint());
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

    //////////////////////////////////////////////////////////////////////////
    //
    // Update View functions
    //
    //////////////////////////////////////////////////////////////////////////

    public void setChannelData(int channel, WaveData waveData, TimeData timeData)
    {
        switch(channel)
        {
            case 1:
                updatePath(mPathChan1, waveData);
                mChan1Text = updateVoltText(waveData, "Chan1");
                break;
            case 2:
                updatePath(mPathChan2, waveData);
                mChan2Text = updateVoltText(waveData, "Chan2");
                break;
            case 3:
                updatePath(mPathMath, waveData);
                mMathText = updateVoltText(waveData, "Math");
                break;
        }
        mTimeText = updateTimeText(timeData);

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

    private String updateVoltText(WaveData waveData, String chan)
    {
        if(waveData != null && waveData.data != null)
        {
            double value = 0.0;
            String end = "";

            if (waveData.voltageScale < 1)
            {
                value = waveData.voltageScale * 1e3;
                end = "mV";
            }
            else
            {
                value = waveData.voltageScale;
                end = "V";
            }

            return String.format("%s: %.2f%s", chan, value, end);
        }
        else
        {
            return "";
        }
    }

    private String updateTimeText(TimeData timeData)
    {
        if(timeData == null)
            return "";

        double value = 0.0;
        String end = "";
        double time = timeData.timeScale;
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
        return String.format("Time: %.2f%s",value,end);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Helper functions
    //
    //////////////////////////////////////////////////////////////////////////

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

    //////////////////////////////////////////////////////////////////////////
    //
    // Class overrides
    //
    //////////////////////////////////////////////////////////////////////////

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
        init();
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Path Selection
    //
    //////////////////////////////////////////////////////////////////////////

    /*private float smallestDistanceToPath(PathPoints path, float x, float y)
    {
        PointF[] points = path.getPoints();
        float minDist = 1000f;
        float dist = 0f;

        for(PointF point : points)
        {
            dist = (float)Math.hypot(x - point.x, y - point.y);
            if(dist < minDist)
                minDist = dist;
        }
        return minDist;
    }*/

    private float smallestDistanceToPath(Path path, float x, float y)
    {
        PathMeasure measure = new PathMeasure(path,false);
        float coord[] = {0f, 0f};
        float minDist = 1000f;
        float dist = 0f;

        for(int i = 0; i < measure.getLength(); ++i)
        {
            measure.getPosTan(i, coord, null);
            dist = (float)Math.hypot(x - coord[0], y - coord[1]);
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
            minDist = dist;
        }

        dist = smallestDistanceToPath(mPathMath,x,y);
        if(dist < minDist)
        {
            selected = 3;
        }

        return selected;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Methods related to gesture handling
    //
    //////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean retVal = mScaleGestureDetector.onTouchEvent(event);
        retVal = mScopeDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }

    private class ScopeGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown(MotionEvent event)
        {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mSelectedPath > 0)
            {
                if(mOnDoCommand != null)
                    mOnDoCommand.doCommand(
                            BaseScope.Command.SET_VOLTAGE_OFFSET,
                            mSelectedPath,
                            (Float)(-distanceY / (mContentHeight / 8.0f)));
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            final int pointerIndex = MotionEventCompat.getActionIndex(event);
            final float x = MotionEventCompat.getX(event, pointerIndex);
            final float y = MotionEventCompat.getY(event, pointerIndex);

            int hit = pathHitTest(x, y, 15f);
            if(hit == -1 && mSelectedPath == -1)
                return true;

            if(mSelectedPath == hit)
                mSelectedPath = -1;
            else
                mSelectedPath = hit;

            if(mOnDoCommand != null)
                mOnDoCommand.doCommand(BaseScope.Command.SET_ACTIVE_CHANNEL, mSelectedPath, null);

            mDrawableChan1.getPaint().clearShadowLayer();
            mDrawableChan1.getPaint().setStrokeWidth(1);
            mDrawableChan2.getPaint().clearShadowLayer();
            mDrawableChan2.getPaint().setStrokeWidth(1);
            mDrawableMath.getPaint().clearShadowLayer();
            mDrawableMath.getPaint().setStrokeWidth(1);

            switch(mSelectedPath)
            {
                case 1:
                    mDrawableChan1.getPaint().setShadowLayer(10f,0f,0f,Color.YELLOW);
                    mDrawableChan1.getPaint().setStrokeWidth(1.5f);
                    break;
                case 2:
                    mDrawableChan2.getPaint().setShadowLayer(10f,0f,0f,Color.BLUE);
                    mDrawableChan2.getPaint().setStrokeWidth(1.5f);
                    break;
                case 3:
                    mDrawableMath.getPaint().setShadowLayer(10f,0f,0f,Color.MAGENTA);
                    mDrawableMath.getPaint().setStrokeWidth(1.5f);
                    break;
                default:
                    break;
            }

            return true;
        }
    }

    private class ScopeScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        private float mLastSpanX;
        private float mLastSpanY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
        {
            mLastSpanX = scaleGestureDetector.getCurrentSpanX();
            mLastSpanY = scaleGestureDetector.getCurrentSpanY();
            return true;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Callback methods
    //
    //////////////////////////////////////////////////////////////////////////

    interface OnDoCommand
    {
        void doCommand(BaseScope.Command command, int channel, Object specialData);
    }
}
