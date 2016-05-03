package de.uni_weimar.mheinz.androidtouchscope.display;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import de.uni_weimar.mheinz.androidtouchscope.display.callback.OnDataChangedInterface;

//TODO: when offset is off screen, moving handel should offset data back to screen
public class HandleView extends View
{
    private static final int HANDLE_LENGTH = 50;
    private static final int HANDLE_BREADTH = 25;

    private final ShapeDrawable mShapeDrawable = new ShapeDrawable();
    private final Path mHandlePath = new Path();
    private final Paint mMainTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private GestureDetectorCompat mGestureDetector;
    private OnDataChangedInterface.OnDataChanged mOnDataChanged = null;

    private int mId = 0;
    private int mColor = Color.BLUE;
    private String mMainText = "";
    private boolean mIsOn = true;
    private HandleDirection mOrientation = HandleDirection.RIGHT;
    private RectF mBounds = new RectF(0, 0, HANDLE_LENGTH, HANDLE_BREADTH);
    private Rect mTextBounds = new Rect();

    private float mHandlePos = HANDLE_BREADTH / 2;
    private float mOldHandlePos = 0;

 //   private PointF mFirstTouch;
    private boolean mTouched = false;
    private boolean mIsMoving = false;

    public HandleView(Context context)
    {
        super(context);
        init();
    }

    public HandleView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public HandleView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setHandleId(int id)
    {
        mId = id;
    }

    public void setOnDoCommand(OnDataChangedInterface.OnDataChanged onDataChanged)
    {
        mOnDataChanged = onDataChanged;
    }

    public void setHandlePosition(float pos)
    {
        //only move handle if move is big enough
       /* if(Math.abs(pos - mHandlePos) < 5)
            return;*/

        if(mOrientation == HandleDirection.UP || mOrientation == HandleDirection.DOWN)
        {
            mHandlePos = Math.max(mBounds.left, Math.min(mBounds.right, pos));
        }
        else
        {
            mHandlePos = Math.max(mBounds.top, Math.min(mBounds.bottom, pos));
        }
        makeHandle();
    }

