import java.util.Properties

// What the Android platform needs from the NDK, which the Kotlin Multiplatform Android plugin of
// :verktex can't build: the native library of the Vulkan backend (see docs/vulkan-backend.md), the
// SPIR-V of the built-in shaders, compiled with the NDK's glslc, and the Vulkan validation layer of
// debug builds. It has no code of its own, the androidMain of :verktex depends on it.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.reco1l.verktex.natives"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    ndkVersion = "22.1.7171670"

    defaultConfig {
        minSdk = 24

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}


// Debug builds can package the Vulkan validation layer, which the backend enables when it's present.
// The NDK's own (1.2.158) crashes in vkCreateInstance on recent Android versions, so a current build
// from https://github.com/KhronosGroup/Vulkan-ValidationLayers/releases (android-binaries-*.zip) is
// used instead: pass its extracted folder, with one folder per ABI, as -Pverktex.validationLayers=<dir>
// or set verktex.validationLayers in the local.properties of Verktex's root.
abstract class CopyValidationLayers : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val layersDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val files: FileSystemOperations

    @TaskAction
    fun copy() {
        files.sync {
            from(layersDirectory)
            include("**/libVkLayer_khronos_validation.so")
            into(outputDirectory)
        }
    }
}

val validationLayers: String? = providers.gradleProperty("verktex.validationLayers").orNull
    ?: rootProject.file("local.properties").takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use(::load) }.getProperty("verktex.validationLayers")
    }

// Vulkan shaders: the built-in GLSL ES shaders are translated and compiled to SPIR-V with the NDK's
// glslc, and packaged next to them as <name>.spv. Without the NDK they are skipped with a warning.
abstract class CompileVulkanShaders : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val shaderDirectory: DirectoryProperty

    @get:Input
    abstract val glslc: Property<String>

    @get:Classpath
    abstract val toolClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun compile() {
        // The assets keep their path, the SPIR-V of shaders/main.vert is shaders/main.vert.spv.
        val output = outputDirectory.get().dir("shaders").asFile

        exec.javaexec {
            classpath = toolClasspath
            mainClass.set("com.reco1l.verktex.shaders.CompileVulkanShadersKt")
            args(shaderDirectory.get().asFile.path, output.path, glslc.get())
        }
    }
}

val vulkanShaderTools: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
    }
}

dependencies {
    vulkanShaderTools(project(":shadertools"))
}

// The built-in shaders, which :verktex packages itself.
val sharedAssets: Directory = rootProject.layout.projectDirectory.dir("verktex/assets")

val compileVulkanShaders = tasks.register<CompileVulkanShaders>("compileVulkanShaders") {
    val os = System.getProperty("os.name").lowercase()
    val host = when {
        "windows" in os -> "windows-x86_64"
        "mac" in os -> "darwin-x86_64"
        else -> "linux-x86_64"
    }
    val name = if ("windows" in os) "glslc.exe" else "glslc"

    shaderDirectory.set(sharedAssets.dir("shaders"))
    toolClasspath.from(vulkanShaderTools)
    outputDirectory.set(layout.buildDirectory.dir("generated/vulkanShaders"))
    glslc.set(androidComponents.sdkComponents.ndkDirectory
        .map { it.file("shader-tools/$host/$name").asFile.path }
        .orElse(""))
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(compileVulkanShaders, CompileVulkanShaders::outputDirectory)
    }
}

if (validationLayers != null) {
    val copyValidationLayers = tasks.register<CopyValidationLayers>("copyValidationLayers") {
        layersDirectory.set(file(validationLayers))
        outputDirectory.set(layout.buildDirectory.dir("generated/validationLayers"))
    }

    androidComponents {
        onVariants(selector().withBuildType("debug")) { variant ->
            variant.sources.jniLibs?.addGeneratedSourceDirectory(copyValidationLayers, CopyValidationLayers::outputDirectory)
        }
    }
}
