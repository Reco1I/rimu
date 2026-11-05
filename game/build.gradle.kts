import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The platform independent part of the game: gameplay, beatmaps, difficulty, mods, scoring and UI.
// Its code is common Kotlin, so it can't depend on Android or the JVM, the platforms provide what it
// needs, see docs/multiplatform-game.md. Only the JVM is targeted for now, which Android uses as well.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // With the JVM as the only target the common code could still use the JVM's API. Building with
    // -Pgame.checkCommon adds a JavaScript target, which compiles it as common code against a platform
    // without it. Verktex needs its own check enabled too, for its JavaScript variant:
    // ./gradlew -Pverktex.checkCommon -Pgame.checkCommon :game:compileKotlinJs
    if (providers.gradleProperty("game.checkCommon").isPresent) {
        js {
            nodejs()
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":verktex:core"))
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

            // Files, streams and hashing.
            api("com.squareup.okio:okio:3.9.1")

            // Key-value preferences, over SharedPreferences on Android.
            api("com.russhwolf:multiplatform-settings:1.3.0")
        }

        if (providers.gradleProperty("game.checkCommon").isPresent) {
            jsMain.dependencies {
                implementation("com.squareup.okio:okio-nodefilesystem:3.9.1")
            }
        }

        jvmTest.dependencies {
            implementation("junit:junit:4.13.2")
        }
    }
}
