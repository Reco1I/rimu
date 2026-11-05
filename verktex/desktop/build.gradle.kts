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

tasks.named<JavaExec>("run") {
    // Relative paths given in --args, e.g. --screenshot, resolve against where Gradle was started.
    workingDir = rootProject.projectDir
}
