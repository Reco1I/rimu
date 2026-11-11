// Verktex builds on its own, and applications include it as a composite build, e.g. from a git
// submodule: includeBuild("verktex") substitutes com.reco1l.verktex:<module> with these projects.
// See docs/extraction.md.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // Libraries the Android platform uses that are published nowhere, see maven/README.md.
        maven { url = uri(rootDir.resolve("maven")) }
        google()
        mavenCentral()
    }
}

rootProject.name = "verktex"

// The engine, with a target per platform.
include(":verktex")

// The native code and the compiled shaders of the Android platform.
include(":android-natives")

// The shader translator, used by the Vulkan backend and by the build.
include(":shadertools")
