#include "AudioSinkUnit.h"

AudioSinkUnit::AudioSinkUnit(int channelCount, int sampleRate)
        : mChannelCount(channelCount), mSampleRate(sampleRate) {
    oboe::AudioStreamBuilder builder;
    // TODO - 파라미터로 받을 수 있도록 수정할 것
    oboe::Result result = builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setChannelCount(mChannelCount) // here
            ->setSampleRate(mSampleRate) // here
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Best)
            ->setFormat(oboe::AudioFormat::I16) // here
            ->setBufferCapacityInFrames(1048576) // here
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

AudioSinkUnit::~AudioSinkUnit() {
    stopAudio();
}

void AudioSinkUnit::startAudio(const void *buffer) {
    auto *audioBuffer = (int16_t *) buffer;
    mStream->write(audioBuffer, 1152, 1e9);
}

void AudioSinkUnit::stopAudio() {
    lock_guard<mutex> lock(mLock);

    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}