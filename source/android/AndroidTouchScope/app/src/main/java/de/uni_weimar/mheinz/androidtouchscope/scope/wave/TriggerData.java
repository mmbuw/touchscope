package de.uni_weimar.mheinz.androidtouchscope.scope.wave;


public class TriggerData
{
    public TriggerSrc mSource;
    public TriggerEdge mEdge;
    public double mLevel;

    public TriggerData()
    {
        mLevel = 1.0;
        mSource = TriggerSrc.CHAN1;
        mEdge = TriggerEdge.NEGATIVE;
    }


    public enum TriggerEdge
    {
        NEGATIVE("NEGATIVE"),
        POSITIVE("POSITIVE"),
        BOTH("ALTERNATION");

        private final String mText;

        private TriggerEdge(final String text)
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

        private TriggerSrc(final String text)
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
            if(text.compareToIgnoreCase(CHAN2.toString()) == 0)
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
