package de.uni_weimar.mheinz.androidtouchscope.display;


import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import de.uni_weimar.mheinz.androidtouchscope.R;

public class HostView extends ViewGroup
{
    private ScopeView mScopeView;
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
        mChan1Cursor.setAttributes(Color.YELLOW, "1", Cursor.HandelDirection.RIGHT);
        addView(mChan1Cursor);

        mChan2Cursor = new Cursor(getContext());
        mChan2Cursor.setAttributes(Color.BLUE, "2", Cursor.HandelDirection.RIGHT);
        addView(mChan2Cursor);

        mTimeCursor = new Cursor(getContext());
        mTimeCursor.setAttributes(Color.rgb(255,215,0), "T", Cursor.HandelDirection.DOWN);
        addView(mTimeCursor);

        mTrigCursor = new Cursor(getContext());
        mTrigCursor.setAttributes(Color.rgb(255,215,0), "T", Cursor.HandelDirection.LEFT);
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
    //    final int midH = (rightPos - leftPos) / 2;

        // These are the top and bottom edges in which we are performing layout.
        final int topPos = getPaddingTop();
        final int bottomPos =  h - topPos - getPaddingBottom();

        int cursorLength = mChan1Cursor.getHandelLength();
        int cursorBreadth = mTimeCursor.getHandelBreadth();

        View buttonRow = findViewById(R.id.button_row);
        int buttonHeight = buttonRow.getMeasuredHeight();

        int cursorBottom = bottomPos - buttonHeight + cursorBreadth / 2;

     //   int scopeWidth = rightPos - leftPos - 2 * cursorWidth;
     //   int scopeHeight = bottomPos - topPos - cursorHeight;

        mChan1Cursor.layout(
                leftPos,
                topPos + cursorLength - cursorBreadth / 2,
                leftPos + cursorLength,
                cursorBottom);
        mChan2Cursor.layout(
                leftPos,
                topPos + cursorLength - cursorBreadth / 2,
                leftPos + cursorLength,
                cursorBottom);
        mTimeCursor.layout(
                leftPos + cursorLength - cursorBreadth / 2,
                topPos,
                rightPos - cursorLength + cursorBreadth / 2,
                topPos + cursorLength);
        mTrigCursor.layout(
                rightPos - cursorLength,
                topPos + cursorLength - cursorBreadth / 2,
                rightPos,
                cursorBottom);

        mScopeView = (ScopeView)findViewById(R.id.scopeView);
        mScopeView.layout(leftPos + cursorLength, topPos + cursorLength,rightPos - cursorLength, bottomPos - buttonHeight);

        buttonRow.layout(leftPos + cursorLength, bottomPos - buttonHeight, rightPos - cursorLength, bottomPos);

    }
}