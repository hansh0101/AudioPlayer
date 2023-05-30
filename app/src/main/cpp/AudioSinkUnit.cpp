#include "AudioSinkUnit.h"

AudioSinkUnit::AudioSinkUnit(int channelCount, int sampleRate, int bitDepth)
        : mChannelCount(channelCount), mSampleRate(sampleRate) {
    oboe::AudioStreamBuilder builder;

    if (bitDepth == 32) {
        mFormat = oboe::AudioFormat::Float;
    } else if (bitDepth == 16) {
        mFormat = oboe::AudioFormat::I16;
    }

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
    LOGI("BytesPerFrame : %d", mStream->getBytesPerFrame());
    LOGI("BytesPerSample : %d", mStream->getBytesPerSample());
    LOGI("getBufferCapacityInFrames : %d", mStream->getBufferCapacityInFrames());
    LOGI("getBufferSizeInFrames : %d", mStream->getBufferSizeInFrames());

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