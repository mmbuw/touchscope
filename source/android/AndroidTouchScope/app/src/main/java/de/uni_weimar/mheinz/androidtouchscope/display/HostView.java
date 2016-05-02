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
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class HostView extends ViewGroup
{
    private ScopeView mScopeView;
    private HandleView mChan1Handle;
    private HandleView mChan2Handle;
    private HandleView mTimeHandle;
    private HandleView mTrigHandle;

    static final int ID_HANDLE_1 = 1;
    static final int ID_HANDLE_2 = 2;
    static final int ID_HANDLE_TIME = 3;
    static final int ID_HANDLE_TRIG = 4;

    private OnDataChanged mOnDataChanged = null;


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

    public void setOnDoCommand(OnDataChanged onDataChanged)
    {
        mOnDataChanged = onDataChanged;
    }

    public void setChannelData(int channel, WaveData waveData, TimeData timeData, TriggerData trigData)
    {
        if(mScopeView != null)
            mScopeView.setChannelData(channel, waveData, timeData, trigData);

        if(mChan1Handle != null && channel == 1)
        {
            boolean off = waveData == null || waveData.data == null || waveData.data.length == 0;
            mChan1Handle.setIsOn(!off);
        }
        if(mChan2Handle != null && channel == 2)
        {
            boolean off = waveData == null || waveData.data == null || waveData.data.length == 0;
            mChan2Handle.setIsOn(!off);
        }
    }

    private void init()
    {
        mChan1Handle = new HandleView(getContext());
        mChan1Handle.setHandleId(ID_HANDLE_1);
        mChan1Handle.setAttributes(Color.YELLOW, "CH1", HandleView.HandleDirection.RIGHT);
        mChan1Handle.setOnDoCommand(mHandleOnDataChanged);
        addView(mChan1Handle);

        mChan2Handle = new HandleView(getContext());
        mChan2Handle.setHandleId(ID_HANDLE_2);
        mChan2Handle.setAttributes(Color.BLUE, "CH2", HandleView.HandleDirection.RIGHT);
        mChan2Handle.setOnDoCommand(mHandleOnDataChanged);
        addView(mChan2Handle);

        mTimeHandle = new HandleView(getContext());
        mTimeHandle.setHandleId(ID_HANDLE_TIME);
        mTimeHandle.setAttributes(Color.rgb(255,215,0), "T", HandleView.HandleDirection.DOWN);
        mTimeHandle.setOnDoCommand(mHandleOnDataChanged);
        addView(mTimeHandle);

        mTrigHandle = new HandleView(getContext());
        mTrigHandle.setHandleId(ID_HANDLE_TRIG);
        mTrigHandle.setAttributes(Color.rgb(255,215,0), "T", HandleView.HandleDirection.LEFT);
        mTrigHandle.setOnDoCommand(mHandleOnDataChanged);
        addView(mTrigHandle);
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

        int cursorLength = mChan1Handle.getHandleLength();
        int cursorBreadth = mTimeHandle.getHandleBreadth();

        View buttonRow = findViewById(R.id.button_row);
        int buttonHeight = buttonRow.getMeasuredHeight();

        int cursorBottom = bottomPos - buttonHeight + cursorBreadth / 2;

     //   int scopeWidth = rightPos - leftPos - 2 * cursorWidth;
     //   int scopeHeight = bottomPos - topPos - cursorHeight;

        mChan1Handle.layout(
                leftPos,
                topPos + cursorLength - cursorBreadth / 2,
                leftPos + cursorLength,
                cursorBottom + 2);
        mChan2Handle.layout(
                leftPos,
                topPos + cursorLength - cursorBreadth / 2,
                leftPos + cursorLength,
                cursorBottom + 2);
        mTimeHandle.layout(
                leftPos + cursorLength - cursorBreadth / 2,
                topPos,
                rightPos - cursorLength + cursorBreadth / 2 + 2,
                topPos + cursorLength);
        mTrigHandle.layout(
                rightPos - cursorLength,
                topPos + cursorLength - cursorBreadth / 2,
                rightPos,
                cursorBottom + 2);

        mScopeView = (ScopeView)findViewById(R.id.scopeView);
        mScopeView.layout(leftPos + cursorLength, topPos + cursorLength,rightPos - cursorLength, bottomPos - buttonHeight);
        mScopeView.setOnDoCommand(new OnDataChanged()
        {
            @Override
            public void doCommand(ScopeInterface.Command command, int channel, Object specialData)
            {
                if(mOnDataChanged != null)
                    mOnDataChanged.doCommand(command, channel, specialData);
            }

            @Override
            public void moveWave(int channel, float pos, boolean moving)
            {
                if(channel == 1)
                    mChan1Handle.setHandlePosition(pos + mChan1Handle.getHandleBreadth() / 2);
                else if(channel == 2)
                    mChan2Handle.setHandlePosition(pos + mChan2Handle.getHandleBreadth() / 2);
            }

            @Override
            public void moveTime(float pos, boolean moving)
            {
                mTimeHandle.setHandlePosition(pos + mTimeHandle.getHandleBreadth() / 2);
            }
        });

        buttonRow.layout(leftPos + cursorLength, bottomPos - buttonHeight, rightPos - cursorLength, bottomPos);

        post(new ExpandScopeViewArea());
    }

    private OnDataChanged mHandleOnDataChanged = new OnDataChanged()
    {
        @Override
        public void doCommand(ScopeInterface.Command command, int channel, Object specialData)
        {
        }

        @Override
        public void moveWave(int channel, float pos, boolean moving)
        {
            mScopeView.setInMovement(moving);
            mScopeView.moveWave(channel, pos, !moving);
        }

        @Override
        public void moveTime(float pos, boolean moving)
        {
            mScopeView.setInMovement(moving);
            mScopeView.moveTime(pos, !moving);
        }
    };

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