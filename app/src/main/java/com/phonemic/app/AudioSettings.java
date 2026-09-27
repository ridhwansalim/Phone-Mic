package com.phonemic.app;

/** Immutable settings snapshot, safely published between UI and audio threads. */
public final class AudioSettings {
    public final float volume, bass, echo;
    public final int delayMs;
    public final float[] bands;
    public final boolean protection, holdToTalk, resumeAfterInterruption;
    public AudioSettings(float volume, float bass, float echo, int delayMs, float[] bands) {
        this(volume,bass,echo,delayMs,bands,false,false,true);
    }
    public AudioSettings(float volume, float bass, float echo, int delayMs, float[] bands,
                         boolean protection, boolean holdToTalk, boolean resumeAfterInterruption) {
        this.volume = Math.max(0, Math.min(1, volume));
        this.bass = Math.max(0, Math.min(12, bass));
        this.echo = Math.max(0, Math.min(.65f, echo));
        this.delayMs = Math.max(80, Math.min(600, delayMs));
        this.bands = bands.clone();
        this.protection=protection;
        this.holdToTalk=holdToTalk;
        this.resumeAfterInterruption=resumeAfterInterruption;
    }
    /** A conservative speech starting point, not a universal best EQ for every voice/speaker. */
    public static AudioSettings vocal() {
        return new AudioSettings(.25f,0,0,220,new float[]{-6,-3,0,1,-2},true,false,true);
    }
}
