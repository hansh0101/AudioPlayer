#include "AudioOutputUnit.h"

AudioOutputUnit::AudioOutputUnit() {
    oboe::Result result = builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setChannelCount(oboe::ChannelCount::Stereo)
            ->setSampleRate(44100)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Best)
            ->setFormat(oboe::AudioFormat::I16)
            ->openStream(stream);

    if (result != oboe::Result::OK) {
        LOGE("Error occurred when open stream : %s", oboe::convertToText(result));
    }

    result = stream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Error occurred when open stream : %s", oboe::convertToText(result));
    }
}

AudioOutputUnit::~AudioOutputUnit() {
    stop();
}

void AudioOutputUnit::requestPlayback(const void *buffer, int32_t numFrames) {
    stream->write(buffer, numFrames, 0);
}

void AudioOutputUnit::flush() {
    stream->requestFlush();
    stream->requestStart();
}

void AudioOutputUnit::stop() {
    lock_guard<mutex> lock(mutexLock);
    if (stream) {
        stream->stop();
        stream->close();
        stream.reset();
    }
}
