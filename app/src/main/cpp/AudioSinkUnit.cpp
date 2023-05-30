#include "AudioSinkUnit.h"

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

AudioSinkUnit::~AudioSinkUnit() = default;

void AudioSinkUnit::startAudio(const void *buffer) {
    auto *audioBuffer = (int16_t *) buffer;
    mStream->write(audioBuffer, 1152, 1e9);
}

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