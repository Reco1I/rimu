#include "vk_context.h"

#include <android/log.h>
#include <vulkan/vulkan_android.h>

#include <algorithm>
#include <cstring>
#include <dlfcn.h>

#define LOG_TAG "VerktexVulkan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace verktex {

namespace {

const char *VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation";

constexpr uint32_t TEXTURE_SETS_PER_POOL = 128;
constexpr uint32_t INITIAL_QUADS = 4096;

VKAPI_ATTR VkBool32 VKAPI_CALL onValidationMessage(
        VkDebugUtilsMessageSeverityFlagBitsEXT severity,
        VkDebugUtilsMessageTypeFlagsEXT,
        const VkDebugUtilsMessengerCallbackDataEXT *data,
        void *) {
    if (severity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) {
        LOGE("Validation: %s", data->pMessage);
    } else if (severity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
        LOGW("Validation: %s", data->pMessage);
    }
    return VK_FALSE;
}

bool hasLayer(const char *name) {
    uint32_t count = 0;
    vkEnumerateInstanceLayerProperties(&count, nullptr);
    std::vector<VkLayerProperties> layers(count);
    vkEnumerateInstanceLayerProperties(&count, layers.data());
    return std::any_of(layers.begin(), layers.end(), [&](auto &layer) { return strcmp(layer.layerName, name) == 0; });
}

bool hasInstanceExtension(const char *name, const char *layer) {
    uint32_t count = 0;
    vkEnumerateInstanceExtensionProperties(layer, &count, nullptr);
    std::vector<VkExtensionProperties> extensions(count);
    vkEnumerateInstanceExtensionProperties(layer, &count, extensions.data());
    return std::any_of(extensions.begin(), extensions.end(), [&](auto &e) { return strcmp(e.extensionName, name) == 0; });
}

bool hasDeviceExtension(VkPhysicalDevice device, const char *name) {
    uint32_t count = 0;
    vkEnumerateDeviceExtensionProperties(device, nullptr, &count, nullptr);
    std::vector<VkExtensionProperties> extensions(count);
    vkEnumerateDeviceExtensionProperties(device, nullptr, &count, extensions.data());
    return std::any_of(extensions.begin(), extensions.end(), [&](auto &e) { return strcmp(e.extensionName, name) == 0; });
}

// AHardwareBuffer functions exist from API 26, the minimum SDK is 24: they're looked up at runtime.
using DescribeHardwareBuffer = void (*)(const AHardwareBuffer *, AHardwareBuffer_Desc *);

DescribeHardwareBuffer describeHardwareBuffer() {
    static auto function = reinterpret_cast<DescribeHardwareBuffer>(
            dlsym(dlopen("libandroid.so", RTLD_NOW), "AHardwareBuffer_describe"));
    return function;
}

bool isYcbcrFormat(VkFormat format) {
    return format >= VK_FORMAT_G8B8G8R8_422_UNORM && format <= VK_FORMAT_G16_B16_R16_3PLANE_444_UNORM;
}

std::string resultName(VkResult result) {
    return "VkResult " + std::to_string(static_cast<int>(result));
}

void imageBarrier(VkCommandBuffer cmd, VkImage image, VkImageAspectFlags aspect,
                  VkImageLayout from, VkImageLayout to,
                  VkPipelineStageFlags srcStage, VkAccessFlags srcAccess,
                  VkPipelineStageFlags dstStage, VkAccessFlags dstAccess) {
    VkImageMemoryBarrier barrier{VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};
    barrier.srcAccessMask = srcAccess;
    barrier.dstAccessMask = dstAccess;
    barrier.oldLayout = from;
    barrier.newLayout = to;
    barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.image = image;
    barrier.subresourceRange = {aspect, 0, 1, 0, 1};
    vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, 0, nullptr, 0, nullptr, 1, &barrier);
}

} // namespace


//region Initialization

