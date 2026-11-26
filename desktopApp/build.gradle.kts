import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The desktop application: the entry points of the game on desktop (OsuDesktop, the UI test browser),
// over the desktop target of :shared and of Verktex. See docs/multiplatform-game.md.
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

dependencies {
    implementation(project(":shared"))
    implementation("com.reco1l.verktex:shared")
}

// The game's assets (the default skin, sounds, fonts) and its strings, generated from the language
// pack, which Android packages as its assets, load from the classpath.
sourceSets.main {
    resources.srcDir(files(project(":shared").layout.buildDirectory.dir("generated/resources/assets")).builtBy(":shared:generateStringResources"))
    resources.srcDir(rootProject.layout.projectDirectory.dir("shared/assets"))
}

// The version and build type OsuDesktop reads (DESKTOP_APPLICATION): a debug build without a version
// unless built with -PdesktopVersion and -PdesktopBuildType=release, as the release workflow does.
val generateApplicationProperties by tasks.registering(WriteProperties::class) {
    destinationFile.set(layout.buildDirectory.file("generated/application/osudroid-desktop.properties"))
    property("versionName", providers.gradleProperty("desktopVersion").orElse("desktop"))
    property("buildType", providers.gradleProperty("desktopBuildType").orElse("debug"))
}

sourceSets.main {
    resources.srcDir(files(layout.buildDirectory.dir("generated/application")).builtBy(generateApplicationProperties))
}

application {
    mainClass.set("com.osudroid.desktop.OsuDesktopKt")
    // The name of the distributions and of their start scripts (bin/osudroid-reci), apart from
    // upstream osu!droid's.
    applicationName = "osudroid-reci"

    // GLFW has to run on the first thread on macOS.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

// FFmpeg's libraries for videos, downloaded by Verktex's desktop platform.
val ffmpegDirectory = rootProject.layout.projectDirectory.dir("verktex/shared/build/ffmpeg").asFile.absolutePath

tasks.named<JavaExec>("run") {
    // Relative paths given in --args, e.g. --screenshot, resolve against where Gradle was started.
    workingDir = rootProject.projectDir

    dependsOn(gradle.includedBuild("verktex").task(":shared:prepareFFmpeg"))
    systemProperty("verktex.ffmpeg.path", ffmpegDirectory)
}

// The UI test browser of osu!droid, with Verktex's suites too.
tasks.register<JavaExec>("runUITests") {
    group = "application"
    description = "Runs the UI test browser of osu!droid on desktop."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.osudroid.desktop.OsuUITestsDesktopKt")
    jvmArgs = application.applicationDefaultJvmArgs.toList()
    workingDir = rootProject.projectDir

    dependsOn(gradle.includedBuild("verktex").task(":shared:prepareFFmpeg"))
    systemProperty("verktex.ffmpeg.path", ffmpegDirectory)
}

// Distributions (installDist, distZip, distTar) bundle FFmpeg's libraries for the platform they're
// built on in ffmpeg/, where OsuDesktop finds them. They need a Java 17 runtime to run.
distributions.main {
    contents {
        from(ffmpegDirectory) {
            into("ffmpeg")
        }
    }
}

for (task in listOf("installDist", "distZip", "distTar")) {
    tasks.named(task) {
        dependsOn(gradle.includedBuild("verktex").task(":shared:prepareFFmpeg"))
    }
}