    private void init()
    {
        mGestureDetector = new GestureDetectorCompat(getContext(), new SimpleGestureListener());

        mShapeDrawable.setShape(new PathShape(mHandlePath, mBounds.width(), mBounds.height()));
        mShapeDrawable.setBounds(0, 0, (int)mBounds.width(), (int)mBounds.height());
        setLayerType(LAYER_TYPE_SOFTWARE, mShapeDrawable.getPaint());
        makeHandle();

        /*mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setColor(mColor);*/

        mIsOn = true;

        mMainTextPaint.setColor(Color.BLACK);
        mMainTextPaint.setTextSize(15);
        mMainTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setAttributes(int color, String mainText, HandleDirection orientation)
    {
        mColor = color;
        mMainText = mainText;
        mOrientation = orientation;

        invalidate();
    }

    public void setIsOn(boolean isOn)
    {
        if(isOn != mIsOn)
            invalidate();
        mIsOn = isOn;
    }

    private void makeHandle()
    {
        mHandlePath.rewind();

        if(mOrientation == HandleDirection.RIGHT)
        {
            mHandlePath.moveTo(5, mHandlePos);
            mHandlePath.lineTo(5, mHandlePos + HANDLE_BREADTH / 2);
            mHandlePath.lineTo(5 + HANDLE_LENGTH * 3/4, mHandlePos + HANDLE_BREADTH / 2);
            mHandlePath.lineTo(mBounds.right, mHandlePos);
            mHandlePath.lineTo(5 + HANDLE_LENGTH * 3/4, mHandlePos - HANDLE_BREADTH / 2);
            mHandlePath.lineTo(5, mHandlePos - HANDLE_BREADTH / 2);
            mHandlePath.close();
        }
        else if(mOrientation == HandleDirection.LEFT)
        {
            mHandlePath.moveTo(mBounds.right - 5, mHandlePos);
            mHandlePath.lineTo(mBounds.right - 5, mHandlePos + HANDLE_BREADTH / 2);
            mHandlePath.lineTo(mBounds.right - 5 - HANDLE_LENGTH * 3/4, mHandlePos + HANDLE_BREADTH / 2);
            mHandlePath.lineTo(mBounds.left, mHandlePos);
            mHandlePath.lineTo(mBounds.right - 5 - HANDLE_LENGTH * 3/4, mHandlePos - HANDLE_BREADTH / 2);
            mHandlePath.lineTo(mBounds.right - 5, mHandlePos - HANDLE_BREADTH / 2);
            mHandlePath.close();
        }
        else if(mOrientation == HandleDirection.DOWN)
        {
            mHandlePath.moveTo(mHandlePos, 5);
            mHandlePath.lineTo(mHandlePos + HANDLE_BREADTH / 2, 5);
            mHandlePath.lineTo(mHandlePos + HANDLE_BREADTH / 2, 5 + HANDLE_LENGTH * 3/4);
            mHandlePath.lineTo(mHandlePos, mBounds.bottom);
            mHandlePath.lineTo(mHandlePos - HANDLE_BREADTH / 2, 5 + HANDLE_LENGTH * 3/4);
            mHandlePath.lineTo(mHandlePos - HANDLE_BREADTH / 2, 5);
            mHandlePath.close();
        }
        invalidate();
    }

    private PointF getCircleCenter()
    {
        float posX, posY;
        if(mOrientation == HandleDirection.UP || mOrientation == HandleDirection.DOWN)
        {
            posX = mHandlePos;//Math.max(mBounds.left, Math.min(mBounds.right, mHandlePos));
            posY = mBounds.centerY();
        }
        else
        {
            posX = mBounds.centerX();
            posY = mHandlePos;//Math.max(mBounds.top, Math.min(mBounds.bottom, mHandlePos));
        }
        return new PointF(posX,posY);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if(mIsOn)
        {
            mShapeDrawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            mShapeDrawable.getPaint().setColor(mColor);
            mShapeDrawable.getPaint().setShadowLayer(2,2,2,Color.GRAY);
            mShapeDrawable.draw(canvas);
        }
        else
        {
            mShapeDrawable.getPaint().setStyle(Paint.Style.FILL);
            mShapeDrawable.getPaint().setColor(Color.WHITE);
            mShapeDrawable.getPaint().setShadowLayer(2,2,2,Color.GRAY);
            mShapeDrawable.draw(canvas);
            mShapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
            mShapeDrawable.getPaint().setColor(Color.BLACK);
            mShapeDrawable.draw(canvas);
        }

        PointF center = getCircleCenter();
        mMainTextPaint.getTextBounds(mMainText, 0, mMainText.length(), mTextBounds);
        canvas.drawText(mMainText, center.x, center.y + mTextBounds.height() / 2, mMainTextPaint);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        if(mOrientation == HandleDirection.UP || mOrientation == HandleDirection.DOWN)
        {
            mBounds = new RectF(HANDLE_BREADTH / 2, 0, width - 2 - HANDLE_BREADTH / 2, height);
            mHandlePos = mBounds.centerX();
        }
        else
        {
            mBounds = new RectF(0, HANDLE_BREADTH / 2, width, height - 2 - HANDLE_BREADTH / 2);
            mHandlePos = mBounds.centerY();
        }
        makeHandle();

      //  super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    public int getHandleLength()
    {
        return HANDLE_LENGTH;
    }

    public int getHandleBreadth()
    {
        return HANDLE_BREADTH;
    }

    private boolean touchSelectCursor(MotionEvent event)
    {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final float x = MotionEventCompat.getX(event, pointerIndex);
        final float y = MotionEventCompat.getY(event, pointerIndex);

        PointF point = getCircleCenter();

        boolean selected = false;
        if(x >= point.x - HANDLE_LENGTH && x <= point.x + HANDLE_LENGTH &&
           y >= point.y - HANDLE_LENGTH && y <= point.y + HANDLE_LENGTH)
        {
            selected = true;
          //  invalidate();
        }

        return selected;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean hit = touchSelectCursor(event);
        mGestureDetector.onTouchEvent(event);

        if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP)
        {
            if(mIsMoving && mIsOn)
            {
                if(mId == HostView.ID_HANDLE_1 || mId == HostView.ID_HANDLE_2)
                {
                   // mOnDataChanged.moveWave(mId, mFirstTouch.y - mHandlePos - HANDLE_BREADTH / 2, false);
                    mOnDataChanged.moveWave(mId, mOldHandlePos - mHandlePos, false);
                }
                else if(mId == HostView.ID_HANDLE_TIME)
                {
                 //   mOnDataChanged.moveTime(mFirstTouch.x - mHandlePos - HANDLE_BREADTH / 2, false);
                    mOnDataChanged.moveTime(mOldHandlePos - mHandlePos, false);
                }
                else if(mId == HostView.ID_HANDLE_TRIG)
                {
                   // mOnDataChanged.moveTrigger(mFirstTouch.y - mHandlePos /*- HANDLE_BREADTH / 2*/, false);
                    mOnDataChanged.moveTrigger(mOldHandlePos - mHandlePos, false);
                }
            }
            mIsMoving = false;
            mTouched = false;
        }

        return hit;
    }

    private class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public void onLongPress(MotionEvent e)
        {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mTouched)
            {
                mIsMoving = true;
                if(mOrientation == HandleDirection.UP || mOrientation == HandleDirection.DOWN)
                {
                    setHandlePosition(mHandlePos - distanceX);
                }
                else
                {
                    setHandlePosition(mHandlePos - distanceY);
                }

                if(mIsOn)
                {
                    if (mId == HostView.ID_HANDLE_1 || mId == HostView.ID_HANDLE_2)
                    {
                        mOnDataChanged.moveWave(mId, -distanceY, true);
                    }
                    else if (mId == HostView.ID_HANDLE_TIME)
                    {
                        mOnDataChanged.moveTime(-distanceX, true);
                    }
                    else if (mId == HostView.ID_HANDLE_TRIG)
                    {
                        mOnDataChanged.moveTrigger(-distanceY, true);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event)
        {
            mTouched = touchSelectCursor(event);
            mOldHandlePos = mHandlePos;

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            //TODO: add menu to change settings for each handle type
            return touchSelectCursor(event);
        }
    }

    public enum HandleDirection
    {
        RIGHT,
        LEFT,
        DOWN,
        UP
    }
}
