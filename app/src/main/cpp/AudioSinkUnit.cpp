#include "AudioSinkUnit.h"

/**
 * AudioSinkUnit 객체를 구성한다.
 * @param channelCount : 출력 데이터의 채널 수
 * @param sampleRate : 1초당 재생할 샘플 수
 * @param bitDepth : 샘플 1개를 표현하는 데 사용되는 비트 수
 * @param isFloat : 샘플의 AudioFormat이 Float인지 나타내는 bool 변수
 */
AudioSinkUnit::AudioSinkUnit(int channelCount, int sampleRate, int bitDepth, bool isFloat)
        : mChannelCount(channelCount), mSampleRate(sampleRate) {
    oboe::AudioStreamBuilder builder;

    setFormat(bitDepth, isFloat);

    oboe::Result result = builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setChannelCount(mChannelCount)
            ->setSampleRate(mSampleRate)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Best)
            ->setFormat(mFormat)
            ->openStream(mStream);

    LOGI("SharingMode : %s", oboe::convertToText(mStream->getSharingMode()));
    LOGI("PerformanceMode : %s", oboe::convertToText(mStream->getPerformanceMode()));
    LOGI("ChannelCount : %d", mStream->getChannelCount());
    LOGI("SampleRate : %d", mStream->getSampleRate());
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
AudioSinkUnit::~AudioSinkUnit() = default;

/**
 * AudioSinkUnit을 통해 buffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력한다.
 * @param buffer : 재생할 오디오 데이터가 들어있는 버퍼
 * @param size : 재생할 오디오 데이터의 크기
 */
void AudioSinkUnit::outputAudio(const void *buffer, int32_t size) {
    auto *audioBuffer = (int16_t *) buffer;
    mStream->write(audioBuffer, getNumFrames(size), 1e9);
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

/**
 * 주어진 오디오 데이터의 크기를 통해 재생할 Frame 수를 계산해 반환한다.
 * @param size : 오디오 데이터의 크기(Bytes)
 * @return : 재생할 Frame 수
 */
int AudioSinkUnit::getNumFrames(int size) {
    int bitDepth;
    switch (mFormat) {
        case oboe::AudioFormat::Float:
        case oboe::AudioFormat::I32:
            bitDepth = 32;
            break;
        case oboe::AudioFormat::I24:
            bitDepth = 24;
            break;
        case oboe::AudioFormat::I16:
        default:
            bitDepth = 16;
    }

    int numFrames = size / (mChannelCount * (bitDepth / 8));
    return numFrames;
}