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
    private final int SCOPE_WIDTH = 600;

    private int mChannel1Color = Color.YELLOW;
    private int mChannel2Color = Color.BLUE;
    private int mChannelMathColor = Color.MAGENTA;

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

        initDrawable(mDrawableChan1, mPathChan1, mChannel1Color, SCOPE_WIDTH, mContentHeight);
        initDrawable(mDrawableChan2, mPathChan2, mChannel2Color, SCOPE_WIDTH, mContentHeight);
        initDrawable(mDrawableMath, mPathMath, mChannelMathColor, SCOPE_WIDTH, mContentHeight);
    }

    private void initDrawable(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.moveTo(0,0);
        drawable.setShape(new PathShape(path,width,height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.setBounds(0, 0, width, height);
    }

    public void setChannelData(String channel, byte[] data)
    {
        switch(channel)
        {
            case RigolScope.CHAN_1:
                updatePath(mPathChan1, data);
                break;
            case RigolScope.CHAN_2:
                updatePath(mPathChan2, data);
                break;
            case RigolScope.CHAN_MATH:
                updatePath(mPathMath, data);
                break;
        }
        postInvalidate();
    }

    private void updatePath(Path path, byte[] data)
    {
        path.rewind();
        if(data == null || data.length == 0)
            return;

        int mid = mContentHeight / 2;

        path.moveTo(0, data[0]);

        for(int i = 1; i < data.length; ++i)
        {
            path.lineTo(i, data[i] + mid);
        }
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
