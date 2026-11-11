import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

// The engine, one Kotlin Multiplatform module with a target per platform:
//
// - commonMain: everything platform independent (scenes, UI, graphics abstractions, audio, input,
//   clocks). It can't depend on Android or the JVM, and expects what differs per platform, e.g.
//   Platform.
// - jvmMain: what Android and desktop share because both run on a JVM, e.g. ThreadLoopWorker.
// - androidMain: the Android platform. Its native code is in :android-natives.
// - desktopMain: the desktop platform (OpenGL 3.3 through LWJGL and GLFW, BASS through JNA), which
//   also runs the UI tests, see docs/desktop-platform.md.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlinMultiplatformLibrary)
}

// The built-in shaders and fonts, a plain folder each platform packages: as assets on Android, and
// as resources on desktop.
val sharedAssets: Directory = layout.projectDirectory.dir("assets")

val lwjglNatives = listOf("natives-windows", "natives-linux", "natives-linux-arm64", "natives-macos", "natives-macos-arm64")

kotlin {
    // The default hierarchy, with a source set between the common one and the JVM platforms.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvm") {
                withJvm()
                withCompilations { it.target.name == "android" }
            }
        }
    }

    android {
        namespace = "com.reco1l.verktex"
        compileSdk {
            version = release(36) { minorApiLevel = 1 }
        }
        minSdk = 24

        // The bindings of BASS are Java.
        withJava()

        // Assets, such as the built-in shaders and fonts, are only packaged with the resources.
        androidResources {
            enable = true
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.keep")
            }
        }

        withHostTest {
            isIncludeAndroidResources = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop") {
        compilerOptions {
            // Compiled against the API of Java 11, the newest whose java.nio.ByteBuffer is declared as
            // Android declares it (abstract, sealed from 17), so that both can be the actual
            // PixelBuffer of one expected class.
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xjdk-release=11")
        }
    }

    // With only JVM targets the common code could still use the JVM's API. Building with
    // -Pverktex.checkCommon adds a JavaScript target, which compiles it as common code against a
    // platform without it: ./gradlew -Pverktex.checkCommon :verktex:compileKotlinJs
    if (providers.gradleProperty("verktex.checkCommon").isPresent) {
        js {
            nodejs()
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("com.reco1l.verktex.VerktexPlatformApi")
    }

    sourceSets {
        commonMain.dependencies {
            // Files, streams and hashing, see Platform.fileSystem.
            api(libs.okio)

            // Multiplatform locks.
            implementation(libs.atomicfu)

            // HTTP, see Network. Each platform provides its engine.
            api(libs.ktor.client.core)

            // JSON, e.g. the strings of the localizations and the settings, whose keys expose
            // their serializer.
            api(libs.kotlinx.serialization.json)
        }

        if (providers.gradleProperty("verktex.checkCommon").isPresent) {
            jsMain.dependencies {
                implementation(libs.okio.nodefilesystem)
            }
        }

        getByName("jvmMain").dependencies {
            // The HTTP engine of Network.
            implementation(libs.ktor.client.okhttp)
        }

        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)

            // Video playback, with FFmpeg for the codecs the device lacks (see maven/README.md).
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.decoder.ffmpeg)

            // Shader translation and uniform layouts, shared with the build that compiles the
            // Vulkan shaders.
            implementation(project(":shadertools"))

            // The native library of the Vulkan backend and the SPIR-V of the built-in shaders.
            implementation(project(":android-natives"))
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
        }

        getByName("desktopMain") {
            // The built-in shaders and fonts load from the classpath.
            resources.srcDir(sharedAssets)

            dependencies {
                implementation(project.dependencies.platform(libs.lwjgl.bom))
                for (module in listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb")) {
                    implementation("org.lwjgl:$module")
                    for (natives in lwjglNatives) {
                        runtimeOnly("org.lwjgl:$module::$natives")
                    }
                }

                implementation(libs.jna)
            }
        }

        getByName("desktopTest").dependencies {
            implementation(libs.junit)
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addStaticSourceDirectory(sharedAssets.asFile.path)
    }
}

// FFmpeg for videos, see docs/desktop-platform.md: LGPL shared builds of BtbN/FFmpeg-Builds, pinned
// with their checksums. The dated autobuilds are pruned after a while; when the download fails,
// pick a newer one of the same major versions (avformat 62, avcodec 62, avutil 60, swscale 9) and
// update the release, the assets and the checksums. FFmpegVideoDecoder reads structs at the offsets
// of these versions and refuses others.
val ffmpegRelease = "autobuild-2026-11-10-12-39"
val ffmpegBuilds = mapOf(
    "win32-x86-64" to ("ffmpeg-n8.1.3-34-ga35157abb9-win64-lgpl-shared-8.1.zip" to
        "3045c2213ed8cbb05edf0d5a631b4490061de3b7567ec145644156e7c98da2c6"),
    "linux-x86-64" to ("ffmpeg-n8.1.3-34-ga35157abb9-linux64-lgpl-shared-8.1.tar.xz" to
        "cde02ff1124f60f36dd732bbf6cbe3ff3646e95c7fc0f11736cb92e05e90a485"),
    "linux-aarch64" to ("ffmpeg-n8.1.3-34-ga35157abb9-linuxarm64-lgpl-shared-8.1.tar.xz" to
        "7bd06a245816c2c84d8ef2d90e34e6a4609b70dd1c958d676182942c1defda5f"),
)

