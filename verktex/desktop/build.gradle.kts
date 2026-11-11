import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The desktop platform: OpenGL 3.3 through LWJGL and GLFW, BASS through JNA. For now it runs
// Verktex's UI tests, see docs/desktop-platform.md. The game never depends on it.
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        optIn.add("com.reco1l.verktex.VerktexPlatformApi")
    }
}

val lwjglVersion = "3.4.3"
val lwjglNatives = listOf("natives-windows", "natives-linux", "natives-linux-arm64", "natives-macos", "natives-macos-arm64")

dependencies {
    implementation(project(":verktex:core"))

    // The HTTP engine of Network.
    implementation("io.ktor:ktor-client-okhttp:3.1.3")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    for (module in listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb")) {
        implementation("org.lwjgl:$module")
        for (natives in lwjglNatives) {
            runtimeOnly("org.lwjgl:$module::$natives")
        }
    }

    implementation("net.java.dev.jna:jna:5.19.1")

    testImplementation("junit:junit:4.13.2")
}

// The built-in shaders and fonts, shared with the other platforms, load from the classpath.
sourceSets.main {
    resources.srcDir(rootProject.layout.projectDirectory.dir("verktex/core/assets"))
}

application {
    mainClass.set("com.reco1l.verktex.desktop.testing.VerktexUITestsMainKt")

    // GLFW has to run on the first thread on macOS.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
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

tasks.named<JavaExec>("run") {
    // Relative paths given in --args, e.g. --screenshot, resolve against where Gradle was started.
    workingDir = rootProject.projectDir

    dependsOn(prepareFFmpeg)
    systemProperty("verktex.ffmpeg.path", layout.buildDirectory.dir("ffmpeg").get().asFile.absolutePath)
}
