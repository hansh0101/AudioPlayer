#ifndef AUDIOPLAYER_AUDIOSINKUNIT_H
#define AUDIOPLAYER_AUDIOSINKUNIT_H

#include <oboe/Oboe.h>
#include <android/log.h>
#include <queue>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"AudioSinkUnit",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"AudioSinkUnit",__VA_ARGS__)

using namespace std;

/**
 * Oboe의 AudioStream을 통해 출력을 처리하는 객체.
 */
class AudioSinkUnit : public oboe::AudioStreamDataCallback {
public:
    /**
     * AudioSinkUnit 객체를 구성한다.
     * @param channelCount : 출력 데이터의 채널 수
     * @param sampleRate : 1초당 재생할 샘플 수
     * @param bitDepth : 샘플 1개를 표현하는 데 사용되는 비트 수
     * @param isFloat : 샘플의 AudioFormat이 Float인지 나타내는 bool 변수
     */
    AudioSinkUnit(int channelCount, int sampleRate, int bitDepth, bool isFloat);

    /**
     * AudioSinkUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    ~AudioSinkUnit();

    /**
     * AudioSinkUnit을 통해 buffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력을 요청한다.
     * @param audioDataPtr : 재생할 오디오 데이터의 주소를 가리키는 포인터
     * @param size : 재생할 오디오 데이터의 크기
     */
    void requestPlayback(void *audioDataPtr, int32_t size);

    /**
     * 오디오 재생 시작을 요청한다.
     */
    void requestStart();

    /**
     * 오디오 재생 중지를 요청한다.
     */
    void requestPause();

    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    /**
     * AudioStream 객체에 대한 공유 포인터.
     */
    shared_ptr<oboe::AudioStream> mStream;

    /**
     * 출력 데이터의 채널 수.
     */
    int mChannelCount;

    /**
     * 1초당 재생할 샘플 수.
     */
    int mSampleRate;

    /**
     * 출력 데이터의 AudioFormat.
     */
    oboe::AudioFormat mFormat = oboe::AudioFormat::Unspecified;

    /**
     * AudioSinkUnit 객체의 mFormat을 설정한다.
     * @param bitDepth : 샘플 1개를 표현하는 데 사용되는 비트 수
     * @param isFloat : 샘플의 AudioFormat이 Flaot인지 나타내는 bool 변수
     */
    void setFormat(int bitDepth, bool isFloat);

    struct AudioDataInfo {
        void *audioDataPtr;
        int size;
    };

    queue<AudioDataInfo> q;
};

#endif
