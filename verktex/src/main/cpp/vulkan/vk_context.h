#pragma once

#include <vulkan/vulkan.h>
#include <android/native_window.h>

#include <cstdint>
#include <deque>
#include <functional>
#include <mutex>
#include <string>
#include <vector>

namespace verktex {

// Frames the CPU may record ahead of the GPU. Each has its own command buffer, ring and staging,
// which are only rewritten once the frame's fence says the GPU is done with them.
constexpr uint32_t FRAMES_IN_FLIGHT = 2;

// The per-frame buffer holding the frame's vertex, instance and uniform data, written by Kotlin
// through a direct ByteBuffer. See VKFrameRing.
constexpr VkDeviceSize RING_SIZE = 4 * 1024 * 1024;

// The size of the dynamic uniform buffer descriptors. Every uniform block must fit in it.
constexpr VkDeviceSize UNIFORM_RANGE = 256;

// The smallest staging chunk for texture uploads, larger uploads get a chunk of their size.
constexpr VkDeviceSize STAGING_CHUNK_SIZE = 4 * 1024 * 1024;

// The opcodes of the command stream Kotlin encodes, see VKCommandEncoder.
enum Opcode : int32_t {
    // pipelineLow, pipelineHigh, scissorX, scissorY, scissorWidth, scissorHeight,
    // vertexOffset, count, vertexUniformsOffset, fragmentUniformsOffset, textureLow, textureHigh,
    // instanceOffset, instanceCount, indexed
    // count is vertices, or quads when indexed. A texture of 0 binds the white texture.
    OP_DRAW = 1,
    OP_DRAW_INTS = 15,

    // Clears the depth buffer.
    OP_CLEAR_DEPTH = 2,

    // textureLow, textureHigh: copies what was drawn so far into the texture, top-left origin.
    OP_CAPTURE = 3,
    OP_CAPTURE_INTS = 2,
};

struct Texture {
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkImageView view = VK_NULL_HANDLE;
    VkDescriptorPool descriptorPool = VK_NULL_HANDLE;
    VkDescriptorSet descriptorSet = VK_NULL_HANDLE;
    uint32_t width = 0;
    uint32_t height = 0;
};

struct Buffer {
    VkBuffer buffer = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    void *mapped = nullptr;
    VkDeviceSize size = 0;
};

struct StagingChunk {
    Buffer buffer;
    VkDeviceSize used = 0;
};

// A texture operation recorded before the frame's render pass.
struct TextureOperation {
    Texture *texture;
    bool clear;              // A new texture is cleared to transparent, as it's sampled before any upload.
    VkBuffer source;         // Otherwise a copy from staging.
    VkDeviceSize sourceOffset;
    int32_t x, y, width, height;
};

struct Frame {
    VkCommandPool commandPool = VK_NULL_HANDLE;
    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    VkFence fence = VK_NULL_HANDLE;
    VkSemaphore imageAvailable = VK_NULL_HANDLE;

    Buffer ring;
    VkDescriptorSet uniformSet = VK_NULL_HANDLE;

    std::vector<StagingChunk> staging;

    // The submission this slot last made, complete once its fence is signaled.
    uint64_t submission = 0;
    bool timestampsWritten = false;
};

class Context {
public:
    // Creates the instance, device and per-frame resources. On failure, error says why and the
    // context must be destroyed.
    bool initialize(bool enableValidation, std::string &error);
    void destroy();

    // Replaces the window rendered to, null to release it. Takes ownership of the window reference.
    // Returns 1 when the render passes were recreated (pipelines must be recreated), 0 otherwise, or
    // -1 on failure.
    int setSurface(ANativeWindow *window, std::string &error);

    // Waits for the next frame's resources and acquires a swapchain image of the given size,
    // (re)creating the swapchain if needed. Returns the frame index, or -1 to skip the frame.
    int acquireFrame(uint32_t width, uint32_t height);

    // Records the frame's texture operations and the command stream and submits it.
    void submit(const int32_t *commands, int32_t count);

    // Presents the submitted frame. Apart from submit as it may wait for the display (vsync), like
    // eglSwapBuffers, which isn't part of the time spent submitting.
    void present();

