import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.w3c.dom.Element

// The platform independent part of the game: gameplay, beatmaps, difficulty, mods, scoring and UI.
// Its code is common Kotlin, so it can't depend on Android or the JVM, the platforms provide what it
// needs, see docs/multiplatform-game.md. Only the JVM is targeted for now, which Android uses as well.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// The localizations come from the language pack, an Android library. Its resources are read where
// each platform runs: by Android itself, and from these same files on the other platforms. The game
// refers to them through Res, generated from the language pack's names.
val languagePack: Configuration by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    languagePack("com.github.osudroid:language-pack:${providers.gradleProperty("languagePackVersion").get()}@aar")
}

/**
 * Generates Res, the keys of the strings and string arrays of the language pack.
 */
abstract class GenerateStringResources : DefaultTask() {

    @get:InputFiles
    abstract val languagePack: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val strings = sortedSetOf<String>()
        val arrays = sortedSetOf<String>()

        ZipFile(languagePack.singleFile).use { zip ->
            val entry = zip.getEntry("res/values/values.xml")
            val document = zip.getInputStream(entry).use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
            val resources = document.documentElement.childNodes

            for (i in 0 until resources.length) {
                val node = resources.item(i) as? Element ?: continue
                when (node.tagName) {
                    "string" -> strings += node.getAttribute("name")
                    "string-array" -> arrays += node.getAttribute("name")
                }
            }
        }

        val file = outputDirectory.get().file("com/osudroid/resources/Res.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(buildString {
            appendLine("// Generated from the language pack by :game:generateStringResources, don't edit.")
            appendLine("package com.osudroid.resources")
            appendLine()
            appendLine("@Suppress(\"ClassName\", \"ObjectPropertyName\", \"SpellCheckingInspection\")")
            appendLine("object Res {")
            appendLine("    object string {")
            strings.forEach { appendLine("        val `$it` = StringResource(\"$it\")") }
            appendLine("    }")
            appendLine()
            appendLine("    object array {")
            arrays.forEach { appendLine("        val `$it` = StringArrayResource(\"$it\")") }
            appendLine("    }")
            appendLine("}")
        })
    }
}

val generateStringResources by tasks.registering(GenerateStringResources::class) {
    languagePack.from(configurations.named("languagePack"))
    outputDirectory.set(layout.buildDirectory.dir("generated/resources/commonMain/kotlin"))
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
        commonMain {
            kotlin.srcDir(generateStringResources.map { it.outputDirectory })
        }

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
