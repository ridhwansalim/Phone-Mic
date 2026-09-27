#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include "speex/speex_echo.h"
#include "speex/speex_preprocess.h"

#define FRAME 480
typedef struct {
    SpeexEchoState *echo;
    SpeexPreprocessState *pre;
    short near_pcm[FRAME], far_pcm[FRAME], clean_pcm[FRAME];
} Echo;

JNIEXPORT jlong JNICALL Java_com_phonemic_app_NativeEcho_create(JNIEnv *env, jclass type) {
    (void)env; (void)type;
    Echo *s=(Echo*)calloc(1,sizeof(Echo));
    if(!s)return 0;
    s->echo=speex_echo_state_init(FRAME,9600); /* 200 ms acoustic tail after external delay alignment. */
    s->pre=speex_preprocess_state_init(FRAME,48000);
    if(!s->echo || !s->pre) {
        if(s->echo)speex_echo_state_destroy(s->echo);
        if(s->pre)speex_preprocess_state_destroy(s->pre);
        free(s);return 0;
    }
    int rate=48000,off=0,suppress=-24,active=-10;
    speex_echo_ctl(s->echo,SPEEX_ECHO_SET_SAMPLING_RATE,&rate);
    speex_preprocess_ctl(s->pre,SPEEX_PREPROCESS_SET_ECHO_STATE,s->echo);
    speex_preprocess_ctl(s->pre,SPEEX_PREPROCESS_SET_DENOISE,&off);
    speex_preprocess_ctl(s->pre,SPEEX_PREPROCESS_SET_AGC,&off);
    speex_preprocess_ctl(s->pre,SPEEX_PREPROCESS_SET_ECHO_SUPPRESS,&suppress);
    speex_preprocess_ctl(s->pre,SPEEX_PREPROCESS_SET_ECHO_SUPPRESS_ACTIVE,&active);
    return (jlong)(intptr_t)s;
}
JNIEXPORT void JNICALL Java_com_phonemic_app_NativeEcho_processNative(JNIEnv *env,jclass type,jlong handle,jshortArray near,jshortArray far) {
    (void)type;
    Echo *s=(Echo*)(intptr_t)handle;
    if(!s || (*env)->GetArrayLength(env,near)!=FRAME || (*env)->GetArrayLength(env,far)!=FRAME) {
        jclass error=(*env)->FindClass(env,"java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env,error,"Echo cancellation requires two 480-sample frames and a live handle");return;
    }
    (*env)->GetShortArrayRegion(env,near,0,FRAME,s->near_pcm);
    (*env)->GetShortArrayRegion(env,far,0,FRAME,s->far_pcm);
    if((*env)->ExceptionCheck(env))return;
    speex_echo_cancellation(s->echo,s->near_pcm,s->far_pcm,s->clean_pcm);
    speex_preprocess_run(s->pre,s->clean_pcm);
    (*env)->SetShortArrayRegion(env,near,0,FRAME,s->clean_pcm);
}
JNIEXPORT void JNICALL Java_com_phonemic_app_NativeEcho_destroy(JNIEnv *env,jclass type,jlong handle) {
    (void)env; (void)type;
    Echo *s=(Echo*)(intptr_t)handle;
    if(s) {speex_preprocess_state_destroy(s->pre);speex_echo_state_destroy(s->echo);free(s);}
}