bool Context::initialize(bool enableValidation, std::string &error) {
    std::lock_guard lock(mutex);

    // vkEnumerateInstanceVersion only exists from Vulkan 1.1 on.
    uint32_t instanceVersion = VK_API_VERSION_1_0;
    auto enumerateVersion = reinterpret_cast<PFN_vkEnumerateInstanceVersion>(
            vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion"));
    if (enumerateVersion != nullptr) {
        enumerateVersion(&instanceVersion);
    }

    std::vector<const char *> layers;
    std::vector<const char *> extensions = {VK_KHR_SURFACE_EXTENSION_NAME, VK_KHR_ANDROID_SURFACE_EXTENSION_NAME};

    bool validation = enableValidation && hasLayer(VALIDATION_LAYER);
    if (enableValidation && !validation) {
        LOGW("The validation layer isn't available, running without it.");
    }
    bool debugUtils = false;
    if (validation) {
        layers.push_back(VALIDATION_LAYER);
        if (hasInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME, VALIDATION_LAYER) ||
            hasInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME, nullptr)) {
            extensions.push_back(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            debugUtils = true;
        }
    }

    VkApplicationInfo application{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    application.pApplicationName = "osu!droid";
    application.pEngineName = "Verktex";
    application.apiVersion = instanceVersion >= VK_API_VERSION_1_1 ? VK_API_VERSION_1_1 : VK_API_VERSION_1_0;
    instanceApiVersion = application.apiVersion;

    VkInstanceCreateInfo instanceInfo{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
    instanceInfo.pApplicationInfo = &application;
    instanceInfo.enabledLayerCount = static_cast<uint32_t>(layers.size());
    instanceInfo.ppEnabledLayerNames = layers.data();
    instanceInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    instanceInfo.ppEnabledExtensionNames = extensions.data();

    VkResult result = vkCreateInstance(&instanceInfo, nullptr, &instance);
    if (result != VK_SUCCESS) {
        error = "vkCreateInstance failed: " + resultName(result);
        return false;
    }

    if (debugUtils) {
        auto createMessenger = reinterpret_cast<PFN_vkCreateDebugUtilsMessengerEXT>(
                vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT"));
        if (createMessenger != nullptr) {
            VkDebugUtilsMessengerCreateInfoEXT messengerInfo{VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT};
            messengerInfo.messageSeverity = VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
            messengerInfo.messageType = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
            messengerInfo.pfnUserCallback = onValidationMessage;
            createMessenger(instance, &messengerInfo, nullptr, &messenger);
        }
    }
    LOGI("Instance created, validation %s.", validation ? "on" : "off");

    if (!createDeviceObjects(error) || !createFrames(error)) {
        return false;
    }

    // Bound for shaders that sample while the draw has no texture, e.g. UI boxes, which don't read it.
    whiteTexture = createTexture(1, 1);
    const uint32_t white = 0xFFFFFFFF;
    uploadTexture(whiteTexture, 0, 0, 1, 1, &white, sizeof(white));

    if (!ensureQuadIndices(INITIAL_QUADS)) {
        error = "Couldn't create the quad index buffer.";
        return false;
    }
    return true;
}

bool Context::createDeviceObjects(std::string &error) {
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

    uint32_t timestampBits = 0;

    // Android devices have a single GPU, the first one with a graphics queue is taken.
    for (auto candidate : devices) {
        uint32_t familyCount = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, &familyCount, nullptr);
        std::vector<VkQueueFamilyProperties> families(familyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(candidate, &familyCount, families.data());

        for (uint32_t i = 0; i < familyCount; i++) {
            if (families[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) {
                physical = candidate;
                queueFamily = i;
                timestampBits = families[i].timestampValidBits;
                break;
            }
        }
        if (physical != VK_NULL_HANDLE) break;
    }

    if (physical == VK_NULL_HANDLE) {
        error = "No Vulkan device with a graphics queue.";
        return false;
    }

    vkGetPhysicalDeviceProperties(physical, &physicalProperties);
    vkGetPhysicalDeviceMemoryProperties(physical, &memoryProperties);

    std::vector<const char *> extensions = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};

    // A negative viewport height flips Y like OpenGL, it's core in 1.1 and VK_KHR_maintenance1 before.
    if (physicalProperties.apiVersion < VK_API_VERSION_1_1) {
        if (!hasDeviceExtension(physical, VK_KHR_MAINTENANCE1_EXTENSION_NAME)) {
            error = "The device needs Vulkan 1.1 or VK_KHR_maintenance1.";
            return false;
        }
        extensions.push_back(VK_KHR_MAINTENANCE1_EXTENSION_NAME);
    }

    // Video frames are imported from hardware buffers and sampled through a YCbCr conversion, which
    // needs Vulkan 1.1 (on both sides) and the Android import extensions. Without them, videos aren't shown.
    VkPhysicalDeviceSamplerYcbcrConversionFeatures ycbcrFeatures{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SAMPLER_YCBCR_CONVERSION_FEATURES};
    if (instanceApiVersion >= VK_API_VERSION_1_1 && physicalProperties.apiVersion >= VK_API_VERSION_1_1 &&
        describeHardwareBuffer() != nullptr &&
        hasDeviceExtension(physical, VK_ANDROID_EXTERNAL_MEMORY_ANDROID_HARDWARE_BUFFER_EXTENSION_NAME) &&
        hasDeviceExtension(physical, VK_EXT_QUEUE_FAMILY_FOREIGN_EXTENSION_NAME)) {

        auto getFeatures2 = reinterpret_cast<PFN_vkGetPhysicalDeviceFeatures2>(
                vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFeatures2"));

        if (getFeatures2 != nullptr) {
            VkPhysicalDeviceFeatures2 features{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2};
            features.pNext = &ycbcrFeatures;
            getFeatures2(physical, &features);
            externalSupported = ycbcrFeatures.samplerYcbcrConversion == VK_TRUE;
        }
    }
    if (externalSupported) {
        extensions.push_back(VK_ANDROID_EXTERNAL_MEMORY_ANDROID_HARDWARE_BUFFER_EXTENSION_NAME);
        extensions.push_back(VK_EXT_QUEUE_FAMILY_FOREIGN_EXTENSION_NAME);
    } else {
        LOGW("Video frames can't be imported, videos aren't shown.");
    }

    float priority = 1.0f;
    VkDeviceQueueCreateInfo queueInfo{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
    queueInfo.queueFamilyIndex = queueFamily;
    queueInfo.queueCount = 1;
    queueInfo.pQueuePriorities = &priority;

    VkDeviceCreateInfo deviceInfo{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
    if (externalSupported) {
        ycbcrFeatures.pNext = nullptr;
        deviceInfo.pNext = &ycbcrFeatures;
    }
    deviceInfo.queueCreateInfoCount = 1;
    deviceInfo.pQueueCreateInfos = &queueInfo;
    deviceInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    deviceInfo.ppEnabledExtensionNames = extensions.data();

    VkResult result = vkCreateDevice(physical, &deviceInfo, nullptr, &device);
    if (result != VK_SUCCESS) {
        error = "vkCreateDevice failed: " + resultName(result);
        return false;
    }
    vkGetDeviceQueue(device, queueFamily, 0, &queue);

    if (externalSupported) {
        getHardwareBufferProperties = reinterpret_cast<PFN_vkGetAndroidHardwareBufferPropertiesANDROID>(
                vkGetDeviceProcAddr(device, "vkGetAndroidHardwareBufferPropertiesANDROID"));
        createSamplerYcbcrConversion = reinterpret_cast<PFN_vkCreateSamplerYcbcrConversion>(
                vkGetDeviceProcAddr(device, "vkCreateSamplerYcbcrConversion"));
        destroySamplerYcbcrConversion = reinterpret_cast<PFN_vkDestroySamplerYcbcrConversion>(
                vkGetDeviceProcAddr(device, "vkDestroySamplerYcbcrConversion"));
        externalSupported = getHardwareBufferProperties && createSamplerYcbcrConversion && destroySamplerYcbcrConversion;
    }

    LOGI("Using %s, Vulkan %u.%u.%u.", physicalProperties.deviceName,
         VK_VERSION_MAJOR(physicalProperties.apiVersion), VK_VERSION_MINOR(physicalProperties.apiVersion),
         VK_VERSION_PATCH(physicalProperties.apiVersion));

    // The depth format, the OpenGL ES backends ask for 16 bits of depth and 8 of stencil.
    for (auto candidate : {VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D16_UNORM}) {
        VkFormatProperties properties;
        vkGetPhysicalDeviceFormatProperties(physical, candidate, &properties);
        if (properties.optimalTilingFeatures & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) {
            depthFormat = candidate;
            break;
        }
    }
    if (depthFormat == VK_FORMAT_UNDEFINED) {
        error = "No depth format.";
        return false;
    }

    // Set 0: the vertex and fragment uniform blocks, slices of the frame's ring. Set 1: the
    // texture, see GLSL450VulkanTranslator.
    VkDescriptorSetLayoutBinding uniformBindings[2]{};
    uniformBindings[0].binding = 0;
    uniformBindings[0].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
    uniformBindings[0].descriptorCount = 1;
    uniformBindings[0].stageFlags = VK_SHADER_STAGE_VERTEX_BIT;
    uniformBindings[1].binding = 1;
    uniformBindings[1].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
    uniformBindings[1].descriptorCount = 1;
    uniformBindings[1].stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;

    VkDescriptorSetLayoutCreateInfo uniformLayoutInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
    uniformLayoutInfo.bindingCount = 2;
    uniformLayoutInfo.pBindings = uniformBindings;
    vkCreateDescriptorSetLayout(device, &uniformLayoutInfo, nullptr, &uniformSetLayout);

    VkDescriptorSetLayoutBinding samplerBinding{};
    samplerBinding.binding = 0;
    samplerBinding.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    samplerBinding.descriptorCount = 1;
    samplerBinding.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;

    VkDescriptorSetLayoutCreateInfo samplerLayoutInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
    samplerLayoutInfo.bindingCount = 1;
    samplerLayoutInfo.pBindings = &samplerBinding;
    vkCreateDescriptorSetLayout(device, &samplerLayoutInfo, nullptr, &samplerSetLayout);

    VkDescriptorSetLayout setLayouts[] = {uniformSetLayout, samplerSetLayout};
    VkPipelineLayoutCreateInfo pipelineLayoutInfo{VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO};
    pipelineLayoutInfo.setLayoutCount = 2;
    pipelineLayoutInfo.pSetLayouts = setLayouts;
    vkCreatePipelineLayout(device, &pipelineLayoutInfo, nullptr, &pipelineLayout);

    VkDescriptorPoolSize poolSize{VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, 2 * FRAMES_IN_FLIGHT};
    VkDescriptorPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};
    poolInfo.maxSets = FRAMES_IN_FLIGHT;
    poolInfo.poolSizeCount = 1;
    poolInfo.pPoolSizes = &poolSize;
    vkCreateDescriptorPool(device, &poolInfo, nullptr, &uniformPool);

    VkPipelineCacheCreateInfo cacheInfo{VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO};
    vkCreatePipelineCache(device, &cacheInfo, nullptr, &pipelineCache);

    // Linear and clamped, as the OpenGL ES textures.
    VkSamplerCreateInfo samplerInfo{VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO};
    samplerInfo.magFilter = VK_FILTER_LINEAR;
    samplerInfo.minFilter = VK_FILTER_LINEAR;
    samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.maxLod = 0.0f;
    vkCreateSampler(device, &samplerInfo, nullptr, &sampler);

    if (timestampBits > 0 && physicalProperties.limits.timestampComputeAndGraphics) {
        VkQueryPoolCreateInfo queryInfo{VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO};
        queryInfo.queryType = VK_QUERY_TYPE_TIMESTAMP;
        queryInfo.queryCount = 2 * FRAMES_IN_FLIGHT;
        vkCreateQueryPool(device, &queryInfo, nullptr, &timestamps);
    }

    return true;
}

uint32_t Context::findMemoryType(uint32_t typeBits, VkMemoryPropertyFlags properties) const {
    for (uint32_t i = 0; i < memoryProperties.memoryTypeCount; i++) {
        if ((typeBits & (1u << i)) && (memoryProperties.memoryTypes[i].propertyFlags & properties) == properties) {
            return i;
        }
    }
    return UINT32_MAX;
}

// Host visible and coherent: written directly by the CPU. Mobile GPUs share the memory, there is
// no faster device local copy to make.
bool Context::createBuffer(VkDeviceSize size, VkBufferUsageFlags usage, Buffer &buffer) {
    VkBufferCreateInfo bufferInfo{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO};
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    if (vkCreateBuffer(device, &bufferInfo, nullptr, &buffer.buffer) != VK_SUCCESS) {
        return false;
    }

    VkMemoryRequirements requirements;
    vkGetBufferMemoryRequirements(device, buffer.buffer, &requirements);

    uint32_t memoryType = findMemoryType(requirements.memoryTypeBits,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

    VkMemoryAllocateInfo memoryInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
    memoryInfo.allocationSize = requirements.size;
    memoryInfo.memoryTypeIndex = memoryType;

    if (memoryType == UINT32_MAX || vkAllocateMemory(device, &memoryInfo, nullptr, &buffer.memory) != VK_SUCCESS) {
        vkDestroyBuffer(device, buffer.buffer, nullptr);
        buffer = Buffer{};
        return false;
    }

    vkBindBufferMemory(device, buffer.buffer, buffer.memory, 0);
    vkMapMemory(device, buffer.memory, 0, size, 0, &buffer.mapped);
    buffer.size = size;
    return true;
}

void Context::destroyBuffer(Buffer &buffer) {
    if (buffer.mapped != nullptr) vkUnmapMemory(device, buffer.memory);
    if (buffer.buffer != VK_NULL_HANDLE) vkDestroyBuffer(device, buffer.buffer, nullptr);
    if (buffer.memory != VK_NULL_HANDLE) vkFreeMemory(device, buffer.memory, nullptr);
    buffer = Buffer{};
}

bool Context::createFrames(std::string &error) {
    if (UNIFORM_RANGE > physicalProperties.limits.maxUniformBufferRange) {
        error = "maxUniformBufferRange is too small.";
        return false;
    }

    for (auto &frame : frames) {
        VkCommandPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};
        poolInfo.flags = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
        poolInfo.queueFamilyIndex = queueFamily;
        vkCreateCommandPool(device, &poolInfo, nullptr, &frame.commandPool);

        VkCommandBufferAllocateInfo allocateInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};
        allocateInfo.commandPool = frame.commandPool;
        allocateInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        allocateInfo.commandBufferCount = 1;
        vkAllocateCommandBuffers(device, &allocateInfo, &frame.commandBuffer);

        VkFenceCreateInfo fenceInfo{VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
        fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;
        vkCreateFence(device, &fenceInfo, nullptr, &frame.fence);

        VkSemaphoreCreateInfo semaphoreInfo{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
        vkCreateSemaphore(device, &semaphoreInfo, nullptr, &frame.imageAvailable);

        if (!createBuffer(RING_SIZE, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, frame.ring)) {
            error = "Couldn't create the frame ring.";
            return false;
        }

        VkDescriptorSetAllocateInfo setInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
        setInfo.descriptorPool = uniformPool;
        setInfo.descriptorSetCount = 1;
        setInfo.pSetLayouts = &uniformSetLayout;
        vkAllocateDescriptorSets(device, &setInfo, &frame.uniformSet);

        VkDescriptorBufferInfo ringInfo{frame.ring.buffer, 0, UNIFORM_RANGE};
        VkWriteDescriptorSet writes[2]{};
        for (uint32_t binding = 0; binding < 2; binding++) {
            writes[binding].sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
            writes[binding].dstSet = frame.uniformSet;
            writes[binding].dstBinding = binding;
            writes[binding].descriptorCount = 1;
            writes[binding].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
            writes[binding].pBufferInfo = &ringInfo;
        }
        vkUpdateDescriptorSets(device, 2, writes, 0, nullptr);
    }

    return true;
}

bool Context::ensureQuadIndices(uint32_t quads) {
    if (quads <= quadCapacity) {
        return true;
    }

    uint32_t capacity = std::max(quads, quadCapacity * 2);
    Buffer buffer;
    if (!createBuffer(static_cast<VkDeviceSize>(capacity) * 6 * sizeof(uint32_t), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, buffer)) {
        LOGE("Couldn't create a quad index buffer for %u quads.", capacity);
        return false;
    }

    // Two triangles per quad of four vertices: top left, top right, bottom right, bottom left.
    auto *indices = static_cast<uint32_t *>(buffer.mapped);
    for (uint32_t quad = 0; quad < capacity; quad++) {
        uint32_t first = quad * 4;
        uint32_t *at = indices + quad * 6;
        at[0] = first;
        at[1] = first + 1;
        at[2] = first + 2;
        at[3] = first;
        at[4] = first + 2;
        at[5] = first + 3;
    }

    // Frames still in flight may use the old buffer.
    if (quadIndices.buffer != VK_NULL_HANDLE) {
        Buffer old = quadIndices;
        deferDestroy([this, old]() mutable { destroyBuffer(old); });
    }
    quadIndices = buffer;
    quadCapacity = capacity;
    return true;
}

//endregion


//region Render passes and swapchain

bool Context::createRenderPasses() {
    destroyRenderPasses();

    for (int load = 0; load < 2; load++) {
        for (int storeDepth = 0; storeDepth < 2; storeDepth++) {
            VkAttachmentDescription attachments[2]{};

            // Color: cleared by the first pass, loaded by the ones after a capture. The layout stays
            // COLOR_ATTACHMENT_OPTIMAL between them, the transitions to copy and present are barriers.
            attachments[0].format = surfaceFormat;
            attachments[0].samples = VK_SAMPLE_COUNT_1_BIT;
            attachments[0].loadOp = load ? VK_ATTACHMENT_LOAD_OP_LOAD : VK_ATTACHMENT_LOAD_OP_CLEAR;
            attachments[0].storeOp = VK_ATTACHMENT_STORE_OP_STORE;
            attachments[0].stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
            attachments[0].stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
            attachments[0].initialLayout = load ? VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_UNDEFINED;
            attachments[0].finalLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

            // Depth: stored only when a later pass of the frame loads it, which saves writing it back
            // to memory on tilers.
            attachments[1].format = depthFormat;
            attachments[1].samples = VK_SAMPLE_COUNT_1_BIT;
            attachments[1].loadOp = load ? VK_ATTACHMENT_LOAD_OP_LOAD : VK_ATTACHMENT_LOAD_OP_CLEAR;
            attachments[1].storeOp = storeDepth ? VK_ATTACHMENT_STORE_OP_STORE : VK_ATTACHMENT_STORE_OP_DONT_CARE;
            attachments[1].stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
            attachments[1].stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
            attachments[1].initialLayout = load ? VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL : VK_IMAGE_LAYOUT_UNDEFINED;
            attachments[1].finalLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;

            VkAttachmentReference colorReference{0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL};
            VkAttachmentReference depthReference{1, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL};

            VkSubpassDescription subpass{};
            subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
            subpass.colorAttachmentCount = 1;
            subpass.pColorAttachments = &colorReference;
            subpass.pDepthStencilAttachment = &depthReference;

            // Waits for the acquired image (its semaphore is waited at the color output stage), the
            // previous frame's depth use and copies into the image before this pass.
            VkSubpassDependency dependency{};
            dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
            dependency.dstSubpass = 0;
            dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT |
                                      VK_PIPELINE_STAGE_TRANSFER_BIT;
            dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            dependency.srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT |
                                       VK_ACCESS_TRANSFER_READ_BIT;
            dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
                                       VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;

            VkRenderPassCreateInfo info{VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO};
            info.attachmentCount = 2;
            info.pAttachments = attachments;
            info.subpassCount = 1;
            info.pSubpasses = &subpass;
            info.dependencyCount = 1;
            info.pDependencies = &dependency;

            if (vkCreateRenderPass(device, &info, nullptr, &renderPasses[load][storeDepth]) != VK_SUCCESS) {
                return false;
            }
        }
    }
    return true;
}

void Context::destroyRenderPasses() {
    for (auto &row : renderPasses) {
        for (auto &pass : row) {
            if (pass != VK_NULL_HANDLE) vkDestroyRenderPass(device, pass, nullptr);
            pass = VK_NULL_HANDLE;
        }
    }
}

int Context::setSurface(ANativeWindow *newWindow, std::string &error) {
    std::lock_guard lock(mutex);

    vkDeviceWaitIdle(device);
    destroySwapchain(false);
    destroyDepthImage();

    if (surface != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance, surface, nullptr);
        surface = VK_NULL_HANDLE;
    }
    if (window != nullptr) {
        ANativeWindow_release(window);
        window = nullptr;
    }

    if (newWindow == nullptr) {
        return 0;
    }
    window = newWindow;

    VkAndroidSurfaceCreateInfoKHR surfaceInfo{VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR};
    surfaceInfo.window = window;
    VkResult result = vkCreateAndroidSurfaceKHR(instance, &surfaceInfo, nullptr, &surface);
    if (result != VK_SUCCESS) {
        error = "vkCreateAndroidSurfaceKHR failed: " + resultName(result);
        return -1;
    }

    VkBool32 supported = VK_FALSE;
    vkGetPhysicalDeviceSurfaceSupportKHR(physical, queueFamily, surface, &supported);
    if (!supported) {
        error = "The graphics queue can't present to the window.";
        return -1;
    }

    uint32_t formatCount = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, &formatCount, nullptr);
    std::vector<VkSurfaceFormatKHR> formats(formatCount);
    vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, &formatCount, formats.data());

    if (formats.empty()) {
        error = "The window has no surface formats.";
        return -1;
    }

    // UNORM like OpenGL ES' default framebuffer: the shaders output colors without sRGB encoding.
    VkFormat format = formats[0].format;
    for (auto &candidate : formats) {
        if (candidate.format == VK_FORMAT_R8G8B8A8_UNORM || candidate.format == VK_FORMAT_B8G8R8A8_UNORM) {
            format = candidate.format;
            break;
        }
    }

    VkSurfaceCapabilitiesKHR capabilities;
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical, surface, &capabilities);
    canCapture = (capabilities.supportedUsageFlags & VK_IMAGE_USAGE_TRANSFER_SRC_BIT) != 0;
    if (!canCapture) {
        LOGW("The window's images can't be copied, framebuffer captures are skipped.");
    }

    if (format == surfaceFormat && renderPasses[0][0] != VK_NULL_HANDLE) {
        return 0;
    }

    surfaceFormat = format;
    if (!createRenderPasses()) {
        error = "vkCreateRenderPass failed.";
        return -1;
    }
    return 1;
}

bool Context::createDepthImage() {
    VkImageCreateInfo imageInfo{VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO};
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.format = depthFormat;
    imageInfo.extent = {extent.width, extent.height, 1};
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.tiling = VK_IMAGE_TILING_OPTIMAL;
    imageInfo.usage = VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    if (vkCreateImage(device, &imageInfo, nullptr, &depthImage) != VK_SUCCESS) {
        return false;
    }

    VkMemoryRequirements requirements;
    vkGetImageMemoryRequirements(device, depthImage, &requirements);

    // Not lazily allocated: depth must survive between the render passes a capture splits the frame in.
    uint32_t memoryType = findMemoryType(requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

    VkMemoryAllocateInfo memoryInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
    memoryInfo.allocationSize = requirements.size;
    memoryInfo.memoryTypeIndex = memoryType;
    if (memoryType == UINT32_MAX || vkAllocateMemory(device, &memoryInfo, nullptr, &depthMemory) != VK_SUCCESS) {
        return false;
    }
    vkBindImageMemory(device, depthImage, depthMemory, 0);

    VkImageAspectFlags aspect = VK_IMAGE_ASPECT_DEPTH_BIT;
    if (depthFormat == VK_FORMAT_D24_UNORM_S8_UINT || depthFormat == VK_FORMAT_D32_SFLOAT_S8_UINT) {
        aspect |= VK_IMAGE_ASPECT_STENCIL_BIT;
    }

    VkImageViewCreateInfo viewInfo{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO};
    viewInfo.image = depthImage;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = depthFormat;
    viewInfo.subresourceRange = {aspect, 0, 1, 0, 1};
    return vkCreateImageView(device, &viewInfo, nullptr, &depthView) == VK_SUCCESS;
}

void Context::destroyDepthImage() {
    if (depthView != VK_NULL_HANDLE) vkDestroyImageView(device, depthView, nullptr);
    if (depthImage != VK_NULL_HANDLE) vkDestroyImage(device, depthImage, nullptr);
    if (depthMemory != VK_NULL_HANDLE) vkFreeMemory(device, depthMemory, nullptr);
    depthView = VK_NULL_HANDLE;
    depthImage = VK_NULL_HANDLE;
    depthMemory = VK_NULL_HANDLE;
}

bool Context::createSwapchain(uint32_t width, uint32_t height) {
    VkSurfaceCapabilitiesKHR capabilities;
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical, surface, &capabilities);

    VkExtent2D newExtent = capabilities.currentExtent;
    if (newExtent.width == UINT32_MAX) {
        newExtent.width = std::clamp(width, capabilities.minImageExtent.width, capabilities.maxImageExtent.width);
        newExtent.height = std::clamp(height, capabilities.minImageExtent.height, capabilities.maxImageExtent.height);
    }
    if (newExtent.width == 0 || newExtent.height == 0) {
        return false;
    }

    uint32_t imageCount = std::max(capabilities.minImageCount + 1, 2u);
    if (capabilities.maxImageCount > 0) {
        imageCount = std::min(imageCount, capabilities.maxImageCount);
    }

    VkCompositeAlphaFlagBitsKHR compositeAlpha = VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR;
    for (auto candidate : {VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR, VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
                           VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR, VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR}) {
        if (capabilities.supportedCompositeAlpha & candidate) {
            compositeAlpha = candidate;
            break;
        }
    }

    // Without pre-rotation the compositor rotates the image, which costs a bit but is always correct.
    VkSurfaceTransformFlagBitsKHR transform = (capabilities.supportedTransforms & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)
            ? VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR : capabilities.currentTransform;

    VkSwapchainKHR oldSwapchain = swapchain;

    VkSwapchainCreateInfoKHR info{VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR};
    info.surface = surface;
    info.minImageCount = imageCount;
    info.imageFormat = surfaceFormat;
    info.imageColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    info.imageExtent = newExtent;
    info.imageArrayLayers = 1;
    info.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | (canCapture ? VK_IMAGE_USAGE_TRANSFER_SRC_BIT : 0);
    info.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    info.preTransform = transform;
    info.compositeAlpha = compositeAlpha;
    // FIFO is vsync, as OpenGL ES' swap interval of 1, and the only mode every device has.
    info.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    info.clipped = VK_TRUE;
    info.oldSwapchain = oldSwapchain;

    VkSwapchainKHR newSwapchain = VK_NULL_HANDLE;
    VkResult result = vkCreateSwapchainKHR(device, &info, nullptr, &newSwapchain);

    destroySwapchain(true);
    destroyDepthImage();
    if (oldSwapchain != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(device, oldSwapchain, nullptr);
    }
    swapchain = VK_NULL_HANDLE;

    if (result != VK_SUCCESS) {
        LOGE("vkCreateSwapchainKHR failed: %d", result);
        return false;
    }

    swapchain = newSwapchain;
    extent = newExtent;

    if (!createDepthImage()) {
        LOGE("Couldn't create the depth buffer.");
        return false;
    }

    uint32_t count = 0;
    vkGetSwapchainImagesKHR(device, swapchain, &count, nullptr);
    images.resize(count);
    vkGetSwapchainImagesKHR(device, swapchain, &count, images.data());

    imageViews.resize(count);
    framebuffers.resize(count);
    renderFinished.resize(count);

    for (uint32_t i = 0; i < count; i++) {
        VkImageViewCreateInfo viewInfo{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO};
        viewInfo.image = images[i];
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = surfaceFormat;
        viewInfo.subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
        vkCreateImageView(device, &viewInfo, nullptr, &imageViews[i]);

        // Every render pass variant is compatible with this one.
        VkImageView attachments[] = {imageViews[i], depthView};
        VkFramebufferCreateInfo framebufferInfo{VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO};
        framebufferInfo.renderPass = renderPasses[0][0];
        framebufferInfo.attachmentCount = 2;
        framebufferInfo.pAttachments = attachments;
        framebufferInfo.width = extent.width;
        framebufferInfo.height = extent.height;
        framebufferInfo.layers = 1;
        vkCreateFramebuffer(device, &framebufferInfo, nullptr, &framebuffers[i]);

        VkSemaphoreCreateInfo semaphoreInfo{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
        vkCreateSemaphore(device, &semaphoreInfo, nullptr, &renderFinished[i]);
    }

    swapchainOutdated = false;
    LOGI("Swapchain created: %ux%u, %u images.", extent.width, extent.height, count);
    return true;
}

void Context::destroySwapchain(bool keepHandle) {
    for (auto framebuffer : framebuffers) vkDestroyFramebuffer(device, framebuffer, nullptr);
    for (auto view : imageViews) vkDestroyImageView(device, view, nullptr);
    for (auto semaphore : renderFinished) vkDestroySemaphore(device, semaphore, nullptr);
    framebuffers.clear();
    imageViews.clear();
    renderFinished.clear();
    images.clear();

    if (!keepHandle && swapchain != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(device, swapchain, nullptr);
        swapchain = VK_NULL_HANDLE;
    }
}

//endregion


//region Frames

void Context::openSlot() {
    if (slotOpen) {
        return;
    }

    Frame &frame = frames[frameIndex];
    vkWaitForFences(device, 1, &frame.fence, VK_TRUE, UINT64_MAX);

    // The queue executes in order, everything submitted up to this slot's last submission is done.
    completedSubmissions = std::max(completedSubmissions, frame.submission);
    collectGarbage(frame.submission);

    if (frame.timestampsWritten) {
        uint64_t values[2];
        uint32_t base = frameIndex * 2;
        if (vkGetQueryPoolResults(device, timestamps, base, 2, sizeof(values), values, sizeof(uint64_t),
                                  VK_QUERY_RESULT_64_BIT) == VK_SUCCESS && values[1] >= values[0]) {
            lastGpuTime = static_cast<float>(static_cast<double>(values[1] - values[0]) *
                                             physicalProperties.limits.timestampPeriod / 1.0e6);
        }
        frame.timestampsWritten = false;
    }

    // Only the first chunk is kept, larger uploads are rare (e.g. a background).
    while (frame.staging.size() > 1) {
        destroyBuffer(frame.staging.back().buffer);
        frame.staging.pop_back();
    }
    for (auto &chunk : frame.staging) {
        chunk.used = 0;
    }

    slotOpen = true;
}

void Context::deferDestroy(std::function<void()> destroy) {
    // Commands recorded so far, and those of the frame being recorded, may still use the resource.
    garbage.emplace_back(submissions + 1, std::move(destroy));
}

void Context::collectGarbage(uint64_t completedSubmission) {
    while (!garbage.empty() && garbage.front().first <= completedSubmission) {
        garbage.front().second();
        garbage.pop_front();
    }
}

int Context::acquireFrame(uint32_t width, uint32_t height) {
    std::lock_guard lock(mutex);
    imageAcquired = false;

    if (surface == VK_NULL_HANDLE || width == 0 || height == 0) {
        return -1;
    }

    openSlot();
    Frame &frame = frames[frameIndex];

    // A new size (e.g. another resolution scale) doesn't always make the swapchain out of date.
    if (swapchain == VK_NULL_HANDLE || swapchainOutdated || width != requestedWidth || height != requestedHeight) {
        vkDeviceWaitIdle(device);
        requestedWidth = width;
        requestedHeight = height;
        if (!createSwapchain(width, height)) {
            return -1;
        }
    }

    VkResult result = vkAcquireNextImageKHR(device, swapchain, UINT64_MAX, frame.imageAvailable, VK_NULL_HANDLE, &imageIndex);

    if (result == VK_ERROR_OUT_OF_DATE_KHR) {
        swapchainOutdated = true;
        return -1;
    }
    // Android reports VK_SUBOPTIMAL_KHR whenever the pre-transform isn't the display's rotation,
    // which is always the case without pre-rotation (see createSwapchain). The image is still
    // presented correctly, recreating the swapchain wouldn't change it.
    if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
        LOGE("vkAcquireNextImageKHR failed: %d", result);
        return -1;
    }

    imageAcquired = true;
    return static_cast<int>(frameIndex);
}

void Context::recordTextureOperations(VkCommandBuffer cmd) {
    for (auto &operation : textureOperations) {
        Texture *texture = operation.texture;

        if (operation.kind == TextureOperationKind::AcquireExternal) {
            // The producer (e.g. a video decoder) wrote it outside Vulkan, PREINITIALIZED keeps that content.
            VkImageMemoryBarrier barrier{VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};
            barrier.srcAccessMask = 0;
            barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
            barrier.oldLayout = VK_IMAGE_LAYOUT_PREINITIALIZED;
            barrier.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_FOREIGN_EXT;
            barrier.dstQueueFamilyIndex = queueFamily;
            barrier.image = texture->image;
            barrier.subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
            vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                                 0, 0, nullptr, 0, nullptr, 1, &barrier);
            continue;
        }

        if (operation.kind == TextureOperationKind::Clear) {
            imageBarrier(cmd, texture->image, VK_IMAGE_ASPECT_COLOR_BIT,
                         VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                         VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, 0,
                         VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);

            VkClearColorValue transparent{};
            VkImageSubresourceRange range{VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
            vkCmdClearColorImage(cmd, texture->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, &transparent, 1, &range);
        } else {
            imageBarrier(cmd, texture->image, VK_IMAGE_ASPECT_COLOR_BIT,
                         VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                         VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT,
                         VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);

            VkBufferImageCopy region{};
            region.bufferOffset = operation.sourceOffset;
            region.imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
            region.imageOffset = {operation.x, operation.y, 0};
            region.imageExtent = {static_cast<uint32_t>(operation.width), static_cast<uint32_t>(operation.height), 1};
            vkCmdCopyBufferToImage(cmd, operation.source, texture->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
        }

        imageBarrier(cmd, texture->image, VK_IMAGE_ASPECT_COLOR_BIT,
                     VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                     VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT,
                     VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT);
    }
    textureOperations.clear();
}

void Context::beginRenderPass(VkCommandBuffer cmd, bool load, bool storeDepth) {
    VkClearValue clears[2]{};
    clears[0].color = {{0.0f, 0.0f, 0.0f, 1.0f}};
    clears[1].depthStencil = {1.0f, 0};

    VkRenderPassBeginInfo passInfo{VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO};
    passInfo.renderPass = renderPasses[load ? 1 : 0][storeDepth ? 1 : 0];
    passInfo.framebuffer = framebuffers[imageIndex];
    passInfo.renderArea = {{0, 0}, extent};
    passInfo.clearValueCount = 2;
    passInfo.pClearValues = clears;
    vkCmdBeginRenderPass(cmd, &passInfo, VK_SUBPASS_CONTENTS_INLINE);

    // A negative height puts +Y at the top as in OpenGL, so the shaders and scissors stay as they are.
    VkViewport viewport{0.0f, static_cast<float>(extent.height), static_cast<float>(extent.width),
                        -static_cast<float>(extent.height), 0.0f, 1.0f};
    vkCmdSetViewport(cmd, 0, 1, &viewport);
}

void Context::recordCapture(VkCommandBuffer cmd, Texture *texture) {
    VkImage source = images[imageIndex];

    imageBarrier(cmd, source, VK_IMAGE_ASPECT_COLOR_BIT,
                 VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                 VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                 VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_READ_BIT);
    imageBarrier(cmd, texture->image, VK_IMAGE_ASPECT_COLOR_BIT,
                 VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                 VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT,
                 VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT);

    // Top-left origin, as the rest of the engine (unlike OpenGL's framebuffer).
    uint32_t width = std::min(texture->width, extent.width);
    uint32_t height = std::min(texture->height, extent.height);

    if (surfaceFormat == VK_FORMAT_R8G8B8A8_UNORM) {
        VkImageCopy region{};
        region.srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
        region.dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
        region.extent = {width, height, 1};
        vkCmdCopyImage(cmd, source, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                       texture->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
    } else {
        // A copy is bitwise, a BGRA window would swap red and blue. A blit converts.
        VkImageBlit region{};
        region.srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
        region.dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
        region.srcOffsets[1] = {static_cast<int32_t>(width), static_cast<int32_t>(height), 1};
        region.dstOffsets[1] = {static_cast<int32_t>(width), static_cast<int32_t>(height), 1};
        vkCmdBlitImage(cmd, source, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                       texture->image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region, VK_FILTER_NEAREST);
    }

    imageBarrier(cmd, source, VK_IMAGE_ASPECT_COLOR_BIT,
                 VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_READ_BIT,
                 VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
    imageBarrier(cmd, texture->image, VK_IMAGE_ASPECT_COLOR_BIT,
                 VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 VK_PIPELINE_STAGE_TRANSFER_BIT, VK_ACCESS_TRANSFER_WRITE_BIT,
                 VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT);
}

void Context::submit(const int32_t *commands, int32_t count) {
    std::lock_guard lock(mutex);

    if (!imageAcquired) {
        return;
    }
    imageAcquired = false;

    Frame &frame = frames[frameIndex];
    VkCommandBuffer cmd = frame.commandBuffer;

    vkResetCommandPool(device, frame.commandPool, 0);

    VkCommandBufferBeginInfo beginInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    vkBeginCommandBuffer(cmd, &beginInfo);

    if (timestamps != VK_NULL_HANDLE) {
        vkCmdResetQueryPool(cmd, timestamps, frameIndex * 2, 2);
        vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, timestamps, frameIndex * 2);
    }

    // Uploads made while recording happen before the frame draws, as they did on OpenGL ES where a
    // draw uploads its texture's pending sources when it binds it.
    recordTextureOperations(cmd);

    // Whether a capture comes after each position decides if depth must be stored there.
    int32_t lastCapture = -1;
    for (int32_t i = 0; i < count;) {
        int32_t opcode = commands[i];
        if (opcode == OP_CAPTURE) lastCapture = i;
        i += 1 + (opcode == OP_DRAW ? OP_DRAW_INTS : opcode == OP_CAPTURE ? OP_CAPTURE_INTS : 0);
    }

    beginRenderPass(cmd, false, lastCapture >= 0);

    VkPipeline boundPipeline = VK_NULL_HANDLE;
    VkDescriptorSet boundTexture = VK_NULL_HANDLE;
    bool indicesBound = false;
    int32_t i = 0;

    while (i < count) {
        int32_t at = i;
        int32_t opcode = commands[i++];

        switch (opcode) {
            case OP_DRAW: {
                if (i + OP_DRAW_INTS > count) {
                    LOGE("Truncated draw command.");
                    i = count;
                    break;
                }

                const int32_t *c = commands + i;
                i += OP_DRAW_INTS;

                // Non-dispatchable handles are pointers on 64-bit ABIs and uint64_t on 32-bit ones,
                // a C-style cast converts from either.
                auto pipeline = (VkPipeline) (static_cast<uint64_t>(static_cast<uint32_t>(c[0])) |
                                              (static_cast<uint64_t>(static_cast<uint32_t>(c[1])) << 32));
                int32_t x = c[2], y = c[3], w = c[4], h = c[5];
                auto vertexOffset = static_cast<VkDeviceSize>(c[6]);
                auto drawCount = static_cast<uint32_t>(c[7]);
                uint32_t dynamicOffsets[2] = {static_cast<uint32_t>(c[8]), static_cast<uint32_t>(c[9])};
                auto *texture = reinterpret_cast<Texture *>(static_cast<uintptr_t>(
                        static_cast<uint64_t>(static_cast<uint32_t>(c[10])) | (static_cast<uint64_t>(static_cast<uint32_t>(c[11])) << 32)));
                auto instanceOffset = static_cast<VkDeviceSize>(c[12]);
                auto instanceCount = static_cast<uint32_t>(c[13]);
                bool indexed = c[14] != 0;

                if (pipeline != boundPipeline) {
                    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
                    boundPipeline = pipeline;
                }

                // Vulkan rejects scissors outside the framebuffer, they're clamped to it.
                int32_t left = std::clamp(x, 0, static_cast<int32_t>(extent.width));
                int32_t top = std::clamp(y, 0, static_cast<int32_t>(extent.height));
                int32_t right = std::clamp(x + std::max(w, 0), left, static_cast<int32_t>(extent.width));
                int32_t bottom = std::clamp(y + std::max(h, 0), top, static_cast<int32_t>(extent.height));
                VkRect2D scissor{{left, top}, {static_cast<uint32_t>(right - left), static_cast<uint32_t>(bottom - top)}};
                vkCmdSetScissor(cmd, 0, 1, &scissor);

                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, 1,
                                        &frame.uniformSet, 2, dynamicOffsets);

                Texture *sampled = texture != nullptr ? texture : whiteTexture;
                if (sampled->descriptorSet != boundTexture) {
                    // A video frame's set has the layout of its YCbCr sampler, set 0 is the same in both.
                    VkPipelineLayout layout = sampled->ycbcr != nullptr ? sampled->ycbcr->pipelineLayout : pipelineLayout;
                    vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, layout, 1, 1,
                                            &sampled->descriptorSet, 0, nullptr);
                    boundTexture = sampled->descriptorSet;
                }

                vkCmdBindVertexBuffers(cmd, 0, 1, &frame.ring.buffer, &vertexOffset);

                if (indexed) {
                    if (drawCount > quadCapacity) {
                        // Growing replaces the buffer, it must be bound again.
                        if (!ensureQuadIndices(drawCount)) break;
                        indicesBound = false;
                    }
                    if (!indicesBound) {
                        vkCmdBindIndexBuffer(cmd, quadIndices.buffer, 0, VK_INDEX_TYPE_UINT32);
                        indicesBound = true;
                    }
                    vkCmdDrawIndexed(cmd, drawCount * 6, 1, 0, 0, 0);
                } else if (instanceCount > 0) {
                    vkCmdBindVertexBuffers(cmd, 1, 1, &frame.ring.buffer, &instanceOffset);
                    vkCmdDraw(cmd, drawCount, instanceCount, 0, 0);
                } else {
                    vkCmdDraw(cmd, drawCount, 1, 0, 0);
                }
                break;
            }

            case OP_CLEAR_DEPTH: {
                // As glClear, the whole depth buffer whatever the scissor.
                VkClearAttachment attachment{};
                attachment.aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
                attachment.clearValue.depthStencil = {1.0f, 0};
                VkClearRect rect{{{0, 0}, extent}, 0, 1};
                vkCmdClearAttachments(cmd, 1, &attachment, 1, &rect);
                break;
            }

            case OP_CAPTURE: {
                if (i + OP_CAPTURE_INTS > count) {
                    i = count;
                    break;
                }
                auto *texture = reinterpret_cast<Texture *>(static_cast<uintptr_t>(
                        static_cast<uint64_t>(static_cast<uint32_t>(commands[i])) |
                        (static_cast<uint64_t>(static_cast<uint32_t>(commands[i + 1])) << 32)));
                i += OP_CAPTURE_INTS;

                if (!canCapture || texture == nullptr) break;

                // The copy can't happen inside a render pass: the pass ends there and a new one loads
                // what was drawn, depth included.
                vkCmdEndRenderPass(cmd);
                recordCapture(cmd, texture);
                beginRenderPass(cmd, true, at < lastCapture);

                // The descriptor and pipeline bindings survive, render passes don't reset them.
                break;
            }

            default:
                LOGE("Unknown opcode %d, the rest of the frame is dropped.", opcode);
                i = count;
                break;
        }
    }

    vkCmdEndRenderPass(cmd);

    imageBarrier(cmd, images[imageIndex], VK_IMAGE_ASPECT_COLOR_BIT,
                 VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                 VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT,
                 VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0);

    if (timestamps != VK_NULL_HANDLE) {
        vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, timestamps, frameIndex * 2 + 1);
        frame.timestampsWritten = true;
    }

    vkEndCommandBuffer(cmd);

    VkPipelineStageFlags waitStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo submitInfo{VK_STRUCTURE_TYPE_SUBMIT_INFO};
    submitInfo.waitSemaphoreCount = 1;
    submitInfo.pWaitSemaphores = &frame.imageAvailable;
    submitInfo.pWaitDstStageMask = &waitStage;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &cmd;
    submitInfo.signalSemaphoreCount = 1;
    submitInfo.pSignalSemaphores = &renderFinished[imageIndex];

    vkResetFences(device, 1, &frame.fence);
    VkResult result = vkQueueSubmit(queue, 1, &submitInfo, frame.fence);
    if (result != VK_SUCCESS) {
        LOGE("vkQueueSubmit failed: %d", result);
    }
    frame.submission = ++submissions;
    frameSubmitted = true;
}

void Context::present() {
    std::lock_guard lock(mutex);

    if (!frameSubmitted) {
        return;
    }
    frameSubmitted = false;

    VkPresentInfoKHR presentInfo{VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = &renderFinished[imageIndex];
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = &swapchain;
    presentInfo.pImageIndices = &imageIndex;

    VkResult result = vkQueuePresentKHR(queue, &presentInfo);
    if (result == VK_ERROR_OUT_OF_DATE_KHR) {
        swapchainOutdated = true;
    } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
        LOGE("vkQueuePresentKHR failed: %d", result);
    }

    frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;
    slotOpen = false;
}

//endregion


//region Resources

VkShaderModule Context::createShaderModule(const uint32_t *code, size_t size) {
    std::lock_guard lock(mutex);

    VkShaderModuleCreateInfo info{VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO};
    info.codeSize = size;
    info.pCode = code;

    VkShaderModule module = VK_NULL_HANDLE;
    if (vkCreateShaderModule(device, &info, nullptr, &module) != VK_SUCCESS) {
        LOGE("vkCreateShaderModule failed.");
        return VK_NULL_HANDLE;
    }
    return module;
}

void Context::destroyShaderModule(VkShaderModule module) {
    std::lock_guard lock(mutex);
    // Pipelines keep what they need, a module can go as soon as they're created.
    vkDestroyShaderModule(device, module, nullptr);
}

VkPipeline Context::createPipeline(VkShaderModule vertex, VkShaderModule fragment,
                                   const int32_t *vertexInput, int32_t vertexInputCount,
                                   const int32_t *blend, const int32_t *depth, YcbcrSampler *ycbcr) {
    std::lock_guard lock(mutex);

    if (renderPasses[0][0] == VK_NULL_HANDLE) {
        LOGE("No render pass to create a pipeline for, there is no surface yet.");
        return VK_NULL_HANDLE;
    }

    std::vector<VkVertexInputBindingDescription> bindings;
    std::vector<VkVertexInputAttributeDescription> attributes;

    int32_t i = 0;
    int32_t bindingCount = vertexInput[i++];
    for (int32_t b = 0; b < bindingCount; b++) {
        VkVertexInputBindingDescription binding{};
        binding.binding = static_cast<uint32_t>(b);
        binding.stride = static_cast<uint32_t>(vertexInput[i++]);
        binding.inputRate = vertexInput[i++] != 0 ? VK_VERTEX_INPUT_RATE_INSTANCE : VK_VERTEX_INPUT_RATE_VERTEX;
        bindings.push_back(binding);
    }
    int32_t attributeCount = vertexInput[i++];
    for (int32_t a = 0; a < attributeCount && i + 4 <= vertexInputCount; a++) {
        VkVertexInputAttributeDescription attribute{};
        attribute.location = static_cast<uint32_t>(vertexInput[i++]);
        attribute.binding = static_cast<uint32_t>(vertexInput[i++]);
        attribute.format = static_cast<VkFormat>(vertexInput[i++]);
        attribute.offset = static_cast<uint32_t>(vertexInput[i++]);
        attributes.push_back(attribute);
    }

    VkPipelineShaderStageCreateInfo stages[2]{};
    stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    stages[0].module = vertex;
    stages[0].pName = "main";
    stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    stages[1].module = fragment;
    stages[1].pName = "main";

    VkPipelineVertexInputStateCreateInfo vertexInputInfo{VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO};
    vertexInputInfo.vertexBindingDescriptionCount = static_cast<uint32_t>(bindings.size());
    vertexInputInfo.pVertexBindingDescriptions = bindings.data();
    vertexInputInfo.vertexAttributeDescriptionCount = static_cast<uint32_t>(attributes.size());
    vertexInputInfo.pVertexAttributeDescriptions = attributes.data();

    VkPipelineInputAssemblyStateCreateInfo inputAssembly{VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO};
    inputAssembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;

    VkPipelineViewportStateCreateInfo viewportState{VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO};
    viewportState.viewportCount = 1;
    viewportState.scissorCount = 1;

    VkPipelineRasterizationStateCreateInfo rasterization{VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO};
    rasterization.polygonMode = VK_POLYGON_MODE_FILL;
    rasterization.cullMode = VK_CULL_MODE_NONE;
    rasterization.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    rasterization.lineWidth = 1.0f;

    VkPipelineMultisampleStateCreateInfo multisample{VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO};
    multisample.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineDepthStencilStateCreateInfo depthStencil{VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO};
    depthStencil.depthTestEnable = depth[0] != 0 ? VK_TRUE : VK_FALSE;
    depthStencil.depthWriteEnable = depth[0] != 0 && depth[1] != 0 ? VK_TRUE : VK_FALSE;
    depthStencil.depthCompareOp = static_cast<VkCompareOp>(depth[2]);

    // Blending is always on, as in the OpenGL ES backends.
    VkPipelineColorBlendAttachmentState blendAttachment{};
    blendAttachment.blendEnable = VK_TRUE;
    blendAttachment.srcColorBlendFactor = static_cast<VkBlendFactor>(blend[0]);
    blendAttachment.dstColorBlendFactor = static_cast<VkBlendFactor>(blend[1]);
    blendAttachment.colorBlendOp = VK_BLEND_OP_ADD;
    blendAttachment.srcAlphaBlendFactor = static_cast<VkBlendFactor>(blend[2]);
    blendAttachment.dstAlphaBlendFactor = static_cast<VkBlendFactor>(blend[3]);
    blendAttachment.alphaBlendOp = VK_BLEND_OP_ADD;
    blendAttachment.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                                     VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;

    VkPipelineColorBlendStateCreateInfo blendState{VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO};
    blendState.attachmentCount = 1;
    blendState.pAttachments = &blendAttachment;

    VkDynamicState dynamicStates[] = {VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR};
    VkPipelineDynamicStateCreateInfo dynamicState{VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO};
    dynamicState.dynamicStateCount = 2;
    dynamicState.pDynamicStates = dynamicStates;

    VkGraphicsPipelineCreateInfo info{VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO};
    info.stageCount = 2;
    info.pStages = stages;
    info.pVertexInputState = &vertexInputInfo;
    info.pInputAssemblyState = &inputAssembly;
    info.pViewportState = &viewportState;
    info.pRasterizationState = &rasterization;
    info.pMultisampleState = &multisample;
    info.pDepthStencilState = &depthStencil;
    info.pColorBlendState = &blendState;
    info.pDynamicState = &dynamicState;
    info.layout = ycbcr != nullptr ? ycbcr->pipelineLayout : pipelineLayout;
    // Compatible with every variant.
    info.renderPass = renderPasses[0][0];
    info.subpass = 0;

    VkPipeline pipeline = VK_NULL_HANDLE;
    VkResult result = vkCreateGraphicsPipelines(device, pipelineCache, 1, &info, nullptr, &pipeline);
    if (result != VK_SUCCESS) {
        LOGE("vkCreateGraphicsPipelines failed: %d", result);
        return VK_NULL_HANDLE;
    }
    return pipeline;
}

void Context::destroyPipeline(VkPipeline pipeline) {
    std::lock_guard lock(mutex);
    deferDestroy([this, pipeline]() { vkDestroyPipeline(device, pipeline, nullptr); });
}

VkDescriptorSet Context::allocateTextureSet(VkDescriptorPool &pool) {
    VkDescriptorSetAllocateInfo setInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
    setInfo.descriptorSetCount = 1;
    setInfo.pSetLayouts = &samplerSetLayout;

    VkDescriptorSet set = VK_NULL_HANDLE;
    for (auto candidate : texturePools) {
        setInfo.descriptorPool = candidate;
        if (vkAllocateDescriptorSets(device, &setInfo, &set) == VK_SUCCESS) {
            pool = candidate;
            return set;
        }
    }

    // Every pool is full, another one is made.
    VkDescriptorPoolSize poolSize{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, TEXTURE_SETS_PER_POOL};
    VkDescriptorPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};
    poolInfo.flags = VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT;
    poolInfo.maxSets = TEXTURE_SETS_PER_POOL;
    poolInfo.poolSizeCount = 1;
    poolInfo.pPoolSizes = &poolSize;

    VkDescriptorPool newPool = VK_NULL_HANDLE;
    if (vkCreateDescriptorPool(device, &poolInfo, nullptr, &newPool) != VK_SUCCESS) {
        return VK_NULL_HANDLE;
    }
    texturePools.push_back(newPool);

    setInfo.descriptorPool = newPool;
    if (vkAllocateDescriptorSets(device, &setInfo, &set) != VK_SUCCESS) {
        return VK_NULL_HANDLE;
    }
    pool = newPool;
    return set;
}

