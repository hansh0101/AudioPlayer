#include <jni.h>
#include "AudioSinkUnit.h"

using namespace std;

extern "C" JNIEXPORT jlong JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_initialize(JNIEnv *env, jobject thiz,
                                                                   jint channel_count,
                                                                   jint sample_rate) {
    auto *audioSink = new AudioSinkUnit(channel_count, sample_rate);
    return (jlong) audioSink;
}

extern "C" JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_release(JNIEnv *env, jobject thiz,
                                                                jlong audio_sink) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    delete audioSink;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_requestPlayback(JNIEnv *env, jobject thiz,
                                                                        jlong audio_sink,
                                                                        jobject output_buffer) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    void *bufferPtr = env->GetDirectBufferAddress(output_buffer);
    audioSink->startAudio(bufferPtr);
}