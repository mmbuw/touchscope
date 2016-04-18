package de.uni_weimar.mheinz.androidtouchscope.not_used;

import java.util.ArrayDeque;

/**
 * Wrapper around {@link ArrayDeque} of type Byte, FIFO list with a defined capacity.
 * When adding elements over the capacity, the front elements are removed.
 *
 */
public class LimitedByteDeque
{
    private final ArrayDeque<Byte> mQueue;
    private int mCapacity = 0;

    private final Byte[] mBuffer;

    public LimitedByteDeque(int capacity)
    {
        mQueue = new ArrayDeque<Byte>(capacity);
        mBuffer = new Byte[capacity];
        mCapacity = capacity;
    }

    public int size()
    {
        return mQueue.size();
    }

    public boolean isEmpty()
    {
        return mQueue.isEmpty();
    }

    public void clear()
    {
        synchronized (mQueue)
        {
            mQueue.clear();
        }
    }

    public int remainingCapacity()
    {
        return mCapacity - size();
    }

    /**
     * Inserts the specified element at the tail of this queue, if capacity is already reached,
     * the head will be removed.
     * @param item
     * @return
     */
    public boolean offer(Byte item)
    {
        synchronized (mQueue)
        {
            if (remainingCapacity() == 0)
            {
                mQueue.poll();
            }
        }
        return mQueue.offer(item);
    }

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     * @return
     */
    public Byte poll()
    {
        synchronized (mQueue)
        {
            return mQueue.poll();
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue, or returns null if this
     * queue is empty.
     * @return
     */
    public Byte peek()
    {
        return mQueue.peek();
    }

    /**
     * Retrieves, but does not remove, (count) items from the queue, or returns null if this
     * queue is empty.
     * @param count
     * @return
     */
    public byte[] peekTo(int count)
    {
        synchronized (mQueue)
        {
            int size = count;
            if (count > mQueue.size())
                size = mQueue.size();

            Byte[] data = mQueue.toArray(mBuffer);
            byte[] byteData = new byte[count];
            int i = 0;
            for(; i < size; ++i)
            {
                byteData[i] = (byte)data[i];
            }
            for(; i < count; ++i)
            {
                byteData[i] = 0;
            }
            return byteData;
        }
    }

    /**
     * Inserts the specified elements at the tail of this queue, if capacity is already reached,
     * as many leading elements will be removed as necessary.
     * @param data the data to add
     * @return
     */
    public int addMany(Byte[] data)
    {
        int count = data.length;
        synchronized (mQueue)
        {
            int remaining = remainingCapacity();
            if (count > remaining)
            {
                removeTo(count - remaining);
            }

            for(int i = 0; i < count; ++i)
            {
                mQueue.offer(data[i]);
            }
        }
        return count;
    }

    /**
     * Inserts the specified elements at the tail of this queue, if capacity is already reached,
     * as many leading elements will be removed as necessary.
     * @param data the data to add
     * @return
     */
    public int addMany(byte[] data)
    {
        int count = data.length;
        synchronized (mQueue)
        {
            int remaining = remainingCapacity();
            if (count > remaining)
            {
                removeTo(count - remaining);
            }

            for(int i = 0; i < count; ++i)
            {
                mQueue.offer(data[i]);
            }
        }
        return count;
    }

    /**
     * Removes at most the given number of available elements from this queue
     * @param count
     * @return
     */
    private int removeTo(int count)
    {
        if(count > mQueue.size())
            count = mQueue.size();

        for(int i = 0; i < count; ++i)
        {
            mQueue.poll();
        }
        return count;
    }
}
