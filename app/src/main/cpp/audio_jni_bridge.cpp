#include <jni.h>
#include "AudioSinkUnit.h"

using namespace std;

extern "C" JNIEXPORT jlong JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_initialize(JNIEnv *, jobject,
                                                                     jint channel_count,
                                                                     jint sample_rate,
                                                                     jint bit_depth,
                                                                     jboolean is_float) {
    auto *audioSink = new AudioSinkUnit(channel_count, sample_rate, bit_depth, is_float);
    return (jlong) audioSink;
}

extern "C" JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_release(JNIEnv *, jobject,
                                                                  jlong audio_sink) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    delete audioSink;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_requestPlayback(JNIEnv *env, jobject,
                                                                          jlong audio_sink,
                                                                          jobject output_buffer,
                                                                          jint size) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    void *bufferPtr = env->GetDirectBufferAddress(output_buffer);
    audioSink->startAudio(bufferPtr, size);
}