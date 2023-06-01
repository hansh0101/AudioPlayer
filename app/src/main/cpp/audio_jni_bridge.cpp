#include <jni.h>
#include "AudioSinkUnit.h"

using namespace std;

/**
 * AudioSinkUnit 객체를 구성한다.
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_initialize(JNIEnv *, jobject,
                                                                     jint channel_count,
                                                                     jint sample_rate,
                                                                     jint bit_depth,
                                                                     jboolean is_float) {
    auto *audioSink = new AudioSinkUnit(channel_count, sample_rate, bit_depth, is_float);
    return (jlong) audioSink;
}

/**
 * AudioSinkUnit 객체 사용을 마친 후 리소스를 정리한다.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_release(JNIEnv *, jobject,
                                                                  jlong audio_sink) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    delete audioSink;
}

/**
 * AudioSinkUnit 객체를 사용해 output_byte_array에 들어있는 size 크기의 오디오 데이터를 소리로 출력을 요청한다.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_requestPlayback(JNIEnv *env, jobject,
                                                                          jlong audio_sink,
                                                                          jbyteArray output_byte_array,
                                                                          jint size) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    jbyte *elements = env->GetByteArrayElements(output_byte_array, nullptr);
    void *audioDataPtr = static_cast<void *>(elements);
    audioSink->requestPlayback(audioDataPtr, size);
}

/**
 * AudioSinkUnit의 오디오 재생 중지를 요청한다.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_requestStart(JNIEnv *, jobject,
                                                                       jlong audio_sink) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    audioSink->requestStart();
}

/**
 * AudioSinkUnit의 오디오 재생 중지를 요청한다.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_output_OboeOutputUnit_requestPause(JNIEnv *, jobject,
                                                                       jlong audio_sink) {
    auto *audioSink = (AudioSinkUnit *) audio_sink;
    audioSink->requestPause();
}