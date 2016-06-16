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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import de.uni_weimar.mheinz.androidtouchscope.R;
import de.uni_weimar.mheinz.androidtouchscope.display.handler.OnDataChangedInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.ScopeInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

//TODO: when offset is off screen, moving handel should offset data back to screen
public class HandleView extends View implements HandlePopup.HandlePopupListener
{
    private static final String TAG = "ScopeView";

    private static final int HANDLE_LENGTH = 55;
    private static final int HANDLE_BREADTH = 25;

    private final ShapeDrawable mShapeDrawable = new ShapeDrawable();
    private final Path mHandlePath = new Path();
    private final Paint mMainTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable mPressDrawable = null;

    private GestureDetectorCompat mGestureDetector;
    private OnDataChangedInterface.OnDataChanged mOnDataChanged = null;

    private int mId = 0;
    private int mColor = Color.BLUE;
    private String mMainText = "";
    private boolean mIsOn = true;
    private WaveData mWaveData = null;
    private TriggerData mTrigData = null;

    HandlePopup mPopupWindow;

    private HandleDirection mOrientation = HandleDirection.RIGHT;
    private RectF mBounds = new RectF(0, 0, HANDLE_LENGTH, HANDLE_BREADTH);
    private final Rect mTextBounds = new Rect();

    private float mHandlePos = HANDLE_BREADTH / 2;
    private float mOldHandlePos = 0;

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

