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

package de.uni_weimar.mheinz.androidtouchscope.scope.wave;

public class TriggerData
{
    public TriggerSrc source;
    public TriggerEdge edge;
    public double level;

    public TriggerData()
    {
        level = 1.0;
        source = TriggerSrc.CHAN1;
        edge = TriggerEdge.NEGATIVE;
    }


    public enum TriggerEdge
    {
        NEGATIVE("NEGATIVE"),
        POSITIVE("POSITIVE"),
        BOTH("ALTERNATION");

        private final String mText;

        TriggerEdge(final String text)
        {
            mText = text;
        }

        @Override
        public String toString()
        {
            return mText;
        }

        public static TriggerEdge toTriggerEdge(String text)
        {
            if(text.compareToIgnoreCase(NEGATIVE.toString()) == 0)
            {
                return NEGATIVE;
            }
            else if(text.compareToIgnoreCase(POSITIVE.toString()) == 0)
            {
                return POSITIVE;
            }
            else
            {
                return BOTH;
            }
        }
    }

    public enum TriggerSrc
    {
        CHAN1("CH1"),
        CHAN2("CH2"),
        EXT("EXT"),
        AC("ACLINE");

        private final String mText;

        TriggerSrc(final String text)
        {
            mText = text;
        }

        @Override
        public String toString()
        {
            return mText;
        }

        public static TriggerSrc toTriggerSrc(String text)
        {
            if(text.compareToIgnoreCase(CHAN2.toString()) == 0 ||
               text.compareToIgnoreCase("CHAN2") == 0)
            {
                return CHAN2;
            }
            else if(text.compareToIgnoreCase(EXT.toString()) == 0)
            {
                return EXT;
            }
            else if(text.compareToIgnoreCase(AC.toString()) == 0)
            {
                return AC;
            }
            else
            {
                return CHAN1;
            }
        }
    }
}
