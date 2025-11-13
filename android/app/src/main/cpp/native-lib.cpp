#include <jni.h>
#include <android/log.h>
#include <memory>
#include <mutex>
#include <vector>

#include "EdgeProcessor.h"

namespace {
constexpr char kTag[] = "EdgeNative";
std::unique_ptr<EdgeProcessor> gProcessor;
std::mutex gProcessorMutex;

EdgeProcessor* processor() {
    std::lock_guard<std::mutex> lock(gProcessorMutex);
    if (!gProcessor) {
        gProcessor = std::make_unique<EdgeProcessor>();
    }
    return gProcessor.get();
}
}

extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgeviewer_nativebridge_NativeBridge_nativeConfigure(
        JNIEnv* env,
        jobject /*thiz*/,
        jint width,
        jint height) {
    processor()->configure(width, height);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_flam_edgeviewer_nativebridge_NativeBridge_nativeProcessFrame(
        JNIEnv* env,
        jobject /*thiz*/,
        jbyteArray frame,
        jint width,
        jint height,
        jlong /*timestampNs*/) {
    if (frame == nullptr) {
        return env->NewByteArray(0);
    }
    const jsize length = env->GetArrayLength(frame);
    std::vector<uint8_t> buffer(static_cast<size_t>(length));
    env->GetByteArrayRegion(frame, 0, length, reinterpret_cast<jbyte*>(buffer.data()));

    const auto processed = processor()->process(buffer, width, height);
    jbyteArray output = env->NewByteArray(static_cast<jsize>(processed.size()));
    if (!processed.empty()) {
        env->SetByteArrayRegion(
                output,
                0,
                static_cast<jsize>(processed.size()),
                reinterpret_cast<const jbyte*>(processed.data()));
    }
    return output;
}

extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgeviewer_nativebridge_NativeBridge_nativeSetMode(
        JNIEnv* env,
        jobject /*thiz*/,
        jboolean edgeMode) {
    processor()->setMode(edgeMode == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_flam_edgeviewer_nativebridge_NativeBridge_nativeRelease(
        JNIEnv* env,
        jobject /*thiz*/) {
    std::lock_guard<std::mutex> lock(gProcessorMutex);
    gProcessor.reset();
    __android_log_print(ANDROID_LOG_INFO, kTag, "Native pipeline released");
}
