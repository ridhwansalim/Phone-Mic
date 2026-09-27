package com.phonemic.app;

/** Allocation-free PCM processing: five peaking filters, bass shelf and feedback delay. */
public final class AudioProcessor {
    private final Filter[] filters = new Filter[6];
    private final float[] delay;
    private final int rate;
    private int cursor;
    private float gain;
    private AudioSettings current;
    private final FeedbackGuard guard;
    private float previousInput, highPassed, envelope, gateGain;
    private final float highPassCoefficient, envelopeRelease, gateRelease;
    private int gateHold;
    private boolean wasMuted;
    public AudioProcessor(int rate) {
        this.rate = rate;
        delay = new float[rate];
        guard=new FeedbackGuard(rate);
        highPassCoefficient=(float)Math.exp(-2*Math.PI*100/rate);
        envelopeRelease=(float)Math.exp(-1.0/(rate*.045));
        gateRelease=(float)(1-Math.exp(-1.0/(rate*.035)));
        for (int i=0; i<filters.length; i++) filters[i] = new Filter();
    }
    public float process(short[] samples, int count, AudioSettings settings) {
        return process(samples,count,settings,true);
    }
    public float protectionGain() {return guard.getGain();}
    public float process(short[] samples, int count, AudioSettings settings, boolean transmit) {
        if(!transmit) {
            if(!wasMuted) {java.util.Arrays.fill(delay,0);for(Filter f:filters)f.reset();gain=0;envelope=0;gateGain=0;gateHold=0;previousInput=0;highPassed=0;}
            wasMuted=true;
            java.util.Arrays.fill(samples,0,count,(short)0);
            return 0;
        }
        wasMuted=false;
        if (settings != current) {
            double[] hz = {100, 400, 1000, 4000, 10000};
            for (int i=0; i<5; i++) filters[i].configure(hz[i], settings.bands[i], rate, false);
            filters[5].configure(180, settings.bass, rate, true);
            current = settings;
        }
        float peak = 0;
        int offset = rate * settings.delayMs / 1000;
        for (int i=0; i<count; i++) {
            float x = samples[i] / 32768f;
            peak = Math.max(peak, Math.abs(x));
            if(settings.protection) {
                guard.accept(x);
                highPassed=highPassCoefficient*(highPassed+x-previousInput);previousInput=x;x=highPassed;
                envelope=Math.max(Math.abs(x),envelope*envelopeRelease);
                if(envelope>.012f)gateHold=rate/12;
                else if(gateHold>0)gateHold--;
                float target=gateHold>0?1:0;
                gateGain+=(target-gateGain)*(target>gateGain?.012f:gateRelease);
                x*=gateGain;
            }
            for (Filter filter : filters) x = filter.next(x);
            float wet = delay[(cursor - offset + delay.length) % delay.length];
            delay[cursor] = Math.max(-2, Math.min(2, x + wet * settings.echo * .55f));
            cursor = (cursor + 1) % delay.length;
            float targetVolume=settings.volume*(settings.protection?guard.getGain():1);
            gain += (targetVolume - gain) * .002f;
            x = (x + wet * settings.echo) * gain;
            // Soft limiting protects against digital overflow; it cannot prevent acoustic feedback.
            x = (float)Math.tanh(x);
            samples[i] = (short)Math.round(x * 32767);
        }
        return peak;
    }
    private static final class Filter {
        double b0=1,b1,b2,a1,a2,z1,z2;
        void configure(double hz, double db, int rate, boolean shelf) {
            double a=Math.pow(10, db/40), w=2*Math.PI*hz/rate, c=Math.cos(w), s=Math.sin(w);
            double alpha=s/2/.85, a0;
            if (shelf) {
                alpha=s/2*Math.sqrt(2); double t=2*Math.sqrt(a)*alpha;
                b0=a*((a+1)-(a-1)*c+t); b1=2*a*((a-1)-(a+1)*c); b2=a*((a+1)-(a-1)*c-t);
                a0=(a+1)+(a-1)*c+t; a1=-2*((a-1)+(a+1)*c); a2=(a+1)+(a-1)*c-t;
            } else {
                b0=1+alpha*a; b1=-2*c; b2=1-alpha*a; a0=1+alpha/a; a1=-2*c; a2=1-alpha/a;
            }
            b0/=a0; b1/=a0; b2/=a0; a1/=a0; a2/=a0;
        }
        float next(float x) { double y=b0*x+z1; z1=b1*x-a1*y+z2; z2=b2*x-a2*y; return (float)y; }
        void reset() {z1=0;z2=0;}
    }
}
