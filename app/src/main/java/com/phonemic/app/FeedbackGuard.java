package com.phonemic.app;

/** Detects persistent, dominant narrow-band tones. This is a howling heuristic, not echo cancellation.
 * Attenuation latches for this stream; it never repeatedly turns a feedback loop back up.
 * Strong sustained musical notes can trigger it too. Speech mode is its intended use. */
public final class FeedbackGuard {
    private static final int SIZE=2048;
    private final double[] real=new double[SIZE], imaginary=new double[SIZE], window=new double[SIZE];
    private final float[] frame=new float[SIZE];
    private int filled, lastBin=-100, persistent, cooldown;
    private float gain=1;
    private final int rate;
    public FeedbackGuard(int rate) {
        this.rate=rate;
        for(int i=0;i<SIZE;i++)window[i]=.5-.5*Math.cos(2*Math.PI*i/(SIZE-1));
    }
    public float getGain() {return gain;}
    public void accept(float sample) {
        frame[filled++]=sample;
        if(filled==SIZE) {filled=0;analyze();}
    }
    private void analyze() {
        double energy=0;
        for(int i=0;i<SIZE;i++) {energy+=frame[i]*frame[i];real[i]=frame[i]*window[i];imaginary[i]=0;}
        if(cooldown>0)cooldown--;
        if(energy/SIZE<.0001) {persistent=0;return;} // Ignore signals below -40 dBFS RMS.
        for(int i=1,j=0;i<SIZE;i++) {
            int bit=SIZE>>1;for(; (j&bit)!=0;bit>>=1)j^=bit;j^=bit;
            if(i<j) {double t=real[i];real[i]=real[j];real[j]=t;}
        }
        for(int length=2;length<=SIZE;length<<=1) {
            double angle=-2*Math.PI/length,wr=Math.cos(angle),wi=Math.sin(angle);
            for(int start=0;start<SIZE;start+=length) {
                double ur=1,ui=0;
                for(int j=0;j<length/2;j++) {
                    int a=start+j,b=a+length/2;
                    double vr=real[b]*ur-imaginary[b]*ui,vi=real[b]*ui+imaginary[b]*ur;
                    real[b]=real[a]-vr;imaginary[b]=imaginary[a]-vi;real[a]+=vr;imaginary[a]+=vi;
                    double next=ur*wr-ui*wi;ui=ur*wi+ui*wr;ur=next;
                }
            }
        }
        double total=0,max=0;int peak=0;
        for(int i=1;i<SIZE/2;i++) {
            double power=real[i]*real[i]+imaginary[i]*imaginary[i];total+=power;
            if(i*rate/SIZE>=180 && i*rate/SIZE<=10000 && power>max) {max=power;peak=i;}
        }
        double nearby=0;
        for(int i=Math.max(1,peak-1);i<=Math.min(SIZE/2-1,peak+1);i++)nearby+=real[i]*real[i]+imaginary[i]*imaginary[i];
        if(total>0 && nearby/total>.82 && Math.abs(peak-lastBin)<=1) persistent++;
        else persistent=0;
        lastBin=peak;
        if(persistent>=7 && cooldown==0) {
            gain=Math.max(.0625f,gain*.25f); // Up to two 12 dB reductions per stream.
            persistent=0;cooldown=24; // Allow roughly one second for Bluetooth's buffered output to drain.
        }
    }
}
