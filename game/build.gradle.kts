import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.w3c.dom.Element

// The game: gameplay, beatmaps, difficulty, mods, scoring and UI, with the targets of Verktex, see
// docs/multiplatform-game.md. commonMain is common Kotlin, which can't depend on Android or the JVM,
// and jvmMain is what still needs a JVM, shared by the Android and desktop targets. androidMain is
// the Android side of the game (MainActivity, the services, the database, the views), which the
// application module packages, and desktopMain the desktop launcher, which the run and runUITests
// tasks start.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.google.devtools.ksp") version "2.3.12"
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
    extraStrings.from(layout.projectDirectory.file("src/androidMain/res/values/strings_temporary_for_locals.xml"))
    outputDirectory.set(layout.buildDirectory.dir("generated/resources/commonMain/kotlin"))
    assetsDirectory.set(layout.buildDirectory.dir("generated/resources/assets"))
}

/**
 * Generates suspend copies of the game's DAOs, and adapters that implement the game's DAOs over them.
 *
 * Room only accepts suspend DAO functions outside Android, while the game calls its DAOs blocking. Each
 * `@Dao` interface of the game's data files is copied with its abstract functions made `suspend`, for
 * Room to implement, and its adapter blocks on them. The functions with a body come from the game's
 * interface, so the queries stay defined once.
 */
abstract class GenerateSuspendDaos : DefaultTask() {

    @get:InputFiles
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    private class Function(val name: String, val parameters: String, val returnType: String?)

    @TaskAction
    fun generate() {
        val interfaces = StringBuilder()
        val adapters = StringBuilder()

        sources.files.sortedBy { it.name }.forEach { file ->
            val text = file.readText()
            var start = text.indexOf("@Dao")

            while (start >= 0) {
                val name = Regex("""interface\s+(\w+)""").find(text, start)!!.groupValues[1]
                val open = text.indexOf('{', start)
                val body = text.substring(open + 1, matchingBrace(text, open))
                val suspendName = "Suspend" + name.removePrefix("I")

                val functions = mutableListOf<Function>()
                interfaces.appendLine("@Dao").appendLine("interface $suspendName {")

                val lines = body.lines()
                val annotations = mutableListOf<String>()
                var i = 0

                while (i < lines.size) {
                    val line = lines[i].trim()

                    when {
                        line.startsWith("@") -> annotations += line

                        line.startsWith("fun ") -> {
                            val signature = Regex("""fun\s+(\w+)\((.*)\)\s*(?::\s*([^={]+?))?\s*(=|\{)?\s*$""").find(line)!!

                            if (signature.groupValues[4].isEmpty()) {
                                val function = Function(signature.groupValues[1], signature.groupValues[2], signature.groupValues[3].ifBlank { null })
                                functions += function

                                annotations.forEach { interfaces.appendLine("    $it") }
                                interfaces.appendLine("    suspend $line").appendLine()
                            } else if (signature.groupValues[4] == "{") {
                                // Skip the body, the adapter inherits it.
                                var depth = 0
                                do {
                                    depth += lines[i].count { it == '{' } - lines[i].count { it == '}' }
                                    i++
                                } while (depth > 0 && i < lines.size)
                                i--
                            } else {
                                // An expression body, up to the next blank line.
                                while (i + 1 < lines.size && lines[i + 1].isNotBlank()) i++
                            }

                            annotations.clear()
                        }
                    }
                    i++
                }

                interfaces.appendLine("}").appendLine()

                adapters.appendLine("class ${suspendName}Adapter(private val dao: $suspendName) : $name {")
                functions.forEach { function ->
                    val arguments = splitParameters(function.parameters).joinToString(", ") { it.substringBefore(':').trim() }
                    val call = "runBlocking { dao.${function.name}($arguments) }"

                    if (function.returnType != null) {
                        adapters.appendLine("    override fun ${function.name}(${function.parameters}): ${function.returnType.trim()} = $call")
                    } else {
                        adapters.appendLine("    override fun ${function.name}(${function.parameters}) { $call }")
                    }
                }
                adapters.appendLine("}").appendLine()

                start = text.indexOf("@Dao", open)
            }
        }

        val output = outputDirectory.get().file("com/osudroid/desktop/data/SuspendDaos.kt").asFile
        output.parentFile.mkdirs()
        output.writeText(buildString {
            appendLine("// Generated from the game's DAOs by :game-desktop:generateSuspendDaos, don't edit.")
            appendLine("package com.osudroid.desktop.data")
            appendLine()
            appendLine("import androidx.room.*")
            appendLine("import com.osudroid.data.*")
            appendLine("import kotlinx.coroutines.runBlocking")
            appendLine()
            append(interfaces)
            append(adapters)
        })
    }

    private fun matchingBrace(text: String, open: Int): Int {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return i
            }
        }
        error("Unbalanced braces")
    }

    // Splits at the commas outside of type arguments.
    private fun splitParameters(parameters: String): List<String> {
        if (parameters.isBlank()) return emptyList()

        val result = mutableListOf<String>()
        var depth = 0
        var start = 0

        parameters.forEachIndexed { i, c ->
            when (c) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) {
                    result += parameters.substring(start, i)
                    start = i + 1
                }
            }
        }
        result += parameters.substring(start)
        return result
    }
}

val generateSuspendDaos by tasks.registering(GenerateSuspendDaos::class) {
    sources.from(fileTree(layout.projectDirectory.dir("src/commonMain/kotlin/com/osudroid/data")) {
        include("*.kt")
    })
    outputDirectory.set(layout.buildDirectory.dir("generated/daos"))
}

val roomVersion = "2.7.2"

