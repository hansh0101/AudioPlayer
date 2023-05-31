#include "AudioSinkUnit.h"

/**
 * AudioSinkUnit 객체를 구성한다.
 * @param channelCount : 출력 데이터의 채널 수
 * @param sampleRate : 1초당 재생할 샘플 수
 * @param bitDepth : 샘플 1개를 표현하는 데 사용되는 비트 수
 * @param isFloat : 샘플의 AudioFormat이 Float인지 나타내는 bool 변수
 */
AudioSinkUnit::AudioSinkUnit(int channelCount, int sampleRate, int bitDepth, bool isFloat)
        : mChannelCount(channelCount), mSampleRate(sampleRate), mBitDepth(bitDepth) {
    oboe::AudioStreamBuilder builder;

    setFormat(bitDepth, isFloat);

    oboe::Result result = builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setChannelCount(mChannelCount)
            ->setSampleRate(mSampleRate)
            ->setFormat(mFormat)
            ->setFramesPerCallback(1152)
            ->setDataCallback(this)
            ->openStream(mStream);

    LOGI("SharingMode : %s", oboe::convertToText(mStream->getSharingMode()));
    LOGI("PerformanceMode : %s", oboe::convertToText(mStream->getPerformanceMode()));
    LOGI("ChannelCount : %d", mStream->getChannelCount());
    LOGI("SampleRate : %d", mStream->getSampleRate());
    LOGI("Frames per callback : %d", mStream->getFramesPerCallback());
    LOGI("Frames per burst : %d", mStream->getFramesPerBurst());
    LOGI("Format : %s", oboe::convertToText(mStream->getFormat()));

    if (result != oboe::Result::OK) {
        LOGE("Error occurred when open stream : %s", oboe::convertToText(result));
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Error occurred when open stream : %s", oboe::convertToText(result));
    }
}

/**
 * AudioSinkUnit 객체 사용을 마친 후 리소스를 정리한다.
 */
AudioSinkUnit::~AudioSinkUnit() {
    mStream->close();
    while (!q.empty()) q.pop();
}

/**
 * AudioSinkUnit을 통해 buffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력한다.
 * @param buffer : 재생할 오디오 데이터가 들어있는 버퍼
 * @param size : 재생할 오디오 데이터의 크기
 */
void AudioSinkUnit::requestPlayback(void *buffer, int32_t size) {
    AudioDataInfo audioDataInfo = AudioDataInfo{buffer, size};
    q.push(audioDataInfo);
}

/**
 * AudioSinkUnit 객체의 mFormat을 설정한다.
 * @param bitDepth : 샘플 1개를 표현하는 데 사용되는 비트 수
 * @param isFloat : 샘플의 AudioFormat이 Flaot인지 나타내는 bool 변수
 */
void AudioSinkUnit::setFormat(int bitDepth, bool isFloat) {
    if (bitDepth == 32) {
        if (isFloat) {
            mFormat = oboe::AudioFormat::Float;
        } else {
            mFormat = oboe::AudioFormat::I32;
        }
    } else if (bitDepth == 24) {
        mFormat = oboe::AudioFormat::I24;
    } else if (bitDepth == 16) {
        mFormat = oboe::AudioFormat::I16;
    } else {
        mFormat = oboe::AudioFormat::Unspecified;
    }
}

oboe::DataCallbackResult
AudioSinkUnit::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    if (!q.empty()) {
        AudioDataInfo audioDataInfo = q.front();
        q.pop();

        LOGI("address : %p", audioDataInfo.audioData);
        memcpy(audioData, audioDataInfo.audioData, audioDataInfo.size);
    } else {
        memset(audioData, 0, numFrames * 2 /* mChannelCount */ * 2 /* mBitDepth / 8 */);
    }

    return oboe::DataCallbackResult::Continue;
}
