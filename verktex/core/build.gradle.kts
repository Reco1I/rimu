import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The platform independent part of the engine: scenes, UI, graphics abstractions, audio, input and
// clocks. It must not depend on Android, the platforms implement it, see :verktex:android.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        optIn.add("com.reco1l.verktex.VerktexPlatformApi")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
