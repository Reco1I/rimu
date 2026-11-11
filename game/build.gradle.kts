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
 * Generates Res, the keys of the strings and string arrays of the language pack, and the strings of
 * each of its languages as `strings/<locale>.json` assets, which Verktex's Localization reads.
 */
abstract class GenerateStringResources : DefaultTask() {

    @get:InputFiles
    abstract val languagePack: ConfigurableFileCollection

    /**
     * English resource files with strings that aren't in the language pack yet.
     */
    @get:InputFiles
    abstract val extraStrings: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val assetsDirectory: DirectoryProperty

    private class Strings {
        val strings = sortedMapOf<String, String>()
        val arrays = sortedMapOf<String, List<String>>()
    }

    @TaskAction
    fun generate() {
        val locales = sortedMapOf<String, Strings>()

        fun read(input: java.io.InputStream, into: Strings) {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
            val resources = document.documentElement.childNodes

            for (i in 0 until resources.length) {
                val node = resources.item(i) as? Element ?: continue
                val name = node.getAttribute("name")
                when (node.tagName) {
                    "string" -> into.strings[name] = unescape(node.textContent)
                    "string-array" -> {
                        val items = node.getElementsByTagName("item")
                        into.arrays[name] = List(items.length) { unescape(items.item(it).textContent) }
                    }
                }
            }
        }

        val english = locales.getOrPut("en") { Strings() }

        // The language pack's strings replace the ones not in it yet, as Android resolves them.
        extraStrings.forEach { file -> file.inputStream().use { read(it, english) } }

        ZipFile(languagePack.singleFile).use { zip ->
            val folder = Regex("^res/values(-[^/]+)?/[^/]+\\.xml$")
            for (entry in zip.entries()) {
                val match = folder.matchEntire(entry.name) ?: continue
                val locale = match.groupValues[1].removePrefix("-").ifEmpty { "en" }.replace("-r", "-")
                zip.getInputStream(entry).use { read(it, locales.getOrPut(locale) { Strings() }) }
            }
        }

        val file = outputDirectory.get().file("com/osudroid/resources/Res.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(buildString {
            appendLine("// Generated from the language pack by :game:generateStringResources, don't edit.")
            appendLine("package com.osudroid.resources")
            appendLine()
            appendLine("import com.reco1l.verktex.localization.LocalizedString")
            appendLine("import com.reco1l.verktex.localization.LocalizedStringArray")
            appendLine("import kotlin.jvm.JvmField")
            appendLine()
            appendLine("@Suppress(\"ClassName\", \"ObjectPropertyName\", \"SpellCheckingInspection\")")
            appendLine("object Res {")
            appendLine("    object string {")
            english.strings.keys.forEach { appendLine("        @JvmField val `$it` = LocalizedString(\"$it\")") }
            appendLine("    }")
            appendLine()
            appendLine("    object array {")
            english.arrays.keys.forEach { appendLine("        @JvmField val `$it` = LocalizedStringArray(\"$it\")") }
            appendLine("    }")
            appendLine("}")
        })

        val strings = assetsDirectory.get().dir("strings").asFile
        strings.deleteRecursively()
        strings.mkdirs()
        for ((locale, values) in locales) {
            val content = sortedMapOf<String, Any>()
            content.putAll(values.strings)
            content.putAll(values.arrays)
            strings.resolve("$locale.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(content)))
        }
    }

    // Android's rules for string resources: quotes keep whitespace and aren't shown, whitespace
    // outside them collapses into a space, and backslashes escape characters.
    private fun unescape(text: String) = buildString {
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> inQuotes = !inQuotes

                c == '\\' && i + 1 < text.length -> {
                    i++
                    when (val next = text[i]) {
                        'n' -> append('\n')
                        't' -> append('\t')
                        'u' -> if (i + 4 < text.length) {
                            append(text.substring(i + 1, i + 5).toInt(16).toChar())
                            i += 4
                        }
                        else -> append(next)
                    }
                }

                c.isWhitespace() && !inQuotes -> if (isNotEmpty() && last() != ' ') append(' ')

                else -> append(c)
            }
            i++
        }
    }.trim(' ')
}

val generateStringResources by tasks.registering(GenerateStringResources::class) {
    languagePack.from(configurations.named("languagePack"))
    extraStrings.from(rootProject.layout.projectDirectory.file("res/values/strings_temporary_for_locals.xml"))
    outputDirectory.set(layout.buildDirectory.dir("generated/resources/commonMain/kotlin"))
    assetsDirectory.set(layout.buildDirectory.dir("generated/resources/assets"))
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
            api("com.reco1l.verktex:core")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

            // Response bodies read as @Serializable classes, see com.osudroid.online.httpClient.
            implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")

            // Files, streams and hashing.
            api("com.squareup.okio:okio:3.9.1")

            // Multiplatform locks and atomics.
            api("org.jetbrains.kotlinx:atomicfu:0.33.0")

            // Room's annotations, so the entities and DAOs can be declared in common code. The
            // database itself, which Room generates, is built by each platform that has one.
            api("androidx.room:room-common:2.7.2")
        }

        if (providers.gradleProperty("game.checkCommon").isPresent) {
            jsMain.dependencies {
                implementation("com.squareup.okio:okio-nodefilesystem:3.9.1")
            }
        }

        jvmTest.dependencies {
            implementation("junit:junit:4.13.2")

            // An HTTP engine for the tests that connect, see KtorRoomSocketTest.
            implementation("io.ktor:ktor-client-okhttp:3.1.3")

            // Canned responses for the HTTP client's tests.
            implementation("io.ktor:ktor-client-mock:3.1.3")
        }
    }
}

// KtorRoomSocketTest connects to the Socket.IO server at this URL, and is skipped without one:
// ./gradlew :game:jvmTest -PsocketIoTestUrl=http://localhost:3000
tasks.withType<Test>().configureEach {
    providers.gradleProperty("socketIoTestUrl").orNull?.let { systemProperty("socketIoTestUrl", it) }
}
