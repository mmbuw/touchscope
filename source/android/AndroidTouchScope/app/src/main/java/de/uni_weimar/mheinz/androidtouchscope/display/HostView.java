package de.uni_weimar.mheinz.androidtouchscope.display;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;

import de.uni_weimar.mheinz.androidtouchscope.R;
import de.uni_weimar.mheinz.androidtouchscope.scope.ScopeInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class HostView extends ViewGroup
{
    private ScopeView mScopeView;
    private HandelView mChan1Handel;
    private HandelView mChan2Handel;
    private HandelView mTimeHandel;
    private HandelView mTrigHandel;

    private OnDoCommand mOnDoCommand = null;


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

    public void setOnDoCommand(OnDoCommand onDoCommand)
    {
        mOnDoCommand = onDoCommand;
    }

    public void setChannelData(int channel, WaveData waveData, TimeData timeData)
    {

        mScopeView.setChannelData(channel, waveData, timeData);
    }

    private void init()
    {
        mChan1Handel = new HandelView(getContext());
        mChan1Handel.setAttributes(Color.YELLOW, "CH1", HandelView.HandelDirection.RIGHT);
        addView(mChan1Handel);

        mChan2Handel = new HandelView(getContext());
        mChan2Handel.setAttributes(Color.BLUE, "CH2", HandelView.HandelDirection.RIGHT);
        addView(mChan2Handel);

        mTimeHandel = new HandelView(getContext());
        mTimeHandel.setAttributes(Color.rgb(255,215,0), "T", HandelView.HandelDirection.DOWN);
        addView(mTimeHandel);

        mTrigHandel = new HandelView(getContext());
        mTrigHandel.setAttributes(Color.rgb(255,215,0), "T", HandelView.HandelDirection.LEFT);
        addView(mTrigHandel);
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

        int cursorLength = mChan1Handel.getHandelLength();
        int cursorBreadth = mTimeHandel.getHandelBreadth();

        View buttonRow = findViewById(R.id.button_row);
        int buttonHeight = buttonRow.getMeasuredHeight();

        int cursorBottom = bottomPos - buttonHeight + cursorBreadth / 2;

     //   int scopeWidth = rightPos - leftPos - 2 * cursorWidth;
     //   int scopeHeight = bottomPos - topPos - cursorHeight;

        mChan1Handel.layout(
                leftPos,
                topPos + cursorLength - cursorBreadth / 2,
                leftPos + cursorLength,
                cursorBottom);
        mChan2Handel.layout(
                leftPos,
                topPos + cursorLength - cursorBreadth / 2,
                leftPos + cursorLength,
                cursorBottom);
        mTimeHandel.layout(
                leftPos + cursorLength - cursorBreadth / 2,
                topPos,
                rightPos - cursorLength + cursorBreadth / 2,
                topPos + cursorLength);
        mTrigHandel.layout(
                rightPos - cursorLength,
                topPos + cursorLength - cursorBreadth / 2,
                rightPos,
                cursorBottom);

        mScopeView = (ScopeView)findViewById(R.id.scopeView);
        mScopeView.layout(leftPos + cursorLength, topPos + cursorLength,rightPos - cursorLength, bottomPos - buttonHeight);
        mScopeView.setOnDoCommand(new OnDoCommand()
        {
            @Override
            public void doCommand(ScopeInterface.Command command, int channel, Object specialData)
            {
                if(mOnDoCommand != null)
                    mOnDoCommand.doCommand(command, channel, specialData);
            }

            @Override
            public void moveWave(int channel, float pos)
            {
                if(channel == 1)
                    mChan1Handel.setHandlePosition(pos + mChan1Handel.getHandelBreadth() / 2);
                else if(channel == 2)
                    mChan2Handel.setHandlePosition(pos + mChan2Handel.getHandelBreadth() / 2);
            }

            @Override
            public void moveTime(float pos)
            {
                mTimeHandel.setHandlePosition(pos + mTimeHandel.getHandelBreadth() / 2);
            }
        });

        buttonRow.layout(leftPos + cursorLength, bottomPos - buttonHeight, rightPos - cursorLength, bottomPos);

        post(new ExpandScopeViewArea());
    }

    private class ExpandScopeViewArea implements Runnable
    {
        @Override
        public void run()
        {
            ScopeView scopeView = (ScopeView) findViewById(R.id.scopeView);
            assert scopeView != null;

            //Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point(getWidth(), getHeight());
           // display.getSize(size);

            // scopeView.getHitRect(delegateArea);
            //set to available size
            Rect delegateArea = new Rect();
            delegateArea.left = 0;
            delegateArea.top = 0;
            delegateArea.right = size.x;
            delegateArea.bottom = size.y;
            TouchDelegate touchDelegate = new TouchDelegate(delegateArea, scopeView);

            if(View.class.isInstance(scopeView.getParent()))
            {
                ((View)scopeView.getParent()).setTouchDelegate(touchDelegate);
            }
        }
    }
}