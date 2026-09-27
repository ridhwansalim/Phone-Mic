package com.phonemic.app;

public final class AudioProcessorTest {
    public static void main(String[] args) {
        AudioSettings dry=new AudioSettings(1,0,0,100,new float[5]);
        short[] silence=new short[48000];new AudioProcessor(48000).process(silence,silence.length,dry);
        for(short sample:silence) check(sample==0,"Silence must stay silent");
        short[] muted=tone(1000,48000,12000);
        new AudioProcessor(48000).process(muted,muted.length,new AudioSettings(0,12,.65f,80,new float[]{12,12,12,12,12}));
        for(short sample:muted)check(sample==0,"Mute must silence all effects");
        double neutral=energy(100,0,new float[5]);
        check(energy(100,12,new float[5])>neutral*1.8,"Bass shelf must increase low frequency energy");
        check(energy(1000,0,new float[]{0,0,12,0,0})>energy(1000,0,new float[5])*1.8,"EQ must boost selected band");
        AudioProcessor echo=new AudioProcessor(48000);short[] warm=new short[4800];echo.process(warm,warm.length,dry);
        short[] impulse=new short[14000];impulse[0]=10000;
        echo.process(impulse,impulse.length,new AudioSettings(1,0,.5f,100,new float[5]));
        check(impulse[4800]>1000,"Echo must appear after configured delay");
        check(impulse[9600]>0 && impulse[9600]<impulse[4800],"Echo must decay");
        short[] loud=tone(100,48000,32767);
        new AudioProcessor(48000).process(loud,loud.length,new AudioSettings(1,12,.65f,80,new float[]{12,12,12,12,12}));
        for(int i=24000;i<loud.length;i++)check(loud[i]!=Short.MIN_VALUE,"Limiter must avoid overflow");
        FeedbackGuard toneGuard=new FeedbackGuard(48000);
        for(short x:tone(2000,96000,8000))toneGuard.accept(x/32768f);
        check(toneGuard.getGain()<=.25f,"Sustained dominant feedback tone must reduce gain");
        for(int i=0;i<48000*3;i++)toneGuard.accept(0);
        check(toneGuard.getGain()<=.25f,"Guard must not automatically turn the feedback loop back up");
        FeedbackGuard noiseGuard=new FeedbackGuard(48000);
        java.util.Random random=new java.util.Random(37);
        for(int i=0;i<48000*3;i++)noiseGuard.accept((random.nextFloat()-.5f)*.2f);
        check(noiseGuard.getGain()==1,"Broadband noise must not trigger tone guard");
        FeedbackGuard shortTone=new FeedbackGuard(48000);
        for(short x:tone(2000,4800,8000))shortTone.accept(x/32768f);
        for(int i=0;i<48000;i++)shortTone.accept(0);
        check(shortTone.getGain()==1,"A brief tone must not trigger sustained feedback protection");
        AudioProcessor protectedProcessor=new AudioProcessor(48000);
        short[] quiet=tone(500,48000,80);
        protectedProcessor.process(quiet,quiet.length,AudioSettings.vocal());
        for(short x:quiet)check(x==0,"Noise gate must suppress low-level ambient signal");
        short[] speech=tone(1000,4800,8000);
        protectedProcessor.process(speech,speech.length,AudioSettings.vocal());
        check(rms(speech,2400)>100,"Gate must open for a louder voice-level signal");
        AudioProcessor held=new AudioProcessor(48000);
        AudioSettings withEcho=new AudioSettings(1,0,.65f,100,new float[5]);
        short[] voice=tone(1000,12000,8000);held.process(voice,voice.length,withEcho);
        held.process(voice,voice.length,withEcho,false);
        for(short x:voice)check(x==0,"Hold-to-talk release must mute immediately");
        short[] afterRelease=new short[48000];held.process(afterRelease,afterRelease.length,withEcho,true);
        for(short x:afterRelease)check(x==0,"Hold-to-talk must clear old echo before reopening");
        AudioSettings vocal=AudioSettings.vocal();
        check(vocal.echo==0 && vocal.bass==0 && vocal.protection && vocal.volume<=.25f,"Vocal preset must start conservatively");
        System.out.println("PASS: DSP, EQ/bass, echo timing/decay, tone guard, gate, hold-to-talk and vocal preset");
    }
    static short[] tone(int hz,int length,int amplitude) {short[] x=new short[length];for(int i=0;i<length;i++)x[i]=(short)(amplitude*Math.sin(2*Math.PI*hz*i/48000));return x;}
    static double energy(int hz,float bass,float[] bands) {short[] x=tone(hz,48000,1000);new AudioProcessor(48000).process(x,x.length,new AudioSettings(1,bass,0,100,bands));double sum=0;for(int i=24000;i<x.length;i++)sum+=(double)x[i]*x[i];return Math.sqrt(sum/24000);}
    static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
    static double rms(short[] x,int start) {double sum=0;for(int i=start;i<x.length;i++)sum+=(double)x[i]*x[i];return Math.sqrt(sum/(x.length-start));}
}
