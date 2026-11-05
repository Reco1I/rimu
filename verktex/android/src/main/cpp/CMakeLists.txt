cmake_minimum_required(VERSION 3.18.1)

project(verktex_native CXX)

# The Vulkan backend, see verktex/docs/vulkan-backend.md and android/vulkan/VKNative.kt.
add_library(verktex_vulkan SHARED
        vulkan/vk_context.cpp
        vulkan/vk_jni.cpp)

target_compile_features(verktex_vulkan PRIVATE cxx_std_17)
target_compile_definitions(verktex_vulkan PRIVATE VK_USE_PLATFORM_ANDROID_KHR)
target_compile_options(verktex_vulkan PRIVATE -Wall -Wextra -Wno-unused-parameter -Wno-missing-field-initializers)

# libvulkan.so exists from API 24, the minimum SDK.
target_link_libraries(verktex_vulkan vulkan android log)
