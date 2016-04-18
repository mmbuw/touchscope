package de.uni_weimar.mheinz.androidtouchscope.not_used;


import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;

public class PathPoints extends Path
{
    private final ArrayList<PointF> mPointList = new ArrayList<>();

    public PointF[] getPoints()
    {
        synchronized(mPointList)
        {
            return mPointList.toArray(new PointF[]{});
        }
    }

    @Override
    public void reset()
    {
        synchronized(mPointList)
        {
            mPointList.clear();
        }
        super.reset();
    }

    @Override
    public void rewind()
    {
        synchronized(mPointList)
        {
            mPointList.clear();
        }
        super.rewind();
    }

    @Override
    public void moveTo(float x, float y)
    {
        synchronized(mPointList)
        {
            mPointList.add(new PointF(x, y));
        }
        super.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y)
    {
        synchronized(mPointList)
        {
            mPointList.add(new PointF(x, y));
        }
        super.lineTo(x, y);
    }
}
