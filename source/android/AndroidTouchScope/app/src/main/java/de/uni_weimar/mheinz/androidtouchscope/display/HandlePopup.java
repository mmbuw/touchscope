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
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import de.uni_weimar.mheinz.androidtouchscope.R;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

public class HandlePopup extends PopupWindow
{
 /*   public static final int CHANNEL_VISIBLE     = 0x0001;
    public static final int CHANNEL_COUPLING    = 0x0002;
    public static final int CHANNEL_PROBE       = 0x0004;
    public static final int TRIGGER_SOURCE      = 0x0010;
    public static final int TRIGGER_SLOPE       = 0x0020;
    public static final int TRIGGER_50          = 0x0040;*/

    public static final int CHANNEL_POPUP = 1;
    public static final int TRIGGER_POPUP = 2;

    private final Context mContext;
    private HandlePopupListener mListener;
    private int mApproxWidth = 0;

    public HandlePopup(Context context)
    {
        super(context);
        mContext = context;
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y)
    {
        super.showAtLocation(parent, gravity, x, y + 100);
    }

    public void setPopupType(int popupType, Object data)
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.handle_dialog_layout, null);

        float buttonWidth = mContext.getResources().getDimension(R.dimen.handle_dialog_button_width);

        if(popupType == CHANNEL_POPUP)
        {
            WaveData waveData = (WaveData)data;
            boolean hidden = waveData == null || waveData.data == null || waveData.data.length == 0;

            // Visible button
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.channel_visible);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onChannelVisible(v);
                //    dismiss();
                }
            });
            mApproxWidth += buttonWidth;

            // Coupling button
            view = (LinearLayout)layout.findViewById(R.id.channel_coupling);
            view.setVisibility(View.VISIBLE);
            // view.setVisibility(hidden ? View.GONE : View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onChannelCoupling(v);
                }
            });
          //  if(!hidden)
                mApproxWidth += buttonWidth;

            // Probe button
            view = (LinearLayout)layout.findViewById(R.id.channel_probe);
            view.setVisibility(View.VISIBLE);
            // view.setVisibility(hidden ? View.GONE : View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onChannelProbe(v);
               //     dismiss();
                }
            });
         //   if(!hidden)
                mApproxWidth += buttonWidth;

            if(hidden)
            {
                ImageView imageView = (ImageView)layout.findViewById(R.id.channel_visible_subImage);
                imageView.setImageResource(R.drawable.hidden_channel);
            }
            else
            {
                ImageView imageView = (ImageView) layout.findViewById(R.id.channel_visible_subImage);
                imageView.setImageResource(R.drawable.visible_channel);
            }

            if(waveData != null)
            {
                ((TextView) layout.findViewById(R.id.channel_coupling_subtext)).setText(waveData.coupling);

                String probe = waveData.probe + "X";
                ((TextView) layout.findViewById(R.id.channel_probe_subtext)).setText(probe);
            }
        }
        else if(popupType == TRIGGER_POPUP)
        {
            TriggerData trigData = (TriggerData)data;

            // Source button
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.trigger_source);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onTriggerSource(v);
                //    dismiss();
                }
            });
            mApproxWidth += buttonWidth;

            // slope button
            view = (LinearLayout)layout.findViewById(R.id.trigger_slope);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onTriggerSlope(v);
                 //   dismiss();
                }
            });
            mApproxWidth += buttonWidth;

            // 50 button
            Button button = (Button)layout.findViewById(R.id.trigger_50);
            button.setVisibility(View.VISIBLE);
            button.setClickable(true);
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onTrigger50(v);
                    dismiss();
                }
            });
            mApproxWidth += buttonWidth;

            if(trigData != null)
            {
                ((TextView)layout.findViewById(R.id.trigger_source_subtext)).setText(trigData.source.toString());

                ImageView imageView = (ImageView)layout.findViewById(R.id.trigger_slope_subImage);
                if(trigData.edge == TriggerData.TriggerEdge.POSITIVE)
                    imageView.setImageResource(R.drawable.positive_slope);
                else if(trigData.edge == TriggerData.TriggerEdge.NEGATIVE)
                    imageView.setImageResource(R.drawable.negative_slope);
                else
                    imageView.setImageResource(R.drawable.both_slope);
            }
        }


        setContentView(layout);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        // Closes the popup window when touch outside of it - when looses focus
        setOutsideTouchable(true);
        setFocusable(true);

    }

    public int getApproxWidth()
    {
        return mApproxWidth;
    }

    public int getApproxHeight()
    {
        return (int)mContext.getResources().getDimension(R.dimen.handle_dialog_button_height);
    }

    public void setHandleListener(HandlePopupListener listener)
    {
        mListener = listener;
    }

    public interface HandlePopupListener
    {
        void onChannelVisible(View view);
        void onChannelProbe(View view);
        void onChannelCoupling(View view);
        void onTriggerSource(View view);
        void onTriggerSlope(View view);
        void onTrigger50(View view);
    }
}
