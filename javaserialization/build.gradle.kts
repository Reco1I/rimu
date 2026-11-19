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
    `maven-publish`
}

// Published as com.reco1l:java-serialization, with an artifact per target
// (java-serialization-jvm, java-serialization-android) that Gradle picks from the root one's
// metadata. ./gradlew :javaserialization:publishToMavenLocal publishes it to ~/.m2, and
// :javaserialization:publishAllPublicationsToBuildRepository to build/repository, to look at.
group = "com.reco1l"
version = "0.1.0"

val artifactName = "java-serialization"

publishing {
    publications.withType<MavenPublication>().configureEach {
        // The module's name, javaserialization, is what Kotlin names them after (the targets' are
        // renamed by kotlin.targets, as Kotlin names them later).
        artifactId = artifactId.replace(project.name, artifactName)

        pom {
            name.set("Java Serialization")
            description.set(
                "Reads Java's object serialization streams without the JVM, and decodes them into " +
                    "@Serializable classes with a kotlinx.serialization format. Kotlin Multiplatform."
            )
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("Reco1l")
                    name.set("Reco1l")
                }
            }
        }
    }

    repositories {
        maven {
            name = "build"
            url = uri(layout.buildDirectory.dir("repository"))
        }
    }
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

    // Each target's publication, named after the module by Kotlin, see publishing below.
    targets.configureEach {
        mavenPublication {
            artifactId = artifactId.replace(project.name, artifactName)
        }
    }

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
