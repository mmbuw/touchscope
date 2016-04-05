package de.uni_weimar.mheinz.androidtouchscope;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class ScopeView extends View
{
    private static final int SCOPE_WIDTH = 600;

    private ShapeDrawable mDrawableChan1 = new ShapeDrawable();
    private ShapeDrawable mDrawableChan2 = new ShapeDrawable();
    private ShapeDrawable mDrawableMath = new ShapeDrawable();

    private Path mPathChan1 = new Path();
    private Path mPathChan2 = new Path();
    private Path mPathMath = new Path();

    private int mContentWidth = 0;
    private int mContentHeight = 0;

    public ScopeView(Context context)
    {
        super(context);
        init(null, 0);
    }

    public ScopeView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs, 0);
    }

    public ScopeView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle)
    {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mContentWidth = getWidth() - paddingLeft - paddingRight;
        mContentHeight = getHeight() - paddingTop - paddingBottom;

        initDrawable(mDrawableChan1, mPathChan1, Color.YELLOW, mContentWidth, mContentHeight);
        initDrawable(mDrawableChan2, mPathChan2, Color.BLUE, mContentWidth, mContentHeight);
        initDrawable(mDrawableMath, mPathMath, Color.MAGENTA, mContentWidth, mContentHeight);
    }

    private void initDrawable(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.moveTo(0,0);
        drawable.setShape(new PathShape(path,width,height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.setBounds(0, 0, width, height);
    }

    public void setChannelData(int channel, WaveData waveData)
    {
        switch(channel)
        {
            case 1:
                updatePath(mPathChan1, waveData);
                break;
            case 2:
                updatePath(mPathChan2, waveData);
                break;
            case 3:
                updatePath(mPathMath, waveData);
                break;
        }
        postInvalidate();
    }

    private void updatePath(Path path, WaveData waveData)
    {
        path.rewind();
        if(waveData == null || waveData.data == null || waveData.data.length == 0)
            return;

        float vScale = (float)waveData.voltageScale;
        if(vScale == 0)
            vScale = 1.0f;

        float widthRatio = ((float)mContentWidth) / (waveData.data.length - 11);
        float heightRatio = (mContentHeight / 32.0f) / vScale;
        float mid = (mContentHeight / 2.0f);// - (float)waveData.voltageOffset * heightRatio;

        path.moveTo(0, waveData.data[11]);
        for(int i = 12, j = 1; i < waveData.data.length; ++i, ++j)
        {
            float point = actualVoltage((float)waveData.voltageOffset, vScale, waveData.data[i]);
            point = (float)(point + waveData.voltageOffset) * heightRatio + mid;
            if(point < 0)
                point = 0;
            else if(point > mContentHeight)
                point = mContentHeight;

            path.lineTo(j * widthRatio, point);
        }
    }

    private float actualVoltage(float offset, float scale, byte point)
    {
        // Walk through the data, and map it to actual voltages
        // This mapping is from Cibo Mahto
        // First invert the data
        double tPoint = point * -1 + 255;

        // Now, we know from experimentation that the scope display range is actually
        // 30-229.  So shift by 130 - the voltage offset in counts, then scale to
        // get the actual voltage.

        tPoint = (tPoint - 130.0 - (offset/ scale * 25)) / 25 * scale;
        return (float)tPoint;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        mDrawableChan1.draw(canvas);
        mDrawableChan2.draw(canvas);
        mDrawableMath.draw(canvas);

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        init(null,0);
    }
}
