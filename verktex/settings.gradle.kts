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
        // Libraries the Android platform uses that are published nowhere, see android/maven/README.md.
        maven { url = uri(rootDir.resolve("android/maven")) }
        google()
        mavenCentral()
    }
}

rootProject.name = "verktex"

// Everything platform independent.
include(":core")

// The Android platform, which brings the core with it.
include(":android")

// The desktop platform, which also runs the UI tests.
include(":desktop")

// The shader translator, used by the Vulkan backend and by the build.
include(":shadertools")
