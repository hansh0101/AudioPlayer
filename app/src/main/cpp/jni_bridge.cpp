#include <jni.h>
#include <string>
#include <oboe/Oboe.h>
#include <cmath>

using namespace std;
using namespace oboe;

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
    static int constexpr kChannelCount = 2;
    static int constexpr kSampleRate = 48000;

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