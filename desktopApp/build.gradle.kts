import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The desktop application: the entry points of the game on desktop (OsuDesktop, the UI test browser)
// and its database, over the desktop target of :shared and of Verktex. See docs/multiplatform-game.md.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp") version "2.3.12"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        optIn.add("com.reco1l.verktex.VerktexPlatformApi")
    }
}

val roomVersion = "2.7.2"

dependencies {
    implementation(project(":shared"))
    implementation("com.reco1l.verktex:verktex")

    // The database: Room on the JVM, over the bundled SQLite.
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.sqlite:sqlite-bundled:2.5.1")
    ksp("androidx.room:room-compiler:$roomVersion")

}

ksp {
    arg("room.generateKotlin", "true")
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
            appendLine("// Generated from the game's DAOs by :desktopApp:generateSuspendDaos, don't edit.")
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
    sources.from(fileTree(rootProject.layout.projectDirectory.dir("shared/src/commonMain/kotlin/com/osudroid/data")) {
        include("*.kt")
    })
    outputDirectory.set(layout.buildDirectory.dir("generated/daos"))
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateSuspendDaos)
}

// The game's assets (the default skin, sounds, fonts) and its strings, generated from the language
// pack, which Android packages as its assets, load from the classpath.
sourceSets.main {
    resources.srcDir(files(project(":shared").layout.buildDirectory.dir("generated/resources/assets")).builtBy(":shared:generateStringResources"))
    resources.srcDir(rootProject.layout.projectDirectory.dir("shared/assets"))
}

application {
    mainClass.set("com.osudroid.desktop.OsuDesktopKt")

    // GLFW has to run on the first thread on macOS.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

// FFmpeg's libraries for videos, downloaded by Verktex's desktop platform.
val ffmpegDirectory = rootProject.layout.projectDirectory.dir("verktex/verktex/build/ffmpeg").asFile.absolutePath

tasks.named<JavaExec>("run") {
    // Relative paths given in --args, e.g. --screenshot, resolve against where Gradle was started.
    workingDir = rootProject.projectDir

    dependsOn(gradle.includedBuild("verktex").task(":verktex:prepareFFmpeg"))
    systemProperty("verktex.ffmpeg.path", ffmpegDirectory)
}

// The UI test browser of osu!droid, with Verktex's suites too.
tasks.register<JavaExec>("runUITests") {
    group = "application"
    description = "Runs the UI test browser of osu!droid on desktop."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.osudroid.desktop.OsuUITestsDesktopKt")
    jvmArgs = application.applicationDefaultJvmArgs.toList()
    workingDir = rootProject.projectDir

    dependsOn(gradle.includedBuild("verktex").task(":verktex:prepareFFmpeg"))
    systemProperty("verktex.ffmpeg.path", ffmpegDirectory)
}
