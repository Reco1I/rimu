#pragma once

#include <vulkan/vulkan.h>
#include <android/native_window.h>

#include <cstdint>
#include <string>
#include <vector>

namespace verktex {

// Frames the CPU may record ahead of the GPU. Each has its own command buffer and ring, which is
// only rewritten once the frame's fence says the GPU is done with it.
constexpr uint32_t FRAMES_IN_FLIGHT = 2;

// The per-frame buffer holding the frame's vertex and uniform data, written by Kotlin through a
// direct ByteBuffer. See VKFrameRing.
constexpr VkDeviceSize RING_SIZE = 4 * 1024 * 1024;

// The size of the dynamic uniform buffer descriptors. Every uniform block must fit in it.
constexpr VkDeviceSize UNIFORM_RANGE = 256;

// The opcodes of the command stream Kotlin encodes, see VKCommandEncoder.
enum Opcode : int32_t {
    // pipelineLow, pipelineHigh, scissorX, scissorY, scissorWidth, scissorHeight,
    // vertexOffset, vertexCount, vertexUniformsOffset, fragmentUniformsOffset
    OP_DRAW = 1,
};

struct Frame {
    VkCommandPool commandPool = VK_NULL_HANDLE;
    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    VkFence fence = VK_NULL_HANDLE;
    VkSemaphore imageAvailable = VK_NULL_HANDLE;

    VkBuffer ring = VK_NULL_HANDLE;
    VkDeviceMemory ringMemory = VK_NULL_HANDLE;
    void *ringMapped = nullptr;

    VkDescriptorSet uniformSet = VK_NULL_HANDLE;
};

class Context {
public:
    // Creates the instance, device and per-frame resources. On failure, error says why and the
    // context must be destroyed.
    bool initialize(bool enableValidation, std::string &error);
    void destroy();

    // Replaces the window rendered to, null to release it. Takes ownership of the window reference.
    // Returns 1 when the render pass was recreated (pipelines must be recreated), 0 otherwise, or
    // -1 on failure.
    int setSurface(ANativeWindow *window, std::string &error);

    // Waits for the next frame's resources and acquires a swapchain image of the given size,
    // (re)creating the swapchain if needed. Returns the frame index, or -1 to skip the frame.
    int acquireFrame(uint32_t width, uint32_t height);

    // Records the command stream into the acquired frame, submits and presents it.
    void submit(const int32_t *commands, int32_t count);

    VkShaderModule createShaderModule(const uint32_t *code, size_t size);
    void destroyShaderModule(VkShaderModule module);

    // vertexInput: bindingCount, (stride, inputRate) per binding, attributeCount,
    // (location, binding, format, offset) per attribute. blend: the 4 VkBlendFactors.
    VkPipeline createPipeline(VkShaderModule vertex, VkShaderModule fragment,
                              const int32_t *vertexInput, int32_t vertexInputCount,
                              const int32_t *blend);
    void destroyPipeline(VkPipeline pipeline);

    void *ringMemory(uint32_t frameIndex) const { return frames[frameIndex].ringMapped; }

    const VkPhysicalDeviceProperties &properties() const { return physicalProperties; }

private:
    bool createDeviceObjects(std::string &error);
    bool createFrames(std::string &error);
    bool createRenderPass(VkFormat format);
    bool createSwapchain(uint32_t width, uint32_t height);
    void destroySwapchain(bool keepHandle);
    uint32_t findMemoryType(uint32_t typeBits, VkMemoryPropertyFlags properties) const;

    VkInstance instance = VK_NULL_HANDLE;
    VkDebugUtilsMessengerEXT messenger = VK_NULL_HANDLE;

    VkPhysicalDevice physical = VK_NULL_HANDLE;
    VkPhysicalDeviceProperties physicalProperties{};
    VkPhysicalDeviceMemoryProperties memoryProperties{};
    VkDevice device = VK_NULL_HANDLE;
    uint32_t queueFamily = 0;
    VkQueue queue = VK_NULL_HANDLE;

    VkDescriptorSetLayout uniformSetLayout = VK_NULL_HANDLE;
    VkDescriptorSetLayout samplerSetLayout = VK_NULL_HANDLE;
    VkPipelineLayout pipelineLayout = VK_NULL_HANDLE;
    VkDescriptorPool descriptorPool = VK_NULL_HANDLE;
    VkPipelineCache pipelineCache = VK_NULL_HANDLE;

    Frame frames[FRAMES_IN_FLIGHT];
    uint32_t frameIndex = 0;

    ANativeWindow *window = nullptr;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkFormat surfaceFormat = VK_FORMAT_UNDEFINED;
    VkRenderPass renderPass = VK_NULL_HANDLE;

    VkSwapchainKHR swapchain = VK_NULL_HANDLE;
    VkExtent2D extent{};
    uint32_t requestedWidth = 0;
    uint32_t requestedHeight = 0;
    bool swapchainOutdated = false;
    std::vector<VkImage> images;
    std::vector<VkImageView> imageViews;
    std::vector<VkFramebuffer> framebuffers;
    // One per image: a semaphore is only reused once the present that waited on it is done, which
    // the acquire of the same image guarantees.
    std::vector<VkSemaphore> renderFinished;

    uint32_t imageIndex = 0;
    bool imageAcquired = false;
};

} // namespace verktex