Texture *Context::createTexture(uint32_t width, uint32_t height) {
    std::lock_guard lock(mutex);

    auto *texture = new Texture();
    texture->width = width;
    texture->height = height;

    VkImageCreateInfo imageInfo{VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO};
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.format = VK_FORMAT_R8G8B8A8_UNORM;
    imageInfo.extent = {width, height, 1};
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.tiling = VK_IMAGE_TILING_OPTIMAL;
    imageInfo.usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;

    if (vkCreateImage(device, &imageInfo, nullptr, &texture->image) != VK_SUCCESS) {
        LOGE("vkCreateImage failed for a %ux%u texture.", width, height);
        delete texture;
        return nullptr;
    }

    VkMemoryRequirements requirements;
    vkGetImageMemoryRequirements(device, texture->image, &requirements);

    VkMemoryAllocateInfo memoryInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
    memoryInfo.allocationSize = requirements.size;
    memoryInfo.memoryTypeIndex = findMemoryType(requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

    if (memoryInfo.memoryTypeIndex == UINT32_MAX || vkAllocateMemory(device, &memoryInfo, nullptr, &texture->memory) != VK_SUCCESS) {
        LOGE("Couldn't allocate a %ux%u texture.", width, height);
        vkDestroyImage(device, texture->image, nullptr);
        delete texture;
        return nullptr;
    }
    vkBindImageMemory(device, texture->image, texture->memory, 0);

    VkImageViewCreateInfo viewInfo{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO};
    viewInfo.image = texture->image;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = VK_FORMAT_R8G8B8A8_UNORM;
    viewInfo.subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
    vkCreateImageView(device, &viewInfo, nullptr, &texture->view);

    texture->descriptorSet = allocateTextureSet(texture->descriptorPool);

    VkDescriptorImageInfo imageDescriptor{sampler, texture->view, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};
    VkWriteDescriptorSet write{VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};
    write.dstSet = texture->descriptorSet;
    write.dstBinding = 0;
    write.descriptorCount = 1;
    write.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    write.pImageInfo = &imageDescriptor;
    vkUpdateDescriptorSets(device, 1, &write, 0, nullptr);

    textureOperations.push_back({texture, TextureOperationKind::Clear, VK_NULL_HANDLE, 0, 0, 0, 0, 0});
    return texture;
}

void Context::uploadTexture(Texture *texture, int32_t x, int32_t y, int32_t width, int32_t height,
                            const void *pixels, size_t size) {
    std::lock_guard lock(mutex);

    if (texture == nullptr || width <= 0 || height <= 0) {
        return;
    }

    // The staging belongs to the frame being recorded, which may not be acquired yet.
    openSlot();
    Frame &frame = frames[frameIndex];

    // Copies need offsets that are a multiple of the texel size.
    StagingChunk *chunk = nullptr;
    VkDeviceSize offset = 0;
    for (auto &candidate : frame.staging) {
        VkDeviceSize aligned = (candidate.used + 15) & ~static_cast<VkDeviceSize>(15);
        if (aligned + size <= candidate.buffer.size) {
            chunk = &candidate;
            offset = aligned;
            break;
        }
    }

    if (chunk == nullptr) {
        StagingChunk created;
        if (!createBuffer(std::max(STAGING_CHUNK_SIZE, static_cast<VkDeviceSize>(size)), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, created.buffer)) {
            LOGE("Couldn't allocate %zu bytes of staging for an upload.", size);
            return;
        }
        frame.staging.push_back(created);
        chunk = &frame.staging.back();
        offset = 0;
    }

    memcpy(static_cast<uint8_t *>(chunk->buffer.mapped) + offset, pixels, size);
    chunk->used = offset + size;

    textureOperations.push_back({texture, TextureOperationKind::Upload, chunk->buffer.buffer, offset, x, y, width, height});
}

void Context::destroyTexture(Texture *texture) {
    std::lock_guard lock(mutex);

    if (texture == nullptr) {
        return;
    }

    deferDestroy([this, texture]() {
        vkFreeDescriptorSets(device, texture->descriptorPool, 1, &texture->descriptorSet);
        vkDestroyImageView(device, texture->view, nullptr);
        vkDestroyImage(device, texture->image, nullptr);
        vkFreeMemory(device, texture->memory, nullptr);
        delete texture;
    });
}

uint64_t Context::submissionCount() {
    std::lock_guard lock(mutex);
    return submissions;
}

uint64_t Context::completedSubmissionCount() {
    std::lock_guard lock(mutex);
    return completedSubmissions;
}

YcbcrSampler *Context::ycbcrSamplerFor(const VkAndroidHardwareBufferFormatPropertiesANDROID &format) {
    // The suggested conversion depends only on the format, frames of a video share one.
    uint64_t externalFormat = format.format == VK_FORMAT_UNDEFINED ? format.externalFormat : 0;
    for (auto *candidate : ycbcrSamplers) {
        if (candidate->externalFormat == externalFormat && candidate->format == format.format &&
            candidate->model == format.suggestedYcbcrModel && candidate->range == format.suggestedYcbcrRange &&
            memcmp(&candidate->components, &format.samplerYcbcrConversionComponents, sizeof(VkComponentMapping)) == 0 &&
            candidate->xChromaOffset == format.suggestedXChromaOffset && candidate->yChromaOffset == format.suggestedYChromaOffset) {
            return candidate;
        }
    }

    auto *ycbcr = new YcbcrSampler();
    ycbcr->externalFormat = externalFormat;
    ycbcr->format = format.format;
    ycbcr->model = format.suggestedYcbcrModel;
    ycbcr->range = format.suggestedYcbcrRange;
    ycbcr->components = format.samplerYcbcrConversionComponents;
    ycbcr->xChromaOffset = format.suggestedXChromaOffset;
    ycbcr->yChromaOffset = format.suggestedYChromaOffset;

    VkFilter filter = (format.formatFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_YCBCR_CONVERSION_LINEAR_FILTER_BIT)
            ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;

    VkExternalFormatANDROID external{VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID};
    external.externalFormat = externalFormat;

    VkSamplerYcbcrConversionCreateInfo conversionInfo{VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO};
    conversionInfo.pNext = &external;
    conversionInfo.format = format.format;
    conversionInfo.ycbcrModel = format.suggestedYcbcrModel;
    conversionInfo.ycbcrRange = format.suggestedYcbcrRange;
    conversionInfo.components = format.samplerYcbcrConversionComponents;
    conversionInfo.xChromaOffset = format.suggestedXChromaOffset;
    conversionInfo.yChromaOffset = format.suggestedYChromaOffset;
    conversionInfo.chromaFilter = filter;
    conversionInfo.forceExplicitReconstruction = VK_FALSE;

    if (createSamplerYcbcrConversion(device, &conversionInfo, nullptr, &ycbcr->conversion) != VK_SUCCESS) {
        LOGE("Couldn't create a YCbCr conversion for the video format.");
        delete ycbcr;
        return nullptr;
    }

    VkSamplerYcbcrConversionInfo conversion{VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO};
    conversion.conversion = ycbcr->conversion;

    // The filters must be the chroma filter unless the format allows a separate one.
    VkSamplerCreateInfo samplerInfo{VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO};
    samplerInfo.pNext = &conversion;
    samplerInfo.magFilter = filter;
    samplerInfo.minFilter = filter;
    samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST;
    samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    vkCreateSampler(device, &samplerInfo, nullptr, &ycbcr->sampler);

    VkDescriptorSetLayoutBinding binding{};
    binding.binding = 0;
    binding.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    binding.descriptorCount = 1;
    binding.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
    binding.pImmutableSamplers = &ycbcr->sampler;

    VkDescriptorSetLayoutCreateInfo setLayoutInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
    setLayoutInfo.bindingCount = 1;
    setLayoutInfo.pBindings = &binding;
    vkCreateDescriptorSetLayout(device, &setLayoutInfo, nullptr, &ycbcr->setLayout);

    VkDescriptorSetLayout setLayouts[] = {uniformSetLayout, ycbcr->setLayout};
    VkPipelineLayoutCreateInfo layoutInfo{VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO};
    layoutInfo.setLayoutCount = 2;
    layoutInfo.pSetLayouts = setLayouts;
    vkCreatePipelineLayout(device, &layoutInfo, nullptr, &ycbcr->pipelineLayout);

    // Multi-planar formats may take up to three descriptors per set.
    VkDescriptorPoolSize poolSize{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 3 * TEXTURE_SETS_PER_POOL};
    VkDescriptorPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};
    poolInfo.flags = VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT;
    poolInfo.maxSets = TEXTURE_SETS_PER_POOL;
    poolInfo.poolSizeCount = 1;
    poolInfo.pPoolSizes = &poolSize;
    vkCreateDescriptorPool(device, &poolInfo, nullptr, &ycbcr->pool);

    LOGI("Created a YCbCr sampler for a video format (external format %llu, VkFormat %d).",
         static_cast<unsigned long long>(externalFormat), format.format);

    ycbcrSamplers.push_back(ycbcr);
    return ycbcr;
}

