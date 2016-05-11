package de.uni_weimar.mheinz.androidtouchscope.display.callback;

import de.uni_weimar.mheinz.androidtouchscope.scope.ScopeInterface;

public interface OnDataChangedInterface
{
    void doCommand(ScopeInterface.Command command, int channel, Object specialData);
    void moveWave(int channel, float pos, boolean moving);
    void moveTime(float pos, boolean moving);
    void moveTrigger(float pos, boolean moving);

    class OnDataChanged implements OnDataChangedInterface
    {
        public void doCommand(ScopeInterface.Command command, int channel, Object specialData){}
        public void moveWave(int channel, float pos, boolean moving){}
        public void moveTime(float pos, boolean moving){}
        public void moveTrigger(float pos, boolean moving){}
    }
}


