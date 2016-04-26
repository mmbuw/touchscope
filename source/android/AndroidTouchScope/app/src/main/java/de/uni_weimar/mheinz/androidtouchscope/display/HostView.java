package de.uni_weimar.mheinz.androidtouchscope.display;


import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import de.uni_weimar.mheinz.androidtouchscope.R;

public class HostView extends ViewGroup
{
    private Cursor mChan1Cursor;
    private Cursor mChan2Cursor;
    private Cursor mTimeCursor;
    private Cursor mTrigCursor;

    public HostView(Context context)
    {
        super(context);
        init();
    }

    public HostView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public HostView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init()
    {
        mChan1Cursor = new Cursor(getContext());
        mChan1Cursor.setAttributes(Color.YELLOW, "Chan1");
        addView(mChan1Cursor);

        mChan2Cursor = new Cursor(getContext());
        mChan2Cursor.setAttributes(Color.BLUE, "Chan2");
        addView(mChan2Cursor);

        mTimeCursor = new Cursor(getContext());
        mTimeCursor.setAttributes(Color.GREEN, "Time");
        addView(mTimeCursor);

        mTrigCursor = new Cursor(getContext());
        mTrigCursor.setAttributes(Color.YELLOW, "Trig");
        addView(mTrigCursor);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        // These are the far left and right edges in which we are performing layout.
        int leftPos = getPaddingLeft();
        int rightPos = w - leftPos - getPaddingRight();
        final int midH = (rightPos - leftPos) / 2;

        // These are the top and bottom edges in which we are performing layout.
        final int parentTop = getPaddingTop();
        final int parentBottom =  h - parentTop - getPaddingBottom();
        final int midV = (parentBottom - parentTop) / 2;

        int cursorWidth = mChan1Cursor.getMeasuredWidth();
        int cursorHeight = mChan1Cursor.getMeasuredHeight();

        mChan1Cursor.layout(leftPos, midV, leftPos + cursorWidth, midV + cursorHeight);
        mChan2Cursor.layout(leftPos, midV + cursorHeight, leftPos + cursorWidth, midV + 2*cursorHeight);
        mTimeCursor.layout(midH, parentTop, midH + cursorWidth, parentTop + cursorHeight);
        mTrigCursor.layout(rightPos - cursorWidth, midV, rightPos, midV + cursorHeight);

        View scopeView = findViewById(R.id.scopeView);
        scopeView.layout(leftPos + cursorWidth, parentTop + cursorHeight,rightPos - cursorWidth, parentBottom);
    }
}