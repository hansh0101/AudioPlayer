#ifndef AUDIOPLAYER_AUDIOOUTPUTUNIT_H
#define AUDIOPLAYER_AUDIOOUTPUTUNIT_H

#include <oboe/Oboe.h>
#include <android/log.h>
#include <queue>
#include <cstdlib>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"AudioOutputUnit",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"AudioOutputUnit",__VA_ARGS__)

using namespace std;

class AudioOutputUnit {
public:
    AudioOutputUnit();

    ~AudioOutputUnit();

    void requestPlayback(const void *buffer, int32_t numFrames);

    void flush();

private:
    struct BufferInfo {
        void *buffer;
        int32_t numFrames;
    };

    mutex mutexLock;
    oboe::AudioStreamBuilder builder;
    shared_ptr<oboe::AudioStream> stream;
    queue<BufferInfo> bufferQueue;

    void stop();
};

#endif
