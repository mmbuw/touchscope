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


import java.util.Locale;

public class MeasureData
{
    public String mVPP;
    public String mVMax;
    public String mVMin;
    public String mVAmp;
    public String mVTop;
    public String mVBase;
    public String mVAvg;
    public String mVRMS;
    public String mOver;
    public String mPre;
    public String mFreq;
    public String mRise;
    public String mFall;
    public String mPeriod;
    public String mPWidth;
    public String mNWidth;
    public String mPDuty;
    public String mNDuty;
    public String mPDelay;
    public String mNDelay;

    public enum MeasureType
    {
        V_PP,
        V_MAX,
        V_MIN,
        V_AMP,
        V_TOP,
        V_BASE,
        V_AVG,
        V_RMS,
        OVER,
        PRE,
        FREQ,
        RISE,
        FALL,
        PERIOD,
        P_WIDTH,
        N_WIDTH,
        P_DUTY,
        N_DUTY,
      /*  P_DELAY,
        N_DELAY*/
    }

    public MeasureData()
    {
        mVPP = mVMax = mVMin = mVAmp = mVTop = mVBase = mVAvg = mVRMS = mOver = mPre = mFreq = "=0.0";
        mPeriod = mPWidth = mNWidth = mPDuty = mNDuty = mPDelay = mNDelay = mRise = mFall = "=0.0";

    }

    public String getTypeString(MeasureType type)
    {
        String text = "";
        switch(type)
        {
            case V_PP:
                text = getVoltText("Vpp ", mVPP);
                break;
            case V_MAX:
                text = getVoltText("Vmax ", mVMax);
                break;
            case V_MIN:
                text = getVoltText("Vmin ", mVMin);
                break;
            case V_AMP:
                text = getVoltText("Vamp ", mVAmp);
                break;
            case V_TOP:
                text = getVoltText("Vtop ", mVTop);
                break;
            case V_BASE:
                text = getVoltText("Vbas ", mVBase);
                break;
            case V_AVG:
                text = getVoltText("Vavg ", mVAvg);
                break;
            case V_RMS:
                text = getVoltText("Vrms ", mVRMS);
                break;
            case OVER:
                text = getPercentText("Vovr ", mOver);
                break;
            case PRE:
                text = getPercentText("Vpre ", mPre);
                break;
            case FREQ:
                text = getFreqText("Freq ", mFreq);
                break;
            case RISE:
                text = getTimeText("Rise ", mRise);
                break;
            case FALL:
                text = getTimeText("Fall ", mFall);
                break;
            case PERIOD:
                text = getTimeText("Prd ", mPeriod);
                break;
            case P_WIDTH:
                text = getTimeText("+Wid ", mPWidth);
                break;
            case N_WIDTH:
                text = getTimeText("-Wid ", mNWidth);
                break;
            case P_DUTY:
                text = getTimeText("+Duty ", mPDuty);
                break;
            case N_DUTY:
                text = getTimeText("-Duty ", mNDuty);
                break;
           /* case P_DELAY:
                break;
            case N_DELAY:
                break;*/
        }
        return text;
    }

    private double getValue(String text) throws NumberFormatException
    {
        double value = 0;
        if(!text.isEmpty())
        {
            if(!Character.isDigit(text.charAt(0)) && text.charAt(0) != '-')
                value = Double.valueOf(text.substring(1));
            else
                value = Double.valueOf(text);
        }
        return value;
    }

    private String getSign(String text)
    {
        String sign = "=";
        if(!text.isEmpty() && !Character.isDigit(text.charAt(0)) && text.charAt(0) != '-')
            sign = text.substring(0, 1);
        return sign;
    }

    private String getVoltText(String startText, String voltText)
    {
        double volt;
        try
        {
            volt = getValue(voltText);
        }
        catch(NumberFormatException ex)
        {
            return String.format(Locale.getDefault(),"%s= *****", startText);
        }

        String sign = getSign(voltText);

        if(volt > 1e30)
        {
            return String.format(Locale.getDefault(),"%s%s *****", startText,sign);
        }

        double value;
        String end;
        double absVolt = Math.abs(volt);

        if (absVolt < 1)
        {
            value = volt * 1e3;
            end = "mV";
        }
        else
        {
            value = volt;
            end = "V";
        }

        return String.format(Locale.getDefault(),"%s%s %.2f%s", startText, sign, value, end);
    }

    private String getTimeText(String startText, String timeText)
    {
        double time;
        try
        {
            time = getValue(timeText);
        }
        catch(NumberFormatException ex)
        {
            return String.format(Locale.getDefault(),"%s= *****", startText);
        }

        String sign = getSign(timeText);

        if(time > 1e36)
        {
            return String.format(Locale.getDefault(),"%s%s *****", startText,sign);
        }

        double value;
        String end;
        double absTime = Math.abs(time);
        if(absTime < 1e-6)
        {
            value = (time * 1e9);
            end = "nS";
        }
        else if(absTime < 1e-3)
        {
            value = time * 1e6;
            end = "uS";
        }
        else if(absTime < 1)
        {
            value = time * 1e3;
            end = "mS";
        }
        else
        {
            value = time;
            end = "S";
        }
        return String.format(Locale.getDefault(),"%s%s %.2f%s",startText, sign, value, end);
    }

    private String getPercentText(String startText, String percentText)
    {
        double percent;
        try
        {
            percent = getValue(percentText);
        }
        catch(NumberFormatException ex)
        {
            return String.format(Locale.getDefault(),"%s= *****", startText);
        }

        String sign = getSign(percentText);

        if(percent > 1e36)
        {
            return String.format(Locale.getDefault(),"%s%s *****", startText, sign);
        }

        return String.format(Locale.getDefault(),"%s%s%.2f%%",startText, sign, percent);
    }

    private String getFreqText(String startText, String freqText)
    {
        double freq;
        try
        {
            freq = getValue(freqText);
        }
        catch(NumberFormatException ex)
        {
            return String.format(Locale.getDefault(),"%s= *****", startText);
        }

        String sign = getSign(freqText);

        if(freq > 1e36)
        {
            return String.format(Locale.getDefault(),"%s%s *****", startText, sign);
        }

        double value;
        String end;

        if (freq >= 1e6)
        {
            value = freq / 1e3;
            end = "mHz";
        }
        else if (freq >= 1e3)
        {
            value = freq / 1e3;
            end = "kHz";
        }
        else
        {
            value = freq;
            end = "Hz";
        }

        return String.format(Locale.getDefault(),"%s%s%.3f%s", startText, sign, value, end);
    }
}
