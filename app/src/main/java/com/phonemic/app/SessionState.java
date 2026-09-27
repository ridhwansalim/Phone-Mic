package com.phonemic.app;

/** Main-thread intent state; worker completion never overrides a later Stop or focus event. */
public final class SessionState {
    public enum State { STOPPED, LIVE, PAUSED }
    private State state=State.STOPPED;
    public State get() {return state;}
    public void start() {state=State.LIVE;}
    public void stop() {state=State.STOPPED;}
    public void temporaryLoss(boolean resumeEnabled) {
        if(state!=State.STOPPED)state=resumeEnabled?State.PAUSED:State.STOPPED;
    }
    public void focusGained() {if(state==State.PAUSED)state=State.LIVE;}
}
