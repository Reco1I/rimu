import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The platform independent part of the engine: scenes, UI, graphics abstractions, audio, input and
// clocks. Its code is common Kotlin, so it can't depend on Android or the JVM, the platforms implement
// it, see :verktex:android. Only the JVM is targeted for now, which Android uses as well.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // With the JVM as the only target the common code could still use the JVM's API. Building with
    // -Pverktex.checkCommon adds a JavaScript target, which compiles it as common code against a
    // platform without it: ./gradlew -Pverktex.checkCommon :verktex:core:compileKotlinJs
    if (providers.gradleProperty("verktex.checkCommon").isPresent) {
        js {
            nodejs()
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        optIn.add("com.reco1l.verktex.VerktexPlatformApi")
    }

    sourceSets {
        commonMain.dependencies {
            // Multiplatform byte buffers, over direct java.nio buffers on the JVM and Android, for the
            // pixels the textures upload.
            api("com.ditchoom:buffer:6.30.8")

            // Multiplatform locks.
            implementation("org.jetbrains.kotlinx:atomicfu:0.33.0")
        }

        jvmTest.dependencies {
            implementation("junit:junit:4.13.2")
        }
    }
}
