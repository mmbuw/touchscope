package de.uni_weimar.mheinz.androidtouchscope.display;

import de.uni_weimar.mheinz.androidtouchscope.scope.ScopeInterface;

public interface OnDataChanged
{
    void doCommand(ScopeInterface.Command command, int channel, Object specialData);
    void moveWave(int channel, float pos, boolean moving);
    void moveTime(float pos, boolean moving);
}
