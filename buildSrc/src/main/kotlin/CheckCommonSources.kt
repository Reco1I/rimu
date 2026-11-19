import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when common code uses the JVM. The modules' targets are all JVMs, so the compiler lets
 * commonMain see the JDK; this catches what it doesn't: Java imports and the stdlib members only the
 * JVM has. Comments are skipped, and so are what strings hold.
 */
abstract class CheckCommonSources : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    private val forbidden = mapOf(
        Regex("""^\s*import (java|javax)\.""") to "a Java import",
        Regex("""\bjava\.(lang|util|io|text|net|nio|math|time)\.""") to "a Java class",
        Regex("""\bString\.format\b|\.format\(Locale|"\.format\(""") to "String.format, use formatString",
        Regex("""\bSystem\.(currentTimeMillis|nanoTime|getProperty|getenv|exit|arraycopy)\b""") to "System, use kotlin.time or Platform",
        Regex("""\.javaClass\b|::class\.java\b""") to "a Java class reference, use ::class",
        Regex("""\bDispatchers\.IO\b""") to "Dispatchers.IO, which isn't common",
        Regex("""(?<![.\w])Math\.[a-zA-Z]""") to "java.lang.Math, use kotlin.math",
        Regex("""\brunBlocking\b""") to "runBlocking, which isn't common",
        Regex("""\bkotlin\.jvm\.(Volatile|Synchronized|Throws|Strictfp|Transient)\b|@Synchronized\b""") to "a JVM-only annotation",
        Regex("""\bkotlinx\.coroutines\.future\b""") to "CompletableFuture interop",
    )

    // A string on one line, with its escapes.
    private val stringLiteral = Regex(""""(?:[^"\\]|\\.)*"""")

    @TaskAction
    fun check() {
        val problems = mutableListOf<String>()

        sources.asFileTree.matching { include("**/*.kt") }.forEach { file ->
            var inBlockComment = false
            file.readLines().forEachIndexed { index, raw ->
                var line = raw
                if (inBlockComment) {
                    val end = line.indexOf("*/")
                    if (end < 0) return@forEachIndexed
                    line = line.substring(end + 2)
                    inBlockComment = false
                }
                // What strings hold is data (such as the names of Java classes a format reads), not
                // code: they're emptied, which keeps "".format( visible.
                line = line.replace(stringLiteral, "\"\"")
                val blockStart = line.indexOf("/*")
                if (blockStart >= 0) {
                    inBlockComment = line.indexOf("*/", blockStart + 2) < 0
                    line = line.substring(0, blockStart)
                }
                line = line.substringBefore("//")

                for ((pattern, reason) in forbidden) {
                    if (pattern.containsMatchIn(line)) {
                        problems += "${file.path}:${index + 1}: $reason: ${raw.trim()}"
                    }
                }
            }
        }

        report.get().asFile.writeText(problems.joinToString("\n"))
        if (problems.isNotEmpty()) {
            throw GradleException("commonMain uses the JVM:\n" + problems.joinToString("\n"))
        }
    }
}
