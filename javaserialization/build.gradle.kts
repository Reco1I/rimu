import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Reads Java's object serialization without the JVM: JavaObjectInputStream, and JavaSerialization,
// a decode-only kotlinx.serialization format over it. It never depends on the game or Verktex,
// meant to be published as a library of its own. It has the game's targets, android and jvm, all
// common code (checkCommonSources checks it).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    // For the tests' @Serializable classes.
    id("org.jetbrains.kotlin.plugin.serialization")
}

val checkCommonSources by tasks.registering(CheckCommonSources::class) {
    sources.from(layout.projectDirectory.dir("src/commonMain/kotlin"))
    report.set(layout.buildDirectory.file("reports/checkCommonSources.txt"))
}

tasks.matching { it.name == "check" || it.name == "jvmTest" }.configureEach {
    dependsOn(checkCommonSources)
}

kotlin {
    android {
        namespace = "com.reco1l.javaserialization"
        compileSdk {
            version = release(36) { minorApiLevel = 1 }
        }
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    explicitApi()

    sourceSets {
        commonMain.dependencies {
            api("com.squareup.okio:okio:3.9.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
        }

        jvmTest.dependencies {
            implementation("junit:junit:4.13.2")
        }
    }
}