Texture *Context::importHardwareBuffer(AHardwareBuffer *buffer) {
    std::lock_guard lock(mutex);

    if (!externalSupported || buffer == nullptr) {
        return nullptr;
    }

    VkAndroidHardwareBufferFormatPropertiesANDROID format{VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID};
    VkAndroidHardwareBufferPropertiesANDROID properties{VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID};
    properties.pNext = &format;

    if (getHardwareBufferProperties(device, buffer, &properties) != VK_SUCCESS) {
        LOGE("Couldn't read the properties of a video frame.");
        return nullptr;
    }

    // Decoders output YCbCr, through an external format or a multi-planar one. An RGB frame is
    // sampled as any texture.
    bool converted = format.format == VK_FORMAT_UNDEFINED || isYcbcrFormat(format.format);
    YcbcrSampler *ycbcr = converted ? ycbcrSamplerFor(format) : nullptr;
    if (converted && ycbcr == nullptr) {
        return nullptr;
    }

    AHardwareBuffer_Desc description{};
    describeHardwareBuffer()(buffer, &description);

    auto *texture = new Texture();
    texture->width = description.width;
    texture->height = description.height;
    texture->ycbcr = ycbcr;

    VkExternalFormatANDROID external{VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID};
    external.externalFormat = format.format == VK_FORMAT_UNDEFINED ? format.externalFormat : 0;

    VkExternalMemoryImageCreateInfo externalMemory{VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO};
    externalMemory.pNext = &external;
    externalMemory.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID;

    // PREINITIALIZED, the content written by the producer is kept when it's acquired.
    VkImageCreateInfo imageInfo{VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO};
    imageInfo.pNext = &externalMemory;
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.format = format.format;
    imageInfo.extent = {description.width, description.height, 1};
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
    imageInfo.tiling = VK_IMAGE_TILING_OPTIMAL;
    imageInfo.usage = VK_IMAGE_USAGE_SAMPLED_BIT;
    imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_PREINITIALIZED;

    if (vkCreateImage(device, &imageInfo, nullptr, &texture->image) != VK_SUCCESS) {
        LOGE("Couldn't create an image for a video frame.");
        delete texture;
        return nullptr;
    }

    // The memory is the hardware buffer's, imported rather than allocated.
    VkImportAndroidHardwareBufferInfoANDROID import{VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID};
    import.buffer = buffer;

    VkMemoryDedicatedAllocateInfo dedicated{VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO};
    dedicated.pNext = &import;
    dedicated.image = texture->image;

    VkMemoryAllocateInfo memoryInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
    memoryInfo.pNext = &dedicated;
    memoryInfo.allocationSize = properties.allocationSize;
    memoryInfo.memoryTypeIndex = properties.memoryTypeBits != 0 ? static_cast<uint32_t>(__builtin_ctz(properties.memoryTypeBits)) : 0;

    if (properties.memoryTypeBits == 0 || vkAllocateMemory(device, &memoryInfo, nullptr, &texture->memory) != VK_SUCCESS) {
        LOGE("Couldn't import the memory of a video frame.");
        vkDestroyImage(device, texture->image, nullptr);
        delete texture;
        return nullptr;
    }
    vkBindImageMemory(device, texture->image, texture->memory, 0);

    VkSamplerYcbcrConversionInfo conversion{VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO};
    conversion.conversion = ycbcr != nullptr ? ycbcr->conversion : VK_NULL_HANDLE;

    VkImageViewCreateInfo viewInfo{VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO};
    viewInfo.pNext = ycbcr != nullptr ? &conversion : nullptr;
    viewInfo.image = texture->image;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = format.format;
    viewInfo.subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
    vkCreateImageView(device, &viewInfo, nullptr, &texture->view);

    VkDescriptorSetAllocateInfo setInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
    setInfo.descriptorSetCount = 1;

    if (ycbcr != nullptr) {
        setInfo.descriptorPool = ycbcr->pool;
        setInfo.pSetLayouts = &ycbcr->setLayout;
        texture->descriptorPool = ycbcr->pool;
        if (vkAllocateDescriptorSets(device, &setInfo, &texture->descriptorSet) != VK_SUCCESS) {
            texture->descriptorSet = VK_NULL_HANDLE;
        }
    } else {
        texture->descriptorSet = allocateTextureSet(texture->descriptorPool);
    }

    if (texture->descriptorSet == VK_NULL_HANDLE) {
        LOGE("Couldn't allocate a descriptor set for a video frame.");
        vkDestroyImageView(device, texture->view, nullptr);
        vkDestroyImage(device, texture->image, nullptr);
        vkFreeMemory(device, texture->memory, nullptr);
        delete texture;
        return nullptr;
    }

    // The sampler of a YCbCr set is immutable, the one given here is ignored.
    VkDescriptorImageInfo imageDescriptor{ycbcr != nullptr ? ycbcr->sampler : sampler, texture->view,
                                          VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};
    VkWriteDescriptorSet write{VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};
    write.dstSet = texture->descriptorSet;
    write.dstBinding = 0;
    write.descriptorCount = 1;
    write.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    write.pImageInfo = &imageDescriptor;
    vkUpdateDescriptorSets(device, 1, &write, 0, nullptr);

    textureOperations.push_back({texture, TextureOperationKind::AcquireExternal, VK_NULL_HANDLE, 0, 0, 0, 0, 0});
    return texture;
}

