package de.uni_weimar.mheinz.androidtouchscope.display;


import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import de.uni_weimar.mheinz.androidtouchscope.R;

public class HandleDialog extends DialogFragment
{
    public static final String BUTTON_MASK      = "BUTTON_MASK";
    public static final int CHANNEL_VISIBLE     = 0x0001;
    public static final int CHANNEL_COUPLING    = 0x0002;
    public static final int CHANNEL_PROBE       = 0x0004;
    public static final int TRIGGER_SOURCE      = 0x0010;
    public static final int TRIGGER_SLOPE       = 0x0020;
    public static final int TRIGGER_50          = 0x0040;

    private int mButtonMask = 0;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        mButtonMask = getArguments().getInt(BUTTON_MASK);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.handle_dialog_layout, null);

        if((mButtonMask & CHANNEL_VISIBLE) == CHANNEL_VISIBLE)
        {

        }
        if((mButtonMask & CHANNEL_COUPLING) == CHANNEL_COUPLING)
        {

        }
        if((mButtonMask & CHANNEL_PROBE) == CHANNEL_PROBE)
        {

        }
        if((mButtonMask & TRIGGER_SOURCE) == TRIGGER_SOURCE)
        {

        }
        if((mButtonMask & TRIGGER_SLOPE) == TRIGGER_SLOPE)
        {

        }
        if((mButtonMask & TRIGGER_50) == TRIGGER_50)
        {

        }

        builder.setView(layout);



        return builder.create();
    }
}
