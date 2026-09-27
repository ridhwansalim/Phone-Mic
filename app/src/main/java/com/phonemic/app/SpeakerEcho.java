package com.phonemic.app;

import java.util.Arrays;
import java.util.concurrent.FutureTask;

/** Calibrates without mic monitoring, then cancels using the exact post-effects rendered PCM.
 * Call capture() followed by rendered() for every complete frame on one audio thread. */
public final class SpeakerEcho implements AutoCloseable {
    public static final int FRAME=480, CALIBRATION_FRAMES=750, PROBE_FRAMES=600;
    private final short[] sent=new short[CALIBRATION_FRAMES*FRAME],captured=new short[sent.length];
    private final short[] history=new short[131072],far=new short[FRAME];
    private int frames,random=0x723a91c5,referenceDelay;
    private long written;
    private float noise;
    private NativeEcho engine;
    private FutureTask<Learned> learning;
    private boolean closed;
    public boolean ready() {return engine!=null;}
    public int delayMs() {return (referenceDelay+960)*1000/48000;}
    public int progress() {return Math.min(95,frames*95/(CALIBRATION_FRAMES+100));}
    /** False means the supplied frame now contains a probe or silence; don't run vocal DSP on it. */
    public boolean capture(short[] microphone) {
        if(closed)throw new IllegalStateException("Speaker cancellation is closed");
        if(microphone.length!=FRAME)throw new IllegalArgumentException("Expected 480 samples");
        if(engine==null) {
            if(frames<CALIBRATION_FRAMES) {
                System.arraycopy(microphone,0,captured,frames*FRAME,FRAME);
                for(int i=0;i<FRAME;i++) {
                    short probe=0;
                    if(frames<PROBE_FRAMES) {
                        random^=random<<13;random^=random>>>17;random^=random<<5;
                        noise+=.25f*((random/(float)Integer.MAX_VALUE)-noise);
                        float fade=Math.min(1,Math.min(frames/30f,(PROBE_FRAMES-frames)/30f));
                        probe=(short)(noise*3600*fade);
                    }
                    microphone[i]=probe;sent[frames*FRAME+i]=probe;
                }
            } else {
                Arrays.fill(microphone,(short)0);
                if(learning==null) {
                    learning=new FutureTask<>(()->learn(sent,captured));
                    Thread thread=new Thread(learning,"PhoneMic-EchoCalibration");thread.setDaemon(true);thread.start();
                }
                if(learning.isDone()) {
                    try {Learned result=learning.get();engine=result.engine;referenceDelay=result.delay;}
                    catch(Exception e) {
                        Throwable cause=e.getCause()==null?e:e.getCause();
                        throw new IllegalStateException("Calibration stopped: "+cause.getMessage()+". You can disable Cancel speaker echo to use the normal microphone.",cause);
                    }
                    learning=null;clearTiming();
                    // This frame was deliberately silenced. Normal capture starts next frame.
                }
                if(frames>1800 && engine==null)throw new IllegalStateException("Speaker calibration timed out. Try again.");
            }
            frames++;return false;
        }
        for(int i=0;i<FRAME;i++) {
            long index=written-referenceDelay+i;
            far[i]=index<0 || index>=written?0:history[(int)index & (history.length-1)];
        }
        engine.process(microphone,far);
        return true;
    }
    public void rendered(short[] pcm,int offset,int count) {
        if(engine==null)return;
        for(int i=0;i<count;i++)history[(int)written++ & (history.length-1)]=pcm[offset+i];
    }
    /** Preserve the learned room filter across a call, but discard stale playback history. */
    public void clearTiming() {
        written=0;Arrays.fill(history,(short)0);Arrays.fill(far,(short)0);
        if(engine!=null) {short[] silent=new short[FRAME];for(int i=0;i<25;i++){Arrays.fill(silent,(short)0);engine.process(silent,far);}}
    }
    private static final class Learned {
        final NativeEcho engine;final int delay;
        Learned(NativeEcho engine,int delay){this.engine=engine;this.delay=delay;}
    }
    private static Learned learn(short[] sent,short[] microphone) {
        EchoDelayEstimator.Result estimate=EchoDelayEstimator.estimate(sent,microphone);
        int clipped=0;double power=0;for(short x:microphone){if(Math.abs((int)x)>32000)clipped++;power+=(double)x*x;}
        String metrics=String.format(java.util.Locale.US," [match %.2f, delay %d ms, mic RMS %.0f]",estimate.confidence,estimate.samples/48,Math.sqrt(power/microphone.length));
        if(clipped>microphone.length/1000)throw new IllegalStateException("Microphone overloaded; lower speaker volume"+metrics);
        if(estimate.confidence<.16)throw new IllegalStateException("Test sound not clearly detected; check speaker media volume and try a slightly higher level"+metrics);
        if(estimate.samples<960 || estimate.samples>57600)throw new IllegalStateException("Measured delay is outside the supported range"+metrics);
        NativeEcho echo=new NativeEcho();
        try {
            int delay=estimate.samples-960;short[] near=new short[FRAME],reference=new short[FRAME];
            double before=0,after=0;
            for(int start=0;start<microphone.length;start+=FRAME) {
                if(Thread.currentThread().isInterrupted())throw new IllegalStateException("Calibration cancelled");
                System.arraycopy(microphone,start,near,0,FRAME);
                for(int i=0;i<FRAME;i++){int index=start-delay+i;reference[i]=index<0?0:sent[index];}
                // Measure the final steady probe second after its acoustic arrival,
                // not a fixed capture window while a long-delay path is still learning.
                int validationStart=(PROBE_FRAMES-130)*FRAME+estimate.samples;
                int validationEnd=(PROBE_FRAMES-30)*FRAME+estimate.samples;
                boolean measure=start>=validationStart && start<validationEnd;
                if(measure)for(short x:near)before+=(double)x*x;
                echo.process(near,reference);
                if(measure)for(short x:near)after+=(double)x*x;
            }
            if(after>before*.7 || before<1)throw new IllegalStateException(String.format(java.util.Locale.US,"Speaker detected, but cancellation was insufficient (%.1f dB); keep devices still or use normal mode",10*Math.log10(Math.max(1,before)/Math.max(1,after)))+metrics);
            return new Learned(echo,delay);
        } catch(Throwable e) {echo.close();throw e;}
    }
    @Override public void close() {
        closed=true;
        if(engine!=null){engine.close();engine=null;}
        if(learning!=null) {
            // Do not cancel a FutureTask after allocation: retrieve and release any completed native result.
            FutureTask<Learned> task=learning;learning=null;
            Thread cleanup=new Thread(()->{try{task.get().engine.close();}catch(Exception ignored){}} ,"PhoneMic-EchoCleanup");
            cleanup.setDaemon(true);cleanup.start();
        }
    }
}
