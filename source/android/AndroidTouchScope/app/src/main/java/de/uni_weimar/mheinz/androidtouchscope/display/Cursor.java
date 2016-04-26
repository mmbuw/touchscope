package de.uni_weimar.mheinz.androidtouchscope.display;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class Cursor extends View
{
    private static final RectF VIEW_BOUNDS = new RectF(0,0,60,60);
    private static final RectF CIRCLE = new RectF(VIEW_BOUNDS.left + 5, VIEW_BOUNDS.top + 5,
            VIEW_BOUNDS.right - 5, VIEW_BOUNDS.bottom - 5);

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mMainTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mColor = Color.BLUE;
    private String mMainText = "";
    private boolean mSelected;

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
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setColor(mColor);
        mSelected = false;

        mMainTextPaint.setColor(Color.BLACK);
        mMainTextPaint.setTextSize(15);
        mMainTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setAttributes(int color, String mainText)
    {
        mColor = color;
        mMainText = mainText;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mColor);
        canvas.drawArc(CIRCLE, 0, 360, true, mPaint);

        mPaint.setStyle(Paint.Style.FILL);
        if(mSelected)
        {
            mPaint.setAlpha(100);
            canvas.drawArc(CIRCLE, 0, 360, true, mPaint);
        }


        Rect textBounds = new Rect();
        mMainTextPaint.getTextBounds(mMainText, 0, mMainText.length(), textBounds);
        canvas.drawText(mMainText, CIRCLE.centerX(), CIRCLE.centerY() + textBounds.height() / 2, mMainTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        setMeasuredDimension((int)VIEW_BOUNDS.width(), (int)VIEW_BOUNDS.height());
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
