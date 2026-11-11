import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.builtInKotlin)
}

kotlin {
   compilerOptions {
       freeCompilerArgs.add("-Xexplicit-backing-fields")
       optIn.add("com.reco1l.verktex.VerktexPlatformApi")
   }
}

android {
    namespace = "com.reco1l.verktex"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    ndkVersion = "22.1.7171670"

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    // The Vulkan backend, see docs/vulkan-backend.md.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {

    // Android specific
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    api(project(":core"))

    // The HTTP engine of Network.
    implementation(libs.ktor.client.okhttp)

    // Video playback, with FFmpeg for the codecs the device lacks (see maven/README.md).
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.decoder.ffmpeg)

    // Shader translation and uniform layouts, shared with the build that compiles the Vulkan shaders.
    implementation(project(":shadertools"))

    //implementation("com.google.android.material:material:1.14.0")
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    //androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //androidTestImplementation("androidx.test.ext:junit:1.1.5")

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

// The engine's built-in shaders and fonts, shared by every platform: a plain folder of the core, so
// they aren't packaged twice (as assets and as the core's jar resources).
val sharedAssets: Directory = rootProject.layout.projectDirectory.dir("core/assets")

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
        variant.sources.assets?.addStaticSourceDirectory(sharedAssets.asFile.path)
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