kotlin {
    // The default hierarchy, with a source set between the common one and the JVM platforms.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvm") {
                withJvm()
                withCompilations { it.target.name == "android" }
            }
        }
    }

    android {
        // The namespace of the resources (R), the one the application had before the game moved
        // here. The application keeps its id.
        namespace = "ru.nsu.ccfit.zuev.osuplus"
        compileSdk {
            version = release(36) { minorApiLevel = 1 }
        }
        minSdk = 24

        // MainActivity and other legacy classes are Java.
        withJava()

        androidResources {
            enable = true
        }

        lint {
            abortOnError = true
            disable += listOf("MissingTranslation", "ExpiredTargetSdkVersion", "ProtectedPermissions", "AppLinkUrlError")
            ignoreWarnings = true
        }

        withHostTest {
            isIncludeAndroidResources = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-jvm-default=no-compatibility")
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)

            // The launcher is a host of the engine.
            optIn.add("com.reco1l.verktex.VerktexPlatformApi")
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
            api("com.reco1l.verktex:verktex")
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

        androidMain.dependencies {
            implementation("androidx.legacy:legacy-support-v4:1.0.0")
            implementation("com.google.android.material:material:1.11.0")
            implementation("com.google.firebase:firebase-crashlytics:19.4.0")
            implementation("com.google.firebase:firebase-analytics:22.2.0")
            implementation("com.google.firebase:firebase-messaging:24.1.0")

            implementation("com.google.code.gson:gson:2.12.1")
            implementation("pub.devrel:easypermissions:3.0.0")
            implementation("net.lingala.zip4j:zip4j:2.11.5")

            // Only its easings, used by the Android views until they're rewritten on Verktex.
            implementation("com.github.EdrowsLuo.EdlGameFramework:EdlJavaExt:d91c53fcff")

            // Localizations
            implementation("com.github.osudroid:language-pack:${providers.gradleProperty("languagePackVersion").get()}")

            implementation("commons-io:commons-io:2.18.0")
            implementation(kotlin("reflect"))

            // The database: Room over Android's SQLite.
            implementation("androidx.room:room-runtime:$roomVersion")
            implementation("androidx.room:room-ktx:$roomVersion")
        }

        getByName("androidHostTest").dependencies {
            implementation("junit:junit:4.13.2")
            implementation("org.robolectric:robolectric:4.14.1")
            implementation("androidx.room:room-testing:$roomVersion")
        }

        getByName("desktopMain") {
            // The DAOs Room implements on desktop, see GenerateSuspendDaos.
            kotlin.srcDir(generateSuspendDaos)

            // The game's assets (the default skin, sounds, fonts) and its strings, which Android
            // packages as its assets, load from the classpath.
            resources.srcDir(generateStringResources.map { it.assetsDirectory })
            resources.srcDir(rootProject.layout.projectDirectory.dir("assets"))

            dependencies {
                // The database: Room on the JVM, over the bundled SQLite.
                implementation("androidx.room:room-runtime:$roomVersion")
                implementation("androidx.sqlite:sqlite-bundled:2.5.1")
            }
        }

        getByName("desktopTest").dependencies {
            implementation("junit:junit:4.13.2")

            // An HTTP engine for the tests that connect, see KtorRoomSocketTest.
            implementation("io.ktor:ktor-client-okhttp:3.1.3")

            // Canned responses for the HTTP client's tests.
            implementation("io.ktor:ktor-client-mock:3.1.3")
        }
    }
}

// KtorRoomSocketTest connects to the Socket.IO server at this URL, and is skipped without one:
// ./gradlew :game:desktopTest -PsocketIoTestUrl=http://localhost:3000
tasks.withType<Test>().configureEach {
    providers.gradleProperty("socketIoTestUrl").orNull?.let { systemProperty("socketIoTestUrl", it) }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:$roomVersion")
    add("kspDesktop", "androidx.room:room-compiler:$roomVersion")
}

ksp {
    arg("room.generateKotlin", "true")

    // The schemas of the Android database, which its migrations are tested against. The desktop
    // one exports none.
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
}

androidComponents {
    onVariants { variant ->
        // The game's assets (the default skin, sounds, fonts) and its strings.
        variant.sources.assets?.addStaticSourceDirectory(rootProject.layout.projectDirectory.dir("assets").asFile.path)
        variant.sources.assets?.addGeneratedSourceDirectory(generateStringResources, GenerateStringResources::assetsDirectory)

        // The Room schemas aren't added as a test resource but as an asset, see
        // https://github.com/robolectric/robolectric/issues/3928#issuecomment-395309991
        variant.sources.assets?.addStaticSourceDirectory(layout.projectDirectory.dir("schemas").asFile.path)
    }
}

// The desktop launcher. Relative paths given in --args, e.g. --screenshot, resolve against the
// root of the project.
fun JavaExec.desktopLauncher(main: String) {
    group = "application"

    val compilation = kotlin.targets.getByName("desktop").compilations.getByName("main") as KotlinJvmCompilation
    classpath = files(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
    mainClass.set(main)

    // GLFW has to run on the first thread on macOS.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }

    workingDir = rootProject.projectDir

    // FFmpeg's libraries for videos, downloaded by Verktex's desktop platform.
    dependsOn(gradle.includedBuild("verktex").task(":verktex:prepareFFmpeg"))
    systemProperty("verktex.ffmpeg.path", rootProject.layout.projectDirectory.dir("verktex/verktex/build/ffmpeg").asFile.absolutePath)
}

tasks.register<JavaExec>("run") {
    description = "Runs osu!droid on desktop."
    desktopLauncher("com.osudroid.desktop.OsuDesktopKt")
}

// With Verktex's suites too.
tasks.register<JavaExec>("runUITests") {
    description = "Runs the UI test browser of osu!droid on desktop."
    desktopLauncher("com.osudroid.desktop.OsuUITestsDesktopKt")
}
