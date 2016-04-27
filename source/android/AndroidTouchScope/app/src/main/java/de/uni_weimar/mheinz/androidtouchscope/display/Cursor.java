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

public class Cursor extends View
{
    private static final int HANDLE_LENGTH = 50;
    private static final int HANDLE_BREADTH = 25;

    private final ShapeDrawable mShapeDrawable = new ShapeDrawable();
    private final Path mCursorHandel = new Path();
    private final Paint mMainTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private GestureDetectorCompat mGestureDetector;

    private int mColor = Color.BLUE;
    private String mMainText = "";
    private boolean mSelected;
    private HandelDirection mOrientation = HandelDirection.RIGHT;
    private RectF mBounds = new RectF(0, 0, HANDLE_LENGTH, HANDLE_BREADTH);

    private float mHandelPos = HANDLE_BREADTH / 2;

    public Cursor(Context context)
    {
        super(context);
        init();
    }

    public Cursor(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public Cursor(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init()
    {
        mGestureDetector = new GestureDetectorCompat(getContext(), new SimpleGestureListener());

        mShapeDrawable.setShape(new PathShape(mCursorHandel, mBounds.width(), mBounds.height()));
        mShapeDrawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        mShapeDrawable.getPaint().setColor(mColor);
        mShapeDrawable.setBounds(0, 0, (int)mBounds.width(), (int)mBounds.height());
        makeHandel();

        /*mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setColor(mColor);*/

        mSelected = false;

        mMainTextPaint.setColor(Color.BLACK);
        mMainTextPaint.setTextSize(15);
        mMainTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setAttributes(int color, String mainText, HandelDirection orientation)
    {
    //    mColor = color;
        mMainText = mainText;
        mOrientation = orientation;
        mShapeDrawable.getPaint().setColor(color);

        invalidate();
    }

    private void makeHandel()
    {
        mCursorHandel.rewind();

        if(mOrientation == HandelDirection.RIGHT)
        {
            mCursorHandel.moveTo(5, mHandelPos);
            mCursorHandel.lineTo(5, mHandelPos + HANDLE_BREADTH / 2);
            mCursorHandel.lineTo(5 + HANDLE_LENGTH * 3/4, mHandelPos + HANDLE_BREADTH / 2);
            mCursorHandel.lineTo(mBounds.right, mHandelPos);
            mCursorHandel.lineTo(5 + HANDLE_LENGTH * 3/4, mHandelPos - HANDLE_BREADTH / 2);
            mCursorHandel.lineTo(5, mHandelPos - HANDLE_BREADTH / 2);
            mCursorHandel.close();
        }
        else if(mOrientation == HandelDirection.LEFT)
        {
            mCursorHandel.moveTo(mBounds.right - 5, mHandelPos);
            mCursorHandel.lineTo(mBounds.right - 5, mHandelPos + HANDLE_BREADTH / 2);
            mCursorHandel.lineTo(mBounds.right - 5 - HANDLE_LENGTH * 3/4, mHandelPos + HANDLE_BREADTH / 2);
            mCursorHandel.lineTo(mBounds.left, mHandelPos);
            mCursorHandel.lineTo(mBounds.right - 5 - HANDLE_LENGTH * 3/4, mHandelPos - HANDLE_BREADTH / 2);
            mCursorHandel.lineTo(mBounds.right - 5, mHandelPos - HANDLE_BREADTH / 2);
            mCursorHandel.close();
        }
        else if(mOrientation == HandelDirection.DOWN)
        {
            mCursorHandel.moveTo(mHandelPos, 5);
            mCursorHandel.lineTo(mHandelPos + HANDLE_BREADTH / 2, 5);
            mCursorHandel.lineTo(mHandelPos + HANDLE_BREADTH / 2, 5 + HANDLE_LENGTH * 3/4);
            mCursorHandel.lineTo(mHandelPos, mBounds.bottom);
            mCursorHandel.lineTo(mHandelPos - HANDLE_BREADTH / 2, 5 + HANDLE_LENGTH * 3/4);
            mCursorHandel.lineTo(mHandelPos - HANDLE_BREADTH / 2, 5);
            mCursorHandel.close();
        }
    }

    private PointF getCircleCenter()
    {
        float posX, posY;
        if(mOrientation == HandelDirection.UP || mOrientation == HandelDirection.DOWN)
        {
            posX = mHandelPos;//Math.max(mBounds.left, Math.min(mBounds.right, mHandelPos));
            posY = mBounds.centerY();
        }
        else
        {
            posX = mBounds.centerX();
            posY = mHandelPos;//Math.max(mBounds.top, Math.min(mBounds.bottom, mHandelPos));
        }
        return new PointF(posX,posY);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

       // mPaint.setStyle(Paint.Style.STROKE);
       // mPaint.setColor(mColor);
        mShapeDrawable.draw(canvas);

        PointF center = getCircleCenter();

      //  canvas.drawCircle(center.x, center.y, CIRCLE_RADIUS, mPaint);

        /*if(mSelected)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAlpha(100);
            canvas.drawCircle(center.x, center.y, CIRCLE_RADIUS, mPaint);
        }*/

        Rect textBounds = new Rect();
        mMainTextPaint.getTextBounds(mMainText, 0, mMainText.length(), textBounds);
        canvas.drawText(mMainText, center.x, center.y + textBounds.height() / 2, mMainTextPaint);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        if(mOrientation == HandelDirection.UP || mOrientation == HandelDirection.DOWN)
        {
            mBounds = new RectF(HANDLE_BREADTH / 2, 0, width - HANDLE_BREADTH / 2, height);
        }
        else
        {
            mBounds = new RectF(0, HANDLE_BREADTH / 2, width, height - HANDLE_BREADTH / 2);
        }
        makeHandel();

      //  super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    public int getHandelLength()
    {
        return HANDLE_LENGTH;
    }

    public int getHandelBreadth()
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
          //  mSelected = true;
            invalidate();
        }

        return selected;
    }

    private boolean mTouched = false;

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean hit = touchSelectCursor(event);
        mGestureDetector.onTouchEvent(event);

        if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP)
            mTouched = false;

        return hit;
    }

    private class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        private PointF mFirstTouch;

        @Override
        public void onLongPress(MotionEvent e)
        {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mFirstTouch != null && mTouched)
            {
                if(mOrientation == HandelDirection.UP || mOrientation == HandelDirection.DOWN)
                {
                    mHandelPos = Math.max(mBounds.left, Math.min(mBounds.right, mHandelPos - distanceX));
                }
                else
                {
                    mHandelPos = Math.max(mBounds.top, Math.min(mBounds.bottom, mHandelPos - distanceY));
                }
                makeHandel();

                invalidate();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event)
        {
            int index = MotionEventCompat.getActionIndex(event);
            float x = MotionEventCompat.getX(event, index);
            float y = MotionEventCompat.getY(event, index);
            mTouched = touchSelectCursor(event);
            mFirstTouch = new PointF(x,y);

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            return touchSelectCursor(event);
        }
    }

    public enum HandelDirection
    {
        RIGHT,
        LEFT,
        DOWN,
        UP
    }
}
