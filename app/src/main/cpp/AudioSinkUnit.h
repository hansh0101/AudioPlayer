#ifndef AUDIOPLAYER_AUDIOSINKUNIT_H
#define AUDIOPLAYER_AUDIOSINKUNIT_H

#include <oboe/Oboe.h>
#include <android/log.h>

#include <thread>
#include <string>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"AudioSinkUnit",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"AudioSinkUnit",__VA_ARGS__)

using namespace std;

class AudioSinkUnit : public oboe::AudioStreamDataCallback {
public:
    AudioSinkUnit(int channelCount, int sampleRate);

    ~AudioSinkUnit();

    oboe::Result startAudio();

    void stopAudio();

    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames);

private:
    // member variables
    mutex mLock;
    shared_ptr<oboe::AudioStream> mStream;

    int mChannelCount;
    int mSampleRate;

    // variables for make sine PCM.
    float mAmplitude = 10000;
    float mFrequency = 440;
    float mPi = M_PI;
    float mTwoPi = mPi * 2;
    double mPhaseIncrement = mFrequency * mTwoPi / (double) (mSampleRate);
    float mPhase = 0.0f;

    string getThreadId();
};

#endif
