package com.phonemic.app;

import java.util.*;

public final class SpeakerEchoTest {
    public static void main(String[] args) throws Exception {
        delayEstimation();nativeCancellation();fullPipeline(14400);fullPipeline(45408);failedCalibration();
        System.out.println("PASS: delay estimation, reference cancellation, double talk, silence and full calibration pipeline");
    }
    private static void delayEstimation() {
        short[] sent=new short[216000],mic=new short[sent.length];Random random=new Random(72);float low=0;
        for(int i=0;i<144000;i++){low+=.25f*((random.nextFloat()-.5f)*3000-low);sent[i]=(short)low;}
        for(int delay:new int[]{4800,14400,33600,54000}) {
            Arrays.fill(mic,(short)0);
            for(int i=delay;i<mic.length;i++)mic[i]=(short)(sent[i-delay]*.7+(random.nextFloat()-.5)*50);
            EchoDelayEstimator.Result result=EchoDelayEstimator.estimate(sent,mic);
            check(Math.abs(result.samples-delay)<=6,"Delay alignment failed: "+delay+" versus "+result.samples);
            check(result.confidence>.7,"Clear probe must have strong confidence");
        }
        Arrays.fill(mic,(short)0);check(EchoDelayEstimator.estimate(sent,mic).confidence<.16,"Silent speaker must not calibrate");
    }
    private static void nativeCancellation() {
        Random random=new Random(7);short[] history=new short[48000],far=new short[480],near=new short[480];int cursor=0;
        double input=0,output=0,nearEnergy=0,error=0;short[] desired=new short[480];
        try(NativeEcho echo=new NativeEcho()) {
            for(int frame=0;frame<1800;frame++) {
                for(int i=0;i<480;i++) {
                    far[i]=(short)((random.nextFloat()-.5)*6000);history[cursor%history.length]=far[i];
                    desired[i]=frame>=1200?(short)((random.nextFloat()-.5)*6000):0;
                    double y=.65*history[(cursor-960+history.length)%history.length]+.2*history[(cursor-1440+history.length)%history.length];
                    near[i]=(short)(y+desired[i]);cursor++;
                    if(frame>=800 && frame<1200)input+=(double)near[i]*near[i];
                }
                echo.process(near,far);
                for(int i=0;i<480;i++) {
                    if(frame>=800 && frame<1200)output+=(double)near[i]*near[i];
                    // Speex preprocessor has frame latency; double-talk check is energy preservation.
                    if(frame>=1400){nearEnergy+=(double)desired[i]*desired[i];error+=(double)near[i]*near[i];}
                }
            }
            double reduction=10*Math.log10(input/Math.max(1,output));
            System.out.printf(Locale.US,"Synthetic echo-only reduction: %.1f dB; double-talk output/near power: %.2f%n",reduction,error/nearEnergy);
            check(reduction>15,"Known linear echo must be cancelled rather than just limited");
            check(error/nearEnergy>.45 && error/nearEnergy<1.6,"Independent near-end audio must survive double talk");
        }
        try(NativeEcho echo=new NativeEcho()) {
            Arrays.fill(far,(short)0);Arrays.fill(near,(short)0);
            for(int i=0;i<100;i++)echo.process(near,far);
            for(short x:near)check(x==0,"Silent reference and microphone must remain silent");
        }
    }
    private static void fullPipeline(int simulatedDelay) throws Exception {
        short[] played=new short[262144],frame=new short[480];long cursor=0;Random random=new Random(10);
        double before=0,after=0;boolean learned=false;int liveFrames=0;
        try(SpeakerEcho echo=new SpeakerEcho()) {
            for(int n=0;n<1700;n++) {
                for(int i=0;i<480;i++) {
                    long index=cursor+i-simulatedDelay;
                    frame[i]=index>=0?(short)(played[(int)index & (played.length-1)]*.8):0;
                }
                short[] original=frame.clone();
                boolean live=echo.capture(frame);
                if(live) {
                    learned=true;liveFrames++;
                    if(liveFrames>150)for(int i=0;i<480;i++){before+=(double)original[i]*original[i];after+=(double)frame[i]*frame[i];}
                    // Independent playback exercises the learned reference model without acoustic-loop bias.
                    for(int i=0;i<480;i++)frame[i]=(short)((random.nextFloat()-.5)*3000);
                }
                for(int i=0;i<480;i++)played[(int)(cursor+i)&(played.length-1)]=frame[i];
                echo.rendered(frame,0,173);echo.rendered(frame,173,307); // Partial AudioTrack writes.
                cursor+=480;
                if(!echo.ready() && n>=SpeakerEcho.CALIBRATION_FRAMES)Thread.sleep(5);
            }
            check(learned,"Calibration must complete");
            check(Math.abs(echo.delayMs()-simulatedDelay/48)<=1,"Reported Bluetooth delay should match simulation");
            double reduction=10*Math.log10(before/Math.max(1,after));
            System.out.printf(Locale.US,"End-to-end calibrated echo-only reduction: %.1f dB%n",reduction);
            check(reduction>12,"Learned cancellation must suppress the simulated speaker echo");
            echo.clearTiming();Arrays.fill(played,(short)0);cursor=0;
            double sourcePower=0,cleanPower=0,tailPower=0;float voice=0;
            for(int n=0;n<800;n++) {
                for(int i=0;i<480;i++) {
                    voice+=.3f*((random.nextFloat()-.5f)*7000-voice);
                    float direct=n<400?voice:0;
                    long index=cursor+i-simulatedDelay;
                    float returned=index>=0?played[(int)index & (played.length-1)]*.8f:0;
                    frame[i]=(short)(direct+returned);
                    if(n>100 && n<400)sourcePower+=direct*direct;
                }
                echo.capture(frame);
                for(int i=0;i<480;i++) {
                    if(n>100 && n<400)cleanPower+=(double)frame[i]*frame[i];
                    if(n>600)tailPower+=(double)frame[i]*frame[i];
                    frame[i]=(short)Math.max(-16000,Math.min(16000,frame[i]*1.3)); // Uncancelled loop gain exceeds one.
                    played[(int)(cursor+i)&(played.length-1)]=frame[i];
                }
                echo.rendered(frame,0,480);cursor+=480;
            }
            System.out.printf(Locale.US,"Closed-loop near power retained: %.2f; residual tail RMS: %.2f PCM units%n",cleanPower/sourcePower,Math.sqrt(tailPower/(199*480)));
            check(cleanPower/sourcePower>.3 && cleanPower/sourcePower<2,"Closed-loop cancellation must retain near-end speech-like input");
            check(Math.sqrt(tailPower/(199*480))<100,"Reference cancellation must prevent a sustained linear feedback loop");
        }
    }
    private static void failedCalibration() throws Exception {
        boolean rejected=false;
        try(SpeakerEcho echo=new SpeakerEcho()) {
            short[] silence=new short[480];
            for(int n=0;n<1800;n++) {
                Arrays.fill(silence,(short)0);
                try {echo.capture(silence);}catch(IllegalStateException expected){rejected=true;break;}
                echo.rendered(silence,0,480);
                if(n>=450)Thread.sleep(2);
            }
        }
        check(rejected,"A speaker with no detectable return must fail calibration rather than go live");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