//endregion


void Context::destroy() {
    std::lock_guard lock(mutex);

    if (device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device);
        destroySwapchain(false);
        destroyDepthImage();

        if (whiteTexture != nullptr) {
            destroyTexture(whiteTexture);
            whiteTexture = nullptr;
        }
        collectGarbage(UINT64_MAX);
        textureOperations.clear();

        destroyBuffer(quadIndices);

        for (auto *ycbcr : ycbcrSamplers) {
            vkDestroyDescriptorPool(device, ycbcr->pool, nullptr);
            vkDestroyPipelineLayout(device, ycbcr->pipelineLayout, nullptr);
            vkDestroyDescriptorSetLayout(device, ycbcr->setLayout, nullptr);
            vkDestroySampler(device, ycbcr->sampler, nullptr);
            destroySamplerYcbcrConversion(device, ycbcr->conversion, nullptr);
            delete ycbcr;
        }
        ycbcrSamplers.clear();

        for (auto &frame : frames) {
            destroyBuffer(frame.ring);
            for (auto &chunk : frame.staging) destroyBuffer(chunk.buffer);
            frame.staging.clear();
            if (frame.fence != VK_NULL_HANDLE) vkDestroyFence(device, frame.fence, nullptr);
            if (frame.imageAvailable != VK_NULL_HANDLE) vkDestroySemaphore(device, frame.imageAvailable, nullptr);
            if (frame.commandPool != VK_NULL_HANDLE) vkDestroyCommandPool(device, frame.commandPool, nullptr);
            frame = Frame{};
        }

        destroyRenderPasses();
        if (timestamps != VK_NULL_HANDLE) vkDestroyQueryPool(device, timestamps, nullptr);
        if (sampler != VK_NULL_HANDLE) vkDestroySampler(device, sampler, nullptr);
        if (pipelineCache != VK_NULL_HANDLE) vkDestroyPipelineCache(device, pipelineCache, nullptr);
        for (auto pool : texturePools) vkDestroyDescriptorPool(device, pool, nullptr);
        texturePools.clear();
        if (uniformPool != VK_NULL_HANDLE) vkDestroyDescriptorPool(device, uniformPool, nullptr);
        if (pipelineLayout != VK_NULL_HANDLE) vkDestroyPipelineLayout(device, pipelineLayout, nullptr);
        if (uniformSetLayout != VK_NULL_HANDLE) vkDestroyDescriptorSetLayout(device, uniformSetLayout, nullptr);
        if (samplerSetLayout != VK_NULL_HANDLE) vkDestroyDescriptorSetLayout(device, samplerSetLayout, nullptr);

        vkDestroyDevice(device, nullptr);
        device = VK_NULL_HANDLE;
    }

    if (surface != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance, surface, nullptr);
        surface = VK_NULL_HANDLE;
    }
    if (window != nullptr) {
        ANativeWindow_release(window);
        window = nullptr;
    }

    if (instance != VK_NULL_HANDLE) {
        if (messenger != VK_NULL_HANDLE) {
            auto destroyMessenger = reinterpret_cast<PFN_vkDestroyDebugUtilsMessengerEXT>(
                    vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT"));
            if (destroyMessenger != nullptr) destroyMessenger(instance, messenger, nullptr);
            messenger = VK_NULL_HANDLE;
        }
        vkDestroyInstance(instance, nullptr);
        instance = VK_NULL_HANDLE;
    }
}

} // namespace verktex
