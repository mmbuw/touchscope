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
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatImageView;

import de.uni_weimar.mheinz.androidtouchscope.R;


public class LearningView extends AppCompatImageView
{
    public enum Controls
    {
        DIAL_KNOB,
        VERT_POS_KNOB,
        VERT_SCALE_KNOB,
        HORZ_POS_KNOB,
        HORZ_SCALE_KNOB,
        TRIGGER_KNOB
    }
    public LearningView(Context context)
    {
        super(context);
    }

    public LearningView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public LearningView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void doAnim(Controls control)
    {
        switch(control)
        {
            case DIAL_KNOB:
                setImageResource(R.drawable.dial_avd);
                break;
            case VERT_POS_KNOB:
                setImageResource(R.drawable.vert_pos_avd);
                break;
        }

        AnimatedVectorDrawableCompat drawable = (AnimatedVectorDrawableCompat)getDrawable();
        drawable.start();
    }
}