        if(mId == HostView.ID_HANDLE_TRIG || mId == HostView.ID_HANDLE_1 || mId == HostView.ID_HANDLE_2)
            mPressDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_chevron_right_black);
        else
            mPressDrawable = null;
    }

    public void setOnDoCommand(OnDataChangedInterface.OnDataChanged onDataChanged)
    {
        mOnDataChanged = onDataChanged;
    }

    public void setHandlePosition(float pos)
    {
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

    public void setWaveData(WaveData waveData)
    {
        mWaveData = waveData;
    }

    public void setTriggerData(TriggerData triggerData)
    {
        mTrigData = triggerData;
        if(mTrigData != null)
        {
            if(mTrigData.source == TriggerData.TriggerSrc.CHAN1)
                mColor = HostView.CHAN1_COLOR;
            else if(mTrigData.source == TriggerData.TriggerSrc.CHAN2)
                mColor = HostView.CHAN2_COLOR;
            else
                mColor = HostView.TRIGGER_COLOR;
        }
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
            /*mShapeDrawable.getPaint().setStyle(Paint.Style.FILL);
            mShapeDrawable.getPaint().setColor(Color.WHITE);
            mShapeDrawable.getPaint().setShadowLayer(2,2,2,Color.GRAY);
            mShapeDrawable.draw(canvas);
            mShapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
            mShapeDrawable.getPaint().setColor(Color.BLACK);
            mShapeDrawable.draw(canvas);*/
            mShapeDrawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            mShapeDrawable.getPaint().setColor(Color.LTGRAY);
            mShapeDrawable.getPaint().setShadowLayer(2,2,2,Color.GRAY);
            mShapeDrawable.draw(canvas);
        }

        PointF center = getCircleCenter();
        mMainTextPaint.getTextBounds(mMainText, 0, mMainText.length(), mTextBounds);
        if(mOrientation == HandleDirection.RIGHT)
            canvas.drawText(mMainText, center.x + 5, center.y + mTextBounds.height() / 2 - 1, mMainTextPaint);
        else if(mOrientation == HandleDirection.LEFT)
            canvas.drawText(mMainText, center.x - 5, center.y + mTextBounds.height() / 2 - 2, mMainTextPaint);
        else
            canvas.drawText(mMainText, center.x, center.y + mTextBounds.height() / 2, mMainTextPaint);

        if(mPressDrawable != null)
        {
            if(mOrientation == HandleDirection.RIGHT)
            {
                mPressDrawable.setBounds(
                        2, (int)(center.y - HANDLE_BREADTH / 2),
                        HANDLE_BREADTH - 3, (int)center.y + HANDLE_BREADTH / 2);
                mPressDrawable.draw(canvas);
            }
            else if(mOrientation == HandleDirection.LEFT)
            {
                canvas.save();
                canvas.rotate(180, HANDLE_LENGTH / 2, mHandlePos);
                mPressDrawable.setBounds(
                        1, (int)(center.y - HANDLE_BREADTH / 2),
                        HANDLE_BREADTH - 5, (int)center.y + HANDLE_BREADTH / 2);
                mPressDrawable.draw(canvas);
                canvas.restore();
            }
        }
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

            if(mId == HostView.ID_HANDLE_1)
                mHandlePos -= 50;
            else if(mId == HostView.ID_HANDLE_2)
                mHandlePos += 50;
        }
        makeHandle();
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
        if(x >= point.x - HANDLE_BREADTH && x <= point.x + HANDLE_BREADTH &&
           y >= point.y - HANDLE_BREADTH && y <= point.y + HANDLE_BREADTH)
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
            boolean hit = touchSelectCursor(event);
            if(hit)
            {
                int[] location = new int[2];
                getLocationOnScreen(location);

                mPopupWindow = new HandlePopup(getContext());
                mPopupWindow.setHandleListener(HandleView.this);

                if(mId == HostView.ID_HANDLE_1)
                {
                    mPopupWindow.setPopupType(HandlePopup.CHANNEL_POPUP, mWaveData);
                    location[1] = (int)mHandlePos + getTop() - mPopupWindow.getApproxHeight();
                    location[0] += HANDLE_LENGTH;

                    mOnDataChanged.doAnimation(LearningView.Controls.CH1_BUTTON);
                    mOnDataChanged.doCommand(ScopeInterface.Command.SET_ACTIVE_CHANNEL, 1, null);
                }
                else if(mId == HostView.ID_HANDLE_2)
                {
                    mPopupWindow.setPopupType(HandlePopup.CHANNEL_POPUP, mWaveData);
                    location[1] = (int)mHandlePos + getTop() - mPopupWindow.getApproxHeight();
                    location[0] += HANDLE_LENGTH;

                    mOnDataChanged.doAnimation(LearningView.Controls.CH2_BUTTON);
                    mOnDataChanged.doCommand(ScopeInterface.Command.SET_ACTIVE_CHANNEL, 2, null);
                }
                else if(mId == HostView.ID_HANDLE_TRIG)
                {
                    mPopupWindow.setPopupType(HandlePopup.TRIGGER_POPUP, mTrigData);
                    location[1] = (int)mHandlePos + getTop() - mPopupWindow.getApproxHeight();
                    location[0] -= mPopupWindow.getApproxWidth();

                    mOnDataChanged.doAnimation(LearningView.Controls.TRIGGER_MENU_BUTTON);
                }

                mPopupWindow.showAtLocation(getRootView(), Gravity.NO_GRAVITY, location[0], location[1]);
            }

            return hit;
        }
    }

    private PopupMenu createPopupMenu(View view, int menuId)
    {
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        View moveView = ((HostView)getParent()).getMovableView();
        moveView.layout(pos[0], pos[1], pos[0] + 10, pos[1] + 10);

        PopupMenu popup = new PopupMenu(getContext(), moveView);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(menuId, popup.getMenu());
        return popup;
    }

    @Override
    public void onChannelVisible(View view)
    {
        Log.i(TAG, "onChannelVisible");

        if(mOnDataChanged != null)
        {
            mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_STATE, mId, !mIsOn);
            if(mIsOn)
                mOnDataChanged.doAnimation(LearningView.Controls.OFF_BUTTON);
            else
            {
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_ACTIVE_CHANNEL, mId, null);
                    }
                }, 1000);
            }
        }

        if(mIsOn)
        {
            ImageView imageView = (ImageView)view.findViewById(R.id.channel_visible_subImage);
            imageView.setImageResource(R.drawable.hidden_channel);
        }
        else
        {
            ImageView imageView = (ImageView) view.findViewById(R.id.channel_visible_subImage);
            imageView.setImageResource(R.drawable.visible_channel);
        }
    }

    @Override
    public void onChannelProbe(final View view)
    {
        Log.i(TAG, "onChannelProbe");

        PopupMenu popup = createPopupMenu(view, R.menu.probe_menu);
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                boolean handled = false;
                String probe = "";
                switch (item.getItemId())
                {
                    case R.id.menu_probe_1:
                        handled = true;
                        probe = getResources().getString(R.string.probe_1X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 1);
                        break;
                    case R.id.menu_probe_5:
                        handled = true;
                        probe = getResources().getString(R.string.probe_5X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 5);
                        break;
                    case R.id.menu_probe_10:
                        handled = true;
                        probe = getResources().getString(R.string.probe_10X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 10);
                        break;
                    case R.id.menu_probe_50:
                        handled = true;
                        probe = getResources().getString(R.string.probe_50X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 50);
                        break;
                    case R.id.menu_probe_100:
                        handled = true;
                        probe = getResources().getString(R.string.probe_100X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 100);
                        break;
                    case R.id.menu_probe_500:
                        handled = true;
                        probe = getResources().getString(R.string.probe_500X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 500);
                        break;
                    case R.id.menu_probe_1000:
                        handled = true;
                        probe = getResources().getString(R.string.probe_1000X);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_PROBE, mId, 1000);
                        break;
                }
                ((TextView)view.findViewById(R.id.channel_probe_subtext)).setText(probe);
                //  mPopupWindow.dismiss();
                return handled;
            }
        });
    }

    @Override
    public void onChannelCoupling(final View view)
    {
        Log.i(TAG, "onChannelCoupling");

        PopupMenu popup = createPopupMenu(view, R.menu.coupling_menu);
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                boolean handled = false;
                String coup = "";
                switch (item.getItemId())
                {
                    case R.id.menu_coupling_ac:
                        handled = true;
                        coup = getResources().getString(R.string.coupling_ac);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_COUPLING, mId, "AC");

                        break;
                    case R.id.menu_coupling_dc:
                        handled = true;
                        coup = getResources().getString(R.string.coupling_dc);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_COUPLING, mId, "DC");
                        break;
                    case R.id.menu_coupling_gnd:
                        handled = true;
                        coup = getResources().getString(R.string.coupling_gnd);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_CHANNEL_COUPLING, mId, "GND");
                        break;
                }
                ((TextView)view.findViewById(R.id.channel_coupling_subtext)).setText(coup);
                //  mPopupWindow.dismiss();
                return handled;
            }
        });
    }

    @Override
    public void onTriggerSource(final View view)
    {
        Log.i(TAG, "onTriggerSource");

        PopupMenu popup = createPopupMenu(view, R.menu.trigger_source_menu);
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                boolean handled = false;
                String ch = "";
                switch (item.getItemId())
                {
                    case R.id.menu_source_ch1:
                        handled = true;
                        ch = getResources().getString(R.string.source_ch1);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_TRIGGER_SOURCE, 0, "CHAN1");
                        break;
                    case R.id.menu_source_ch2:
                        handled = true;
                        ch = getResources().getString(R.string.source_ch2);
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_TRIGGER_SOURCE, 0, "CHAN2");
                        break;
                }
                ((TextView)view.findViewById(R.id.trigger_source_subtext)).setText(ch);
                //  mPopupWindow.dismiss();
                return handled;
            }
        });
    }

    @Override
    public void onTriggerSlope(final View view)
    {
        Log.i(TAG, "onTriggerSlope");

        PopupMenu popup = createPopupMenu(view, R.menu.trigger_slope_menu);
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                boolean handled = false;
                switch (item.getItemId())
                {
                    case R.id.menu_slope_positive:
                    {
                        handled = true;
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_TRIGGER_SLOPE, 0, "POS");

                        ImageView imageView = (ImageView) view.findViewById(R.id.trigger_slope_subImage);
                        imageView.setImageResource(R.drawable.positive_slope);

                        break;
                    }
                    case R.id.menu_slope_negative:
                    {
                        handled = true;
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_TRIGGER_SLOPE, 0, "NEG");

                        ImageView imageView = (ImageView) view.findViewById(R.id.trigger_slope_subImage);
                        imageView.setImageResource(R.drawable.negative_slope);

                        break;
                    }
                    case R.id.menu_slope_both:
                    {
                        handled = true;
                        mOnDataChanged.doCommand(ScopeInterface.Command.SET_TRIGGER_SLOPE, 0, "ALT");

                        ImageView imageView = (ImageView) view.findViewById(R.id.trigger_slope_subImage);
                        imageView.setImageResource(R.drawable.both_slope);

                        break;
                    }
                }
                //  mPopupWindow.dismiss();
                return handled;
            }
        });

    }

    @Override
    public void onTrigger50(View view)
    {
        Log.i(TAG, "onTrigger50");

        if(mOnDataChanged != null)
        {
            mOnDataChanged.doCommand(ScopeInterface.Command.DO_TRIG_50, 0, 0);
            mOnDataChanged.doAnimation(LearningView.Controls.TRIGGER_50_BUTTON);
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
