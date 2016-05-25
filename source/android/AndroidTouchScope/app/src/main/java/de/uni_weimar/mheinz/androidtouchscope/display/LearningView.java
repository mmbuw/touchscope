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
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
//import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.uni_weimar.mheinz.androidtouchscope.R;

public class LearningView extends ImageView
{
    private Drawable mDrawable;
   // private int mHeight = 0;
   // private int mWidth = 0;

    public LearningView(Context context)
    {
        super(context);
    //    init();
    }

    public LearningView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
  //      init();
    }

    public LearningView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
  //      init();
    }

    private void init()
    {
      //  mDrawable = ContextCompat.getDrawable(getContext(), R.drawable.dial_avd);
        setAdjustViewBounds(true);
        setScaleType(ScaleType.CENTER_INSIDE);
        setBackgroundResource(R.drawable.dial_avd);
      //  getLayoutParams().l

    }

    boolean running = false;
    public void doAnim()
    {
        if(!running)
        {
      //      AnimatedVectorDrawableCompat animatedVector = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.dial_avd);
      //      Drawable drawable = getResources().getDrawable(R.drawable.dial_avd, null);
            AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)getBackground();
            drawable.start();
        /*    if (mDrawable instanceof Animatable)
            {
                ((Animatable) mDrawable).start();
                invalidate();
            }*/
           // animatedVector.start();
            running = true;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
     //   mHeight = height;
     //   mWidth = width;
        int imageHeight = 480 * width / 550;
        int margin = (height - imageHeight) / 2;
        setTop(margin);
        setBottom(imageHeight + margin);
       // super.onSizeChanged(width, imageHeight, oldWidth, oldHeight);
    }

   /* @Override
    protected void onDraw(Canvas canvas)
    {
        int imageHeight = 480 * mWidth / 550;
        int margin = (mHeight - imageHeight) / 2;
        mDrawable.setBounds(0, margin, mWidth, imageHeight + margin);
        mDrawable.draw(canvas);
    }*/
}