// JNA's name for the platform, which FFmpegVideoDecoder looks the libraries up under.
val ffmpegPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val cpu = if (arch == "aarch64" || arch == "arm64") "aarch64" else "x86-64"
    when {
        "windows" in os -> "win32-$cpu"
        "linux" in os -> "linux-$cpu"
        else -> "darwin-$cpu"
    }
}

// The libraries, in the order they're loaded: each after the ones it needs.
val ffmpegLibraries = listOf("avutil" to 60, "swresample" to 6, "swscale" to 9, "avcodec" to 62, "avformat" to 62)

val prepareFFmpeg by tasks.registering {
    group = "build setup"
    description = "Downloads FFmpeg's shared libraries for this platform into build/ffmpeg, for videos."

    val build = ffmpegBuilds[ffmpegPlatform]
    val output = layout.buildDirectory.dir("ffmpeg/$ffmpegPlatform")
    // Kept out of build/ so that a clean doesn't download it again.
    val cache = gradle.gradleUserHomeDir.resolve("caches/verktex-ffmpeg")
    // Plain values, the task can't refer to the script with the configuration cache.
    val release = ffmpegRelease
    val libraries = ffmpegLibraries

    onlyIf("There are no bundled FFmpeg builds for $ffmpegPlatform, a system FFmpeg is used") { build != null }
    inputs.property("asset", build?.first ?: "")
    outputs.dir(output)

    doLast {
        val (asset, checksum) = build!!
        val archive = cache.resolve(asset)

        fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

        if (!archive.exists() || sha256(archive) != checksum) {
            cache.mkdirs()
            val url = "https://github.com/BtbN/FFmpeg-Builds/releases/download/$release/$asset"
            logger.lifecycle("Downloading $url")
            try {
                URI(url).toURL().openStream().use { input -> archive.outputStream().use { input.copyTo(it) } }
            } catch (e: Exception) {
                throw GradleException("Couldn't download FFmpeg from $url, see the comment above prepareFFmpeg to pin a newer build.", e)
            }
            check(sha256(archive) == checksum) { "The checksum of $asset doesn't match the pinned one." }
        }

        val extracted = temporaryDir.resolve("extracted")
        extracted.deleteRecursively()
        extracted.mkdirs()

        if (asset.endsWith(".zip")) {
            ZipFile(archive).use { zip ->
                for (entry in zip.entries()) {
                    val file = extracted.resolve(entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile.mkdirs()
                        zip.getInputStream(entry).use { input -> file.outputStream().use { input.copyTo(it) } }
                    }
                }
            }
        } else {
            // Gradle's tarTree reads no xz, tar does.
            val process = ProcessBuilder("tar", "-xJf", archive.absolutePath, "-C", extracted.absolutePath).inheritIO().start()
            check(process.waitFor() == 0) { "Couldn't extract $asset with tar." }
        }

        val root = extracted.listFiles()!!.single()
        val target = output.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        for ((name, major) in libraries) {
            val file = if (asset.endsWith(".zip")) root.resolve("bin/$name-$major.dll") else root.resolve("lib/lib$name.so.$major")
            // Copying follows the symbolic links of the Linux archives.
            file.copyTo(target.resolve(file.name), overwrite = true)
        }
        root.resolve("LICENSE.txt").copyTo(target.resolve("FFMPEG-LICENSE.txt"), overwrite = true)
    }
}

// The UI test browser on desktop: ./gradlew :verktex:run --args="--test 'Verktex/Text'"
tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs the UI test browser of Verktex on desktop."

    val main = kotlin.targets.getByName("desktop").compilations.getByName("main") as KotlinJvmCompilation
    classpath = files(main.output.allOutputs, main.runtimeDependencyFiles)
    mainClass.set("com.reco1l.verktex.desktop.testing.VerktexUITestsMainKt")

    // GLFW has to run on the first thread on macOS.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }

    // Relative paths given in --args, e.g. --screenshot, resolve against the root of Verktex.
    workingDir = rootProject.projectDir

    dependsOn(prepareFFmpeg)
    systemProperty("verktex.ffmpeg.path", layout.buildDirectory.dir("ffmpeg").get().asFile.absolutePath)
}
