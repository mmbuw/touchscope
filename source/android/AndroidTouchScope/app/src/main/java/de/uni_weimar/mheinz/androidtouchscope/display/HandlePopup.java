package de.uni_weimar.mheinz.androidtouchscope.display;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import de.uni_weimar.mheinz.androidtouchscope.R;

public class HandlePopup extends PopupWindow
{
    public static final int CHANNEL_VISIBLE     = 0x0001;
    public static final int CHANNEL_COUPLING    = 0x0002;
    public static final int CHANNEL_PROBE       = 0x0004;
    public static final int TRIGGER_SOURCE      = 0x0010;
    public static final int TRIGGER_SLOPE       = 0x0020;
    public static final int TRIGGER_50          = 0x0040;

    private Context mContext;
    private HandlePopupListener mListener;
    private int mAproxWidth = 0;

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

    public void setButtonMask(int buttonMask)
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.handle_dialog_layout, null);

        float buttonWidth = mContext.getResources().getDimension(R.dimen.handle_dialog_button_width);

        if((buttonMask & CHANNEL_VISIBLE) == CHANNEL_VISIBLE)
        {
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.channel_visible);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onChannelVisible(v);
                    dismiss();
                }
            });
            mAproxWidth += buttonWidth;
        }
        if((buttonMask & CHANNEL_COUPLING) == CHANNEL_COUPLING)
        {
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.channel_coupling);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onChannelCoupling(v);
                    dismiss();
                }
            });
            mAproxWidth += buttonWidth;
        }
        if((buttonMask & CHANNEL_PROBE) == CHANNEL_PROBE)
        {
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.channel_probe);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onChannelProbe(v);
                    dismiss();
                }
            });
            mAproxWidth += buttonWidth;
        }
        if((buttonMask & TRIGGER_SOURCE) == TRIGGER_SOURCE)
        {
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.trigger_source);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onTriggerSource(v);
                    dismiss();
                }
            });
            mAproxWidth += buttonWidth;
        }
        if((buttonMask & TRIGGER_SLOPE) == TRIGGER_SLOPE)
        {
            LinearLayout view = (LinearLayout)layout.findViewById(R.id.trigger_slope);
            view.setVisibility(View.VISIBLE);
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mListener.onTriggerSlope(v);
                    dismiss();
                }
            });
            mAproxWidth += buttonWidth;
        }
        if((buttonMask & TRIGGER_50) == TRIGGER_50)
        {
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
            mAproxWidth += buttonWidth;
        }


        setContentView(layout);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        // Closes the popup window when touch outside of it - when looses focus
        setOutsideTouchable(true);
        setFocusable(true);

    }

    public int getAproxWidth()
    {
        return mAproxWidth;
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
