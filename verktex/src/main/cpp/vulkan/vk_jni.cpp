// The JNI surface of the Vulkan backend, see VKNative.kt. Calls are coarse: resources are created
// one call each and a whole frame is submitted as one command stream.

#include "vk_context.h"

#include <android/native_window_jni.h>
#include <jni.h>

#include <string>
#include <vector>

using verktex::Context;

namespace {

// The error of the last call that failed, read with lastError(). Only the draw thread calls in after
// the context is created.
std::string lastError;

Context *context(jlong handle) {
    return reinterpret_cast<Context *>(handle);
}

// Non-dispatchable handles are pointers on 64-bit ABIs and uint64_t on 32-bit ones.
template<typename T>
jlong toHandle(T value) {
    return static_cast<jlong>((uint64_t) value);
}

template<typename T>
T fromHandle(jlong value) {
    return (T) static_cast<uint64_t>(value);
}

} // namespace

#define VKNATIVE(name) Java_com_reco1l_verktex_android_vulkan_VKNative_##name

extern "C" {

JNIEXPORT jlong JNICALL VKNATIVE(createContext)(JNIEnv *, jclass, jboolean enableValidation) {
    auto *created = new Context();

    if (!created->initialize(enableValidation == JNI_TRUE, lastError)) {
        created->destroy();
        delete created;
        return 0;
    }
    return reinterpret_cast<jlong>(created);
}

JNIEXPORT void JNICALL VKNATIVE(destroyContext)(JNIEnv *, jclass, jlong handle) {
    Context *ctx = context(handle);
    ctx->destroy();
    delete ctx;
}

JNIEXPORT jstring JNICALL VKNATIVE(lastError)(JNIEnv *env, jclass) {
    return env->NewStringUTF(lastError.c_str());
}

JNIEXPORT jstring JNICALL VKNATIVE(deviceName)(JNIEnv *env, jclass, jlong handle) {
    return env->NewStringUTF(context(handle)->properties().deviceName);
}

JNIEXPORT jint JNICALL VKNATIVE(apiVersion)(JNIEnv *, jclass, jlong handle) {
    return static_cast<jint>(context(handle)->properties().apiVersion);
}

JNIEXPORT jint JNICALL VKNATIVE(maxImageDimension2D)(JNIEnv *, jclass, jlong handle) {
    return static_cast<jint>(context(handle)->properties().limits.maxImageDimension2D);
}

JNIEXPORT jint JNICALL VKNATIVE(minUniformBufferOffsetAlignment)(JNIEnv *, jclass, jlong handle) {
    return static_cast<jint>(context(handle)->properties().limits.minUniformBufferOffsetAlignment);
}

JNIEXPORT jint JNICALL VKNATIVE(ringSize)(JNIEnv *, jclass) {
    return static_cast<jint>(verktex::RING_SIZE);
}

JNIEXPORT jint JNICALL VKNATIVE(uniformRange)(JNIEnv *, jclass) {
    return static_cast<jint>(verktex::UNIFORM_RANGE);
}

JNIEXPORT jint JNICALL VKNATIVE(setSurface)(JNIEnv *env, jclass, jlong handle, jobject surface) {
    ANativeWindow *window = surface != nullptr ? ANativeWindow_fromSurface(env, surface) : nullptr;
    return context(handle)->setSurface(window, lastError);
}

JNIEXPORT jint JNICALL VKNATIVE(acquireFrame)(JNIEnv *, jclass, jlong handle, jint width, jint height) {
    return context(handle)->acquireFrame(static_cast<uint32_t>(width), static_cast<uint32_t>(height));
}

JNIEXPORT jobject JNICALL VKNATIVE(ringBuffer)(JNIEnv *env, jclass, jlong handle, jint frameIndex) {
    return env->NewDirectByteBuffer(context(handle)->ringMemory(static_cast<uint32_t>(frameIndex)),
                                    static_cast<jlong>(verktex::RING_SIZE));
}

JNIEXPORT void JNICALL VKNATIVE(submit)(JNIEnv *env, jclass, jlong handle, jobject commands, jint count) {
    auto *data = static_cast<const int32_t *>(env->GetDirectBufferAddress(commands));
    context(handle)->submit(data, count);
}

JNIEXPORT void JNICALL VKNATIVE(present)(JNIEnv *, jclass, jlong handle) {
    context(handle)->present();
}

JNIEXPORT jlong JNICALL VKNATIVE(createShaderModule)(JNIEnv *env, jclass, jlong handle, jbyteArray spirv) {
    jsize size = env->GetArrayLength(spirv);

    // Copied into words, SPIR-V is read as aligned 32-bit words.
    std::vector<uint32_t> words((static_cast<size_t>(size) + 3) / 4);
    env->GetByteArrayRegion(spirv, 0, size, reinterpret_cast<jbyte *>(words.data()));

    return toHandle(context(handle)->createShaderModule(words.data(), static_cast<size_t>(size)));
}

JNIEXPORT void JNICALL VKNATIVE(destroyShaderModule)(JNIEnv *, jclass, jlong handle, jlong module) {
    context(handle)->destroyShaderModule(fromHandle<VkShaderModule>(module));
}

JNIEXPORT jlong JNICALL VKNATIVE(createPipeline)(JNIEnv *env, jclass, jlong handle, jlong vertex, jlong fragment,
                                                jintArray vertexInput, jintArray blend, jintArray depth) {
    jsize inputCount = env->GetArrayLength(vertexInput);
    jint *input = env->GetIntArrayElements(vertexInput, nullptr);
    jint *factors = env->GetIntArrayElements(blend, nullptr);
    jint *depthState = env->GetIntArrayElements(depth, nullptr);

    VkPipeline pipeline = context(handle)->createPipeline(
            fromHandle<VkShaderModule>(vertex), fromHandle<VkShaderModule>(fragment),
            input, inputCount, factors, depthState);

    env->ReleaseIntArrayElements(vertexInput, input, JNI_ABORT);
    env->ReleaseIntArrayElements(blend, factors, JNI_ABORT);
    env->ReleaseIntArrayElements(depth, depthState, JNI_ABORT);
    return toHandle(pipeline);
}

JNIEXPORT void JNICALL VKNATIVE(destroyPipeline)(JNIEnv *, jclass, jlong handle, jlong pipeline) {
    context(handle)->destroyPipeline(fromHandle<VkPipeline>(pipeline));
}

JNIEXPORT jlong JNICALL VKNATIVE(createTexture)(JNIEnv *, jclass, jlong handle, jint width, jint height) {
    return reinterpret_cast<jlong>(context(handle)->createTexture(static_cast<uint32_t>(width), static_cast<uint32_t>(height)));
}

JNIEXPORT void JNICALL VKNATIVE(uploadTexture)(JNIEnv *env, jclass, jlong handle, jlong texture,
                                              jint x, jint y, jint width, jint height,
                                              jobject pixels, jint offset, jint size) {
    auto *data = static_cast<const uint8_t *>(env->GetDirectBufferAddress(pixels));
    if (data == nullptr) {
        return;
    }
    context(handle)->uploadTexture(reinterpret_cast<verktex::Texture *>(texture), x, y, width, height,
                                   data + offset, static_cast<size_t>(size));
}

JNIEXPORT void JNICALL VKNATIVE(destroyTexture)(JNIEnv *, jclass, jlong handle, jlong texture) {
    context(handle)->destroyTexture(reinterpret_cast<verktex::Texture *>(texture));
}

JNIEXPORT jfloat JNICALL VKNATIVE(gpuTime)(JNIEnv *, jclass, jlong handle) {
    return context(handle)->gpuTime();
}

} // extern "C"
