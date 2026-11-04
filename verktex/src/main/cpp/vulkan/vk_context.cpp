#include "vk_context.h"

#include <android/log.h>
#include <vulkan/vulkan_android.h>

#include <algorithm>
#include <cstring>

#define LOG_TAG "VerktexVulkan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace verktex {

namespace {

const char *VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation";

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

std::string resultName(VkResult result) {
    return "VkResult " + std::to_string(static_cast<int>(result));
}

} // namespace


bool Context::initialize(bool enableValidation, std::string &error) {
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
    if (validation) {
        layers.push_back(VALIDATION_LAYER);
        if (hasInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME, VALIDATION_LAYER) ||
            hasInstanceExtension(VK_EXT_DEBUG_UTILS_EXTENSION_NAME, nullptr)) {
            extensions.push_back(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        }
    }
    bool debugUtils = std::find_if(extensions.begin(), extensions.end(),
            [](auto name) { return strcmp(name, VK_EXT_DEBUG_UTILS_EXTENSION_NAME) == 0; }) != extensions.end();

    VkApplicationInfo application{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    application.pApplicationName = "osu!droid";
    application.pEngineName = "Verktex";
    application.apiVersion = instanceVersion >= VK_API_VERSION_1_1 ? VK_API_VERSION_1_1 : VK_API_VERSION_1_0;

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

    return createDeviceObjects(error) && createFrames(error);
}

bool Context::createDeviceObjects(std::string &error) {
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

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

    float priority = 1.0f;
    VkDeviceQueueCreateInfo queueInfo{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
    queueInfo.queueFamilyIndex = queueFamily;
    queueInfo.queueCount = 1;
    queueInfo.pQueuePriorities = &priority;

    VkDeviceCreateInfo deviceInfo{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
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

    LOGI("Using %s, Vulkan %u.%u.%u.", physicalProperties.deviceName,
         VK_VERSION_MAJOR(physicalProperties.apiVersion), VK_VERSION_MINOR(physicalProperties.apiVersion),
         VK_VERSION_PATCH(physicalProperties.apiVersion));

    // Set 0: the vertex and fragment uniform blocks, slices of the frame's ring. Set 1: the
    // samplers, see GLSL450VulkanTranslator.
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
    vkCreateDescriptorPool(device, &poolInfo, nullptr, &descriptorPool);

    VkPipelineCacheCreateInfo cacheInfo{VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO};
    vkCreatePipelineCache(device, &cacheInfo, nullptr, &pipelineCache);

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

        // Host visible and coherent, written directly by Kotlin and read by the GPU as vertex and
        // uniform data. Mobile GPUs share the memory, so there is no staging.
        VkBufferCreateInfo bufferInfo{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO};
        bufferInfo.size = RING_SIZE;
        bufferInfo.usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
        bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        vkCreateBuffer(device, &bufferInfo, nullptr, &frame.ring);

        VkMemoryRequirements requirements;
        vkGetBufferMemoryRequirements(device, frame.ring, &requirements);

        uint32_t memoryType = findMemoryType(requirements.memoryTypeBits,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        if (memoryType == UINT32_MAX) {
            error = "No host visible and coherent memory for the frame ring.";
            return false;
        }

        VkMemoryAllocateInfo memoryInfo{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
        memoryInfo.allocationSize = requirements.size;
        memoryInfo.memoryTypeIndex = memoryType;
        VkResult result = vkAllocateMemory(device, &memoryInfo, nullptr, &frame.ringMemory);
        if (result != VK_SUCCESS) {
            error = "Couldn't allocate the frame ring: " + resultName(result);
            return false;
        }
        vkBindBufferMemory(device, frame.ring, frame.ringMemory, 0);
        vkMapMemory(device, frame.ringMemory, 0, RING_SIZE, 0, &frame.ringMapped);

        VkDescriptorSetAllocateInfo setInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
        setInfo.descriptorPool = descriptorPool;
        setInfo.descriptorSetCount = 1;
        setInfo.pSetLayouts = &uniformSetLayout;
        vkAllocateDescriptorSets(device, &setInfo, &frame.uniformSet);

        VkDescriptorBufferInfo ringInfo{frame.ring, 0, UNIFORM_RANGE};
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

bool Context::createRenderPass(VkFormat format) {
    if (renderPass != VK_NULL_HANDLE) {
        vkDestroyRenderPass(device, renderPass, nullptr);
        renderPass = VK_NULL_HANDLE;
    }

    VkAttachmentDescription color{};
    color.format = format;
    color.samples = VK_SAMPLE_COUNT_1_BIT;
    color.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    color.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    color.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    color.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    color.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    color.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    VkAttachmentReference colorReference{0, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL};

    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &colorReference;

    // The image is acquired with a semaphore waited at the color output stage, the layout
    // transition must wait for it as well.
    VkSubpassDependency dependency{};
    dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
    dependency.dstSubpass = 0;
    dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dependency.srcAccessMask = 0;
    dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;

    VkRenderPassCreateInfo info{VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO};
    info.attachmentCount = 1;
    info.pAttachments = &color;
    info.subpassCount = 1;
    info.pSubpasses = &subpass;
    info.dependencyCount = 1;
    info.pDependencies = &dependency;

    return vkCreateRenderPass(device, &info, nullptr, &renderPass) == VK_SUCCESS;
}

int Context::setSurface(ANativeWindow *newWindow, std::string &error) {
    vkDeviceWaitIdle(device);
    destroySwapchain(false);

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

    if (format == surfaceFormat && renderPass != VK_NULL_HANDLE) {
        return 0;
    }

    surfaceFormat = format;
    if (!createRenderPass(format)) {
        error = "vkCreateRenderPass failed.";
        return -1;
    }
    return 1;
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
    info.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
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

        VkFramebufferCreateInfo framebufferInfo{VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO};
        framebufferInfo.renderPass = renderPass;
        framebufferInfo.attachmentCount = 1;
        framebufferInfo.pAttachments = &imageViews[i];
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

int Context::acquireFrame(uint32_t width, uint32_t height) {
    imageAcquired = false;

    if (surface == VK_NULL_HANDLE || width == 0 || height == 0) {
        return -1;
    }

    Frame &frame = frames[frameIndex];
    vkWaitForFences(device, 1, &frame.fence, VK_TRUE, UINT64_MAX);

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

    // Only reset once an image is acquired, an early return must leave the fence signaled.
    vkResetFences(device, 1, &frame.fence);
    imageAcquired = true;
    return static_cast<int>(frameIndex);
}

void Context::submit(const int32_t *commands, int32_t count) {
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

    VkClearValue clear{};
    clear.color = {{0.0f, 0.0f, 0.0f, 1.0f}};

    VkRenderPassBeginInfo passInfo{VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO};
    passInfo.renderPass = renderPass;
    passInfo.framebuffer = framebuffers[imageIndex];
    passInfo.renderArea = {{0, 0}, extent};
    passInfo.clearValueCount = 1;
    passInfo.pClearValues = &clear;
    vkCmdBeginRenderPass(cmd, &passInfo, VK_SUBPASS_CONTENTS_INLINE);

    // A negative height puts +Y at the top as in OpenGL, so the shaders and scissors stay as they are.
    VkViewport viewport{0.0f, static_cast<float>(extent.height), static_cast<float>(extent.width),
                        -static_cast<float>(extent.height), 0.0f, 1.0f};
    vkCmdSetViewport(cmd, 0, 1, &viewport);

    VkPipeline boundPipeline = VK_NULL_HANDLE;
    int32_t i = 0;

    while (i < count) {
        int32_t opcode = commands[i++];

        switch (opcode) {
            case OP_DRAW: {
                if (i + 10 > count) {
                    LOGE("Truncated draw command.");
                    i = count;
                    break;
                }

                // Non-dispatchable handles are pointers on 64-bit ABIs and uint64_t on 32-bit ones,
                // a C-style cast converts from either.
                auto pipeline = (VkPipeline) (
                        static_cast<uint64_t>(static_cast<uint32_t>(commands[i])) |
                        (static_cast<uint64_t>(static_cast<uint32_t>(commands[i + 1])) << 32));
                int32_t x = commands[i + 2];
                int32_t y = commands[i + 3];
                int32_t w = commands[i + 4];
                int32_t h = commands[i + 5];
                auto vertexOffset = static_cast<VkDeviceSize>(commands[i + 6]);
                auto vertexCount = static_cast<uint32_t>(commands[i + 7]);
                uint32_t dynamicOffsets[2] = {static_cast<uint32_t>(commands[i + 8]), static_cast<uint32_t>(commands[i + 9])};
                i += 10;

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

                vkCmdBindVertexBuffers(cmd, 0, 1, &frame.ring, &vertexOffset);
                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, 1,
                                        &frame.uniformSet, 2, dynamicOffsets);
                vkCmdDraw(cmd, vertexCount, 1, 0, 0);
                break;
            }

            default:
                LOGE("Unknown opcode %d, the rest of the frame is dropped.", opcode);
                i = count;
                break;
        }
    }

    vkCmdEndRenderPass(cmd);
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

    VkResult result = vkQueueSubmit(queue, 1, &submitInfo, frame.fence);
    if (result != VK_SUCCESS) {
        LOGE("vkQueueSubmit failed: %d", result);
    }

    VkPresentInfoKHR presentInfo{VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = &renderFinished[imageIndex];
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = &swapchain;
    presentInfo.pImageIndices = &imageIndex;

    result = vkQueuePresentKHR(queue, &presentInfo);
    if (result == VK_ERROR_OUT_OF_DATE_KHR) {
        swapchainOutdated = true;
    } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
        LOGE("vkQueuePresentKHR failed: %d", result);
    }

    frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;
}

VkShaderModule Context::createShaderModule(const uint32_t *code, size_t size) {
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
    vkDestroyShaderModule(device, module, nullptr);
}

VkPipeline Context::createPipeline(VkShaderModule vertex, VkShaderModule fragment,
                                   const int32_t *vertexInput, int32_t vertexInputCount,
                                   const int32_t *blend) {
    if (renderPass == VK_NULL_HANDLE) {
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
    info.pColorBlendState = &blendState;
    info.pDynamicState = &dynamicState;
    info.layout = pipelineLayout;
    info.renderPass = renderPass;
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
    vkDestroyPipeline(device, pipeline, nullptr);
}

void Context::destroy() {
    if (device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device);
        destroySwapchain(false);

        for (auto &frame : frames) {
            if (frame.ringMapped != nullptr) vkUnmapMemory(device, frame.ringMemory);
            if (frame.ring != VK_NULL_HANDLE) vkDestroyBuffer(device, frame.ring, nullptr);
            if (frame.ringMemory != VK_NULL_HANDLE) vkFreeMemory(device, frame.ringMemory, nullptr);
            if (frame.fence != VK_NULL_HANDLE) vkDestroyFence(device, frame.fence, nullptr);
            if (frame.imageAvailable != VK_NULL_HANDLE) vkDestroySemaphore(device, frame.imageAvailable, nullptr);
            if (frame.commandPool != VK_NULL_HANDLE) vkDestroyCommandPool(device, frame.commandPool, nullptr);
            frame = Frame{};
        }

        if (renderPass != VK_NULL_HANDLE) vkDestroyRenderPass(device, renderPass, nullptr);
        if (pipelineCache != VK_NULL_HANDLE) vkDestroyPipelineCache(device, pipelineCache, nullptr);
        if (descriptorPool != VK_NULL_HANDLE) vkDestroyDescriptorPool(device, descriptorPool, nullptr);
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
