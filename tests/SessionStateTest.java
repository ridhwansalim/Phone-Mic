package com.phonemic.app;

public final class SessionStateTest {
    public static void main(String[] args) {
        SessionState session=new SessionState();
        session.focusGained();check(session,SessionState.State.STOPPED,"Focus gain must never start an idle microphone");
        session.start();session.temporaryLoss(true);check(session,SessionState.State.PAUSED,"A call must pause");
        session.focusGained();check(session,SessionState.State.LIVE,"Returning focus must resume an interrupted session");
        session.temporaryLoss(true);session.stop();session.focusGained();check(session,SessionState.State.STOPPED,"Stop during a call must cancel resume");
        session.start();session.temporaryLoss(false);session.focusGained();check(session,SessionState.State.STOPPED,"Disabled auto resume must leave the microphone stopped");
        session.start();session.stop();session.focusGained();check(session,SessionState.State.STOPPED,"Permanent focus loss or disconnect must not resume");
        System.out.println("PASS: interruption/resume state, stop cancellation, resume disabled, no unsolicited start");
    }
    private static void check(SessionState session,SessionState.State state,String message) {
        if(session.get()!=state)throw new AssertionError(message);
    }
}
