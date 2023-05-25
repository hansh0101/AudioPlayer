#include <jni.h>
#include <string>
#include <oboe/Oboe.h>
#include "AudioOutputUnit.h"
#include <cstdint>
#include <cmath>
#include <android/log.h>

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"C++",__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,"C++",__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,"C++",__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,"C++",__VA_ARGS__)

using namespace std;
using namespace oboe;

// -------------------------------------------------------------------------------------------------
// 실제 코드

extern "C"
JNIEXPORT jlong JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_initialize(JNIEnv *env, jobject thiz) {
    AudioOutputUnit *outputUnit = new AudioOutputUnit();
    return reinterpret_cast<jlong>(outputUnit);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_releaseOutputUnit(JNIEnv *env, jobject thiz,
                                                                          jlong output_unit) {
    AudioOutputUnit *outputUnit = reinterpret_cast<AudioOutputUnit *>(output_unit);
    delete outputUnit;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_requestPlayback(JNIEnv *env, jobject thiz,
                                                                        jlong output_unit,
                                                                        jobject buffer,
                                                                        jint number_of_frames) {
    AudioOutputUnit *outputUnit = reinterpret_cast<AudioOutputUnit *>(output_unit);
    void *audioData = env->GetDirectBufferAddress(buffer);
    int32_t numFrames = static_cast<int32_t>(number_of_frames);
    outputUnit->requestPlayback(audioData, numFrames);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_model_OboeAudioOutputUnit_flushBuffer(JNIEnv *env, jobject thiz,
                                                                    jlong output_unit) {
    AudioOutputUnit *outputUnit = reinterpret_cast<AudioOutputUnit *>(output_unit);
    outputUnit->flush();
}

// -------------------------------------------------------------------------------------------------
// 테스트용 코드

class OboeSinePlayer : public AudioStreamDataCallback {
public:
    virtual ~OboeSinePlayer() = default;

    // Call this from Activity onResume()
    int32_t startAudio() {
        lock_guard<mutex> lock(mLock);
        AudioStreamBuilder builder;

        // The builder set methods can be chained for convenience.
        Result result = builder.setSharingMode(SharingMode::Exclusive)
                ->setPerformanceMode(PerformanceMode::LowLatency)
                ->setChannelCount(kChannelCount)
                ->setSampleRate(kSampleRate)
                ->setSampleRateConversionQuality(SampleRateConversionQuality::Medium)
                ->setFormat(AudioFormat::Float)
                ->setDataCallback(this)
                ->openStream(mStream);

        if (result != Result::OK) {
            return (int32_t) result;
        }

        // Typically, start the stream after querying some stream information, as well as some input from the user
        result = mStream->requestStart();
        return (int32_t) result;
    }

    // Call this from Activity onPause()
    void stopAudio() {
        // Stop, close and delete in case not already closed.
        lock_guard<mutex> lock(mLock);
        if (mStream) {
            mStream->stop();
            mStream->close();
            mStream.reset();
        }
    }

    DataCallbackResult
    onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) override {

        LOGI("onAudioReady called, numFrames value is %d", numFrames);

        float *floatData = (float *) audioData;
        for (int i = 0; i < numFrames; ++i) {
            float sampleValue = kAmplitude * sinf(mPhase);
            for (int j = 0; j < kChannelCount; j++) {
                floatData[i * kChannelCount + j] = sampleValue;
            }
            mPhase += mPhaseIncrement;
            if (mPhase >= kTwoPi) {
                mPhase -= kTwoPi;
            }
        }

        return DataCallbackResult::Continue;
    }

private:
    mutex mLock;
    shared_ptr<AudioStream> mStream;

    // Stream params
    static int constexpr kChannelCount = ChannelCount::Stereo;
    static int constexpr kSampleRate = 44100;

    // Wave params
    static float constexpr kAmplitude = 0.5f;
    static float constexpr kFrequency = 440;
    static float constexpr kPi = M_PI;
    static float constexpr kTwoPi = kPi * 2;
    static double constexpr mPhaseIncrement = kFrequency * kTwoPi / (double) kSampleRate;

    // Keeps track of where the wave is
    float mPhase = 0.0;
};

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_vimosoft_audioplayer_controller_AudioPlayerActivity_stringFromJNI(
        JNIEnv *env,
        jobject) {
    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jlong JNICALL
Java_com_vimosoft_audioplayer_controller_AudioPlayerActivity_startAudio(
        JNIEnv *env,
        jobject) {
    OboeSinePlayer *player = new OboeSinePlayer();
    player->startAudio();

    return reinterpret_cast<jlong>(player);
}


JNIEXPORT void JNICALL
Java_com_vimosoft_audioplayer_controller_AudioPlayerActivity_stopAudio(
        JNIEnv *env,
        jobject,
        jlong playerHandle) {
    OboeSinePlayer *player = reinterpret_cast<OboeSinePlayer *>(playerHandle);
    player->stopAudio();
    delete player;
}
}