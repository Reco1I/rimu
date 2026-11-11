import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The platform independent part of the engine: scenes, UI, graphics abstractions, audio, input and
// clocks. Its code is common Kotlin, so it can't depend on Android or the JVM, the platforms implement
// it, see :android. Only the JVM is targeted for now, which Android uses as well.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // With the JVM as the only target the common code could still use the JVM's API. Building with
    // -Pverktex.checkCommon adds a JavaScript target, which compiles it as common code against a
    // platform without it: ./gradlew -Pverktex.checkCommon :core:compileKotlinJs
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

        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}
