import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shader tooling shared by the engine at runtime and the build: translating the GLSL ES 3.00
// shaders for Vulkan and describing their uniform layout. Plain JVM, so the build can run it.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    testImplementation(libs.junit)
}
