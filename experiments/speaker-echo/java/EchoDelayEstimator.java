package com.phonemic.app;

/** Normalized cross-correlation of a known calibration probe, never of the user's speech. */
public final class EchoDelayEstimator {
    public static final class Result {
        public final int samples;
        public final double confidence;
        Result(int samples,double confidence) {this.samples=samples;this.confidence=confidence;}
    }
    public static Result estimate(short[] sent,short[] captured) {
        if(sent.length!=captured.length || sent.length<48000*4)throw new IllegalArgumentException("Calibration requires four seconds");
        float[] lowSent=downsample(sent,24),lowCaptured=downsample(captured,24);
        Result coarse=search(lowSent,lowCaptured,0,2400,400,4600);
        float[] fineSent=downsample(sent,6),fineCaptured=downsample(captured,6);
        int center=coarse.samples*4;
        Result fine=search(fineSent,fineCaptured,Math.max(0,center-16),Math.min(9600,center+16),1600,18400);
        // An independent later portion must agree, rejecting random/noisy matches.
        // Bluetooth and microphone clocks need not match. Validate the later probe
        // with a local delay search, rather than requiring sample-perfect alignment.
        Result late=search(fineSent,fineCaptured,Math.max(0,fine.samples-160),Math.min(9600,fine.samples+160),18400,23200);
        return new Result(late.samples*6,Math.min(fine.confidence,late.confidence));
    }
    private static float[] downsample(short[] input,int ratio) {
        float[] out=new float[input.length/ratio];
        for(int i=0;i<out.length;i++) {double sum=0;for(int j=0;j<ratio;j++)sum+=input[i*ratio+j];out[i]=(float)(sum/ratio);}
        return out;
    }
    private static Result search(float[] sent,float[] captured,int low,int high,int start,int end) {
        int delay=low;double best=0;
        for(int d=low;d<=high;d++) {double c=Math.abs(correlation(sent,captured,d,start,end));if(c>best){best=c;delay=d;}}
        return new Result(delay,best);
    }
    private static double correlation(float[] sent,float[] captured,int delay,int start,int end) {
        double dot=0,xx=0,yy=0;
        for(int i=start;i<Math.min(end,captured.length-delay);i++) {double x=sent[i],y=captured[i+delay];dot+=x*y;xx+=x*x;yy+=y*y;}
        return dot/Math.sqrt(xx*yy+1e-20);
    }
}
