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
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import de.uni_weimar.mheinz.androidtouchscope.R;
import de.uni_weimar.mheinz.androidtouchscope.display.handler.OnDataChangedInterface;
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
    private MeasurementsView mMeasurementsView;
    private LearningView mLearningView;
    private View mMovableView;

    static final int ID_HANDLE_1 = 1;
    static final int ID_HANDLE_2 = 2;
    static final int ID_HANDLE_TIME = 3;
    static final int ID_HANDLE_TRIG = 4;

    static final int CHAN1_COLOR = Color.YELLOW;
    static final int CHAN2_COLOR = Color.CYAN;
    static final int TRIGGER_COLOR = Color.rgb(255,215,0);

    private OnDataChangedInterface.OnDataChanged mOnDataChanged = null;


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

    public void setOnDoCommand(OnDataChangedInterface.OnDataChanged onDataChanged)
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
            mChan1Handle.setWaveData(waveData);
        }
        if(mChan2Handle != null && channel == 2)
        {
            boolean off = waveData == null || waveData.data == null || waveData.data.length == 0;
            mChan2Handle.setIsOn(!off);
            mChan2Handle.setWaveData(waveData);
        }
        if(mTrigHandle != null)
            mTrigHandle.setTriggerData(trigData);
    }

    private void init()
    {
        mChan2Handle = new HandleView(getContext());
        mChan2Handle.setHandleId(ID_HANDLE_2);
        mChan2Handle.setAttributes(CHAN2_COLOR, "CH2", HandleView.HandleDirection.RIGHT);
        mChan2Handle.setOnDoCommand(mHandleOnDataChanged);
        addView(mChan2Handle);

        mChan1Handle = new HandleView(getContext());
        mChan1Handle.setHandleId(ID_HANDLE_1);
        mChan1Handle.setAttributes(CHAN1_COLOR, "CH1", HandleView.HandleDirection.RIGHT);
        mChan1Handle.setOnDoCommand(mHandleOnDataChanged);
        addView(mChan1Handle);

        mTimeHandle = new HandleView(getContext());
        mTimeHandle.setHandleId(ID_HANDLE_TIME);
        mTimeHandle.setAttributes(TRIGGER_COLOR, "T", HandleView.HandleDirection.DOWN);
        mTimeHandle.setOnDoCommand(mHandleOnDataChanged);
        addView(mTimeHandle);

        mTrigHandle = new HandleView(getContext());
        mTrigHandle.setHandleId(ID_HANDLE_TRIG);
        mTrigHandle.setAttributes(TRIGGER_COLOR, "Trig", HandleView.HandleDirection.LEFT);
        mTrigHandle.setOnDoCommand(mHandleOnDataChanged);
        addView(mTrigHandle);

        mMeasurementsView = new MeasurementsView(getContext());
        mMeasurementsView.setVisibility(GONE);
        addView(mMeasurementsView);

      //  mLearningView = new LearningView(getContext());
        //mLearningView.setVisibility(GONE);
      //  addView(mLearningView);

        mMovableView = new View(getContext());
        mMovableView.setVisibility(INVISIBLE);
        addView(mMovableView);
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
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(w, h, oldWidth, oldHeight);


        // These are the far left and right edges in which we are performing layout.
        int leftPos = getPaddingLeft();
        int rightPos = w - leftPos - getPaddingRight();

        // These are the top and bottom edges in which we are performing layout.
        int topPos = getPaddingTop();
        final int bottomPos =  h - topPos - getPaddingBottom();

        int cursorLength = mChan1Handle.getHandleLength();
        int cursorBreadth = mTimeHandle.getHandleBreadth();

        View buttonRow = findViewById(R.id.button_row);
        int buttonHeight = buttonRow.getMeasuredHeight();


        mLearningView = (LearningView)findViewById(R.id.learningView);
        if(mLearningView.getVisibility() == VISIBLE)
        {
            if(w > h)
            {
                int width = (rightPos - leftPos) / 3;
                if(mLearningView.getGravity() == Gravity.END)
                {
                    mLearningView.layout(rightPos - width, topPos + cursorLength, rightPos, bottomPos - buttonHeight);
                    rightPos -= width;
                }
                else
                {
                    mLearningView.layout(leftPos, topPos + cursorLength, leftPos + width, bottomPos - buttonHeight);
                    leftPos += width;
                }

            }
            else
            {
                int height = (bottomPos - topPos) / 3;
                mLearningView.layout(leftPos, topPos, rightPos, topPos + height);
                topPos += height;
            }
        }

        buttonRow.layout(leftPos + cursorLength, bottomPos - buttonHeight, rightPos, bottomPos);
        int cursorBottom = bottomPos - buttonHeight + cursorBreadth / 2;

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
        mScopeView.setOnDoCommand(new OnDataChangedInterface.OnDataChanged()
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

            @Override
            public void moveTrigger(float pos, boolean moving)
            {
                mTrigHandle.setHandlePosition(pos + mChan2Handle.getHandleBreadth() / 2);
            }

            @Override
            public void doAnimation(LearningView.Controls controls)
            {
                mLearningView.doAnim(controls);
            }
        });


        mMeasurementsView.layout(leftPos + cursorLength, topPos + cursorLength,rightPos - cursorLength, bottomPos - buttonHeight);
        mMeasurementsView.bringToFront();

        mMovableView.layout(0,0,10,10);

        //post(new ExpandScopeViewArea());
    }

    public View getMovableView()
    {
        return mMovableView;
    }

    public MeasurementsView getMeasureView()
    {
        return mMeasurementsView;
    }

    private final OnDataChangedInterface.OnDataChanged mHandleOnDataChanged
            = new OnDataChangedInterface.OnDataChanged()
    {
        @Override
        public void doCommand(ScopeInterface.Command command, int channel, Object specialData)
        {
            if(command == ScopeInterface.Command.SET_ACTIVE_CHANNEL)
            {
                mScopeView.setSelectedPath(channel);
            }
            else if(mOnDataChanged != null)
                mOnDataChanged.doCommand(command, channel, specialData);
        }

        @Override
        public void moveWave(int channel, float pos, boolean moving)
        {
            mScopeView.setInMovement(moving);
            mScopeView.moveWave(channel, pos, !moving);
            mLearningView.doAnim(LearningView.Controls.VERT_POS_KNOB);
        }

        @Override
        public void moveTime(float pos, boolean moving)
        {
            mScopeView.setInMovement(moving);
            mScopeView.moveTime(pos, !moving);
            mLearningView.doAnim(LearningView.Controls.HORZ_POS_KNOB);
        }

        @Override
        public void moveTrigger(float pos, boolean moving)
        {
            mScopeView.setInMovement(moving);
            mScopeView.moveTrigger(pos, !moving);
        }

        @Override
        public void doAnimation(LearningView.Controls controls)
        {
            mLearningView.doAnim(controls);
        }
    };

 /*   private class ExpandScopeViewArea implements Runnable
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
    }*/
}