package com.phonemic.app;

/** Single-thread-owned SpeexDSP reference echo canceller. */
public final class NativeEcho implements AutoCloseable {
    static {System.loadLibrary("phonemic_echo");}
    private long handle;
    public NativeEcho() {handle=create();if(handle==0)throw new IllegalStateException("Echo canceller could not be allocated");}
    public void process(short[] microphone,short[] speakerReference) {
        if(handle==0)throw new IllegalStateException("Echo canceller is closed");
        if(microphone==null || speakerReference==null || microphone.length!=480 || speakerReference.length!=480)
            throw new IllegalArgumentException("Echo cancellation requires 480-sample microphone and reference frames");
        processNative(handle,microphone,speakerReference);
    }
    @Override public void close() {if(handle!=0){destroy(handle);handle=0;}}
    private static native long create();
    private static native void processNative(long handle,short[] microphone,short[] reference);
    private static native void destroy(long handle);
}
