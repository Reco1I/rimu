package com.reco1l.verktex.desktop

import com.reco1l.verktex.Logger
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A [FontFamily.Source] over a font installed on the system.
 *
 * stb_truetype can't synthesise weights or italics, so every weight and style is looked up as a file
 * of its own: through fontconfig on Linux, and among the files of the system's UI font on Windows
 * and macOS. A weight or style without a file uses the regular one.
 */
class DesktopSystemFontSource private constructor(
    private val family: String?,
    private val regular: DesktopFontSource,
    private val regularFile: File,
) : FontFamily.Source {

    // By file, weights resolving to the same file share it.
    private val sources = mutableMapOf(regularFile to regular)

    private val files = mutableMapOf<Pair<Font.Weight, Boolean>, File?>()

    private var warnedFallback = false


    override fun onCreateFont(descriptor: Font.Descriptor): Font {
        val isItalic = descriptor.style == Font.Style.Italic

        val file = files.getOrPut(descriptor.weight to isItalic) { find(family, descriptor.weight, isItalic) }
        val source = file?.let { sources.getOrPut(it) { load(it) ?: regular } } ?: regular

        if (source === regular && (descriptor.weight != Font.Weight.Normal || isItalic) && !warnedFallback) {
            warnedFallback = true
            Logger.w(TAG, "No system font file for ${descriptor.weight}${if (isItalic) " italic" else ""}, the regular one is used.")
        }

        return source.onCreateFont(descriptor)
    }


    companion object {

        private const val TAG = "DesktopSystemFontSource"

        /**
         * Creates the source of the system font [family], or the default UI font when null.
         *
         * @return The source, or null when the regular font wasn't found.
         */
        fun create(family: String?): DesktopSystemFontSource? {
            val file = find(family, Font.Weight.Normal, false) ?: return null
            val source = load(file) ?: return null
            Logger.i(TAG, "System font ${family ?: "default"} is ${file.path}")
            return DesktopSystemFontSource(family, source, file)
        }


        private fun load(file: File): DesktopFontSource? = try {
            DesktopFontSource(file.readBytes(), warnsSynthesis = false)
        } catch (e: Exception) {
            Logger.e(TAG, "Couldn't load the font ${file.path}", e)
            null
        }

        private fun find(family: String?, weight: Font.Weight, isItalic: Boolean): File? {
            val os = System.getProperty("os.name").orEmpty().lowercase()
            val file = when {
                os.contains("win") -> findWindows(family, weight, isItalic)
                os.contains("mac") -> findMac(family)
                else -> findFontconfig(family, weight, isItalic)
            }
            return file?.takeIf { it.isFile }
        }

        private fun findFontconfig(family: String?, weight: Font.Weight, isItalic: Boolean): File? {
            val fcWeight = when (weight) {
                Font.Weight.Thin -> 0
                Font.Weight.ExtraLight -> 40
                Font.Weight.Light -> 50
                Font.Weight.Normal -> 80
                Font.Weight.Medium -> 100
                Font.Weight.SemiBold -> 180
                Font.Weight.Bold -> 200
                Font.Weight.ExtraBold -> 205
                Font.Weight.Black -> 210
            }
            val pattern = "${family ?: "sans-serif"}:weight=$fcWeight:slant=${if (isItalic) 100 else 0}"

            return try {
                val process = ProcessBuilder("fc-match", "-f", "%{file}", pattern)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText().trim()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroy()
                    return null
                }
                if (process.exitValue() != 0 || output.isEmpty()) null else File(output)
            } catch (e: Exception) {
                Logger.w(TAG, "fontconfig isn't available: ${e.message}")
                null
            }
        }

        private fun findWindows(family: String?, weight: Font.Weight, isItalic: Boolean): File? {
            val fonts = File(System.getenv("WINDIR") ?: "C:\\Windows", "Fonts")

            if (family != null) {
                // Without fontconfig only the regular file of a family can be guessed from its name.
                return File(fonts, family.replace(" ", "").lowercase() + ".ttf")
            }

            val name = when (weight) {
                Font.Weight.Thin, Font.Weight.ExtraLight, Font.Weight.Light -> if (isItalic) "seguili" else "segoeuil"
                Font.Weight.Normal, Font.Weight.Medium -> if (isItalic) "segoeuii" else "segoeui"
                Font.Weight.SemiBold -> if (isItalic) "seguisbi" else "seguisb"
                Font.Weight.Bold -> if (isItalic) "segoeuiz" else "segoeuib"
                Font.Weight.ExtraBold, Font.Weight.Black -> if (isItalic) "seguibli" else "seguibl"
            }
            return File(fonts, "$name.ttf")
        }

        private fun findMac(family: String?): File? {
            val candidates = buildList {
                if (family != null) {
                    add("/System/Library/Fonts/$family.ttf")
                    add("/System/Library/Fonts/$family.ttc")
                    add("/Library/Fonts/$family.ttf")
                }
                // Only the regular instance of each, weights use it.
                add("/System/Library/Fonts/SFNS.ttf")
                add("/System/Library/Fonts/Helvetica.ttc")
            }
            return candidates.map(::File).firstOrNull { it.isFile }
        }
    }
}