    // Milliseconds the GPU took for the last measured frame, -1 when unknown.
    float gpuTime() const { return lastGpuTime; }

    VkShaderModule createShaderModule(const uint32_t *code, size_t size);
    void destroyShaderModule(VkShaderModule module);

    // vertexInput: bindingCount, (stride, inputRate) per binding, attributeCount,
    // (location, binding, format, offset) per attribute. blend: the 4 VkBlendFactors.
    // depth: test enabled, write enabled, VkCompareOp.
    VkPipeline createPipeline(VkShaderModule vertex, VkShaderModule fragment,
                              const int32_t *vertexInput, int32_t vertexInputCount,
                              const int32_t *blend, const int32_t *depth);
    void destroyPipeline(VkPipeline pipeline);

    // Textures may be created and destroyed from any thread, destruction waits for the GPU.
    Texture *createTexture(uint32_t width, uint32_t height);
    void uploadTexture(Texture *texture, int32_t x, int32_t y, int32_t width, int32_t height,
                       const void *pixels, size_t size);
    void destroyTexture(Texture *texture);

    void *ringMemory(uint32_t frameIndex) const { return frames[frameIndex].ring.mapped; }

    const VkPhysicalDeviceProperties &properties() const { return physicalProperties; }

private:
    bool createDeviceObjects(std::string &error);
    bool createFrames(std::string &error);
    bool createRenderPasses();
    void destroyRenderPasses();
    bool createSwapchain(uint32_t width, uint32_t height);
    void destroySwapchain(bool keepHandle);
    bool createDepthImage();
    void destroyDepthImage();

    bool createBuffer(VkDeviceSize size, VkBufferUsageFlags usage, Buffer &buffer);
    void destroyBuffer(Buffer &buffer);
    uint32_t findMemoryType(uint32_t typeBits, VkMemoryPropertyFlags properties) const;
    VkDescriptorSet allocateTextureSet(VkDescriptorPool &pool);
    bool ensureQuadIndices(uint32_t quads);

    // Waits for the current slot's previous use and resets it, once per frame. Uploads may come
    // before the frame is acquired, they open it as well.
    void openSlot();
    void collectGarbage(uint64_t completedSubmission);
    void deferDestroy(std::function<void()> destroy);

    void recordTextureOperations(VkCommandBuffer cmd);
    void beginRenderPass(VkCommandBuffer cmd, bool load, bool storeDepth);
    void recordCapture(VkCommandBuffer cmd, Texture *texture);

    // Guards everything, textures can be created and released from threads other than the draw one.
    std::recursive_mutex mutex;

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
    VkDescriptorPool uniformPool = VK_NULL_HANDLE;
    std::vector<VkDescriptorPool> texturePools;
    VkPipelineCache pipelineCache = VK_NULL_HANDLE;
    VkSampler sampler = VK_NULL_HANDLE;

    Texture *whiteTexture = nullptr;
    Buffer quadIndices;
    uint32_t quadCapacity = 0;

    VkQueryPool timestamps = VK_NULL_HANDLE;
    float lastGpuTime = -1.0f;

    Frame frames[FRAMES_IN_FLIGHT];
    uint32_t frameIndex = 0;
    bool slotOpen = false;
    uint64_t submissions = 0;

    std::vector<TextureOperation> textureOperations;
    std::deque<std::pair<uint64_t, std::function<void()>>> garbage;

    ANativeWindow *window = nullptr;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkFormat surfaceFormat = VK_FORMAT_UNDEFINED;
    VkFormat depthFormat = VK_FORMAT_UNDEFINED;
    bool canCapture = false;

    // [load][storeDepth]: the first pass of a frame clears, the ones after a capture load. Depth is
    // only stored when a later pass of the frame loads it.
    VkRenderPass renderPasses[2][2]{};

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

    VkImage depthImage = VK_NULL_HANDLE;
    VkDeviceMemory depthMemory = VK_NULL_HANDLE;
    VkImageView depthView = VK_NULL_HANDLE;

    uint32_t imageIndex = 0;
    bool imageAcquired = false;
    bool frameSubmitted = false;
};

} // namespace verktex
