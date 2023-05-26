#include "AudioSinkUnit.h"

AudioSinkUnit::AudioSinkUnit(int channelCount, int sampleRate)
        : mChannelCount(channelCount), mSampleRate(sampleRate) {
    oboe::AudioStreamBuilder builder;
    // TODO - AudioFormat도 파라미터로 받을 수 있도록 수정할 것
    oboe::Result result = builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setChannelCount(mChannelCount)
            ->setSampleRate(mSampleRate)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Best)
            ->setFormat(oboe::AudioFormat::I16) // here
            ->setDataCallback(this)
            ->openStream(mStream);

    LOGI("SharingMode : %s", oboe::convertToText(mStream->getSharingMode()));
    LOGI("PerformanceMode : %s", oboe::convertToText(mStream->getPerformanceMode()));
    LOGI("ChannelCount : %d", mStream->getChannelCount());
    LOGI("SampleRate : %d", mStream->getSampleRate());
    LOGI("Format : %s", oboe::convertToText(mStream->getFormat()));

    if (result != oboe::Result::OK) {
        LOGE("Error occurred when open stream : %s", oboe::convertToText(result));
    }
}

AudioSinkUnit::~AudioSinkUnit() {
    stopAudio();
}

oboe::Result AudioSinkUnit::startAudio() {
//    this_thread::sleep_for(chrono::microseconds(1000));

    lock_guard<mutex> lock(mLock);

    oboe::Result result = oboe::Result::OK;

    if (mStream->getState() != oboe::StreamState::Starting &&
        mStream->getState() != oboe::StreamState::Started) {
        result = mStream->requestStart();
    }
    return result;
}

void AudioSinkUnit::stopAudio() {
    lock_guard<mutex> lock(mLock);

    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}

oboe::DataCallbackResult
AudioSinkUnit::onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    auto *data = static_cast<int16_t *>(audioData);

    for (int frame = 0; frame < numFrames; frame++) {
        auto sampleValue = static_cast<int16_t>(mAmplitude * sinf(mPhase));

        for (int channel = 0; channel < mChannelCount; channel++) {
            data[frame * mChannelCount + channel] = sampleValue;
        }

        mPhase += mPhaseIncrement;
        if (mPhase >= mTwoPi) {
            mPhase -= mTwoPi;
        }
    }

    return oboe::DataCallbackResult::Continue;
}

string AudioSinkUnit::getThreadId() {
    thread::id threadId = this_thread::get_id();

    ostringstream oss;
    oss << threadId;
    string threadIdString = oss.str();

    return threadIdString;
}
