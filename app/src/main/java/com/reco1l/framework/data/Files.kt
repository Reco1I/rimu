package com.reco1l.framework.data

import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.IOException


/**
 * Returns the file extension but lowercase, specially used for comparisons.
 */
val File.extensionLowercase
    get() = extension.lowercase()

/**
 * Get the file MD5.
 *
 * It internally creates an InputStream in order to generate the MD5, don't use it often.
 */
val File.md5: String
    get() = inputStream().use { return DigestUtils.md5Hex(it) }

/**
 * Check if a File extensions equals to the specified. This ignores case.
 */
fun File.isExtension(extension: String) = isFile && extension.equals(extension, true)

/**
 * Create a new sub directory.
 */
fun File.subDirectory(name: String, mkdirs: Boolean = false): File
{
    if (!isDirectory)
        throw IOException("This isn't a directory.")

    return File(this, name).apply {

        if (mkdirs && !exists())
            mkdirs()
    }
}

/**
 * Create a new sub file.
 */
fun File.subFile(name: String, create: Boolean = true): File
{
    if (!isDirectory)
        throw IOException("This isn't a directory.")

    if (create)
        mkdirs()

    return File(this, name).apply {

        if (create && !exists())
            createNewFile()
    }
}

/**
 * Similarly to [File.listFiles].
 */
fun File.getFiles(vararg extensions: String? = emptyArray()): Array<File>?
{
    return listFiles { file ->
        extensions.isEmpty() || extensions.any { it.equals(file.extension, true) }
    }
}

/**
 * Opposite to [getFiles] where only sub-folders are listed.
 */
fun File.getFolders(namePredicate: ((String) -> Boolean)? = null): Array<out File>?
{
    return listFiles { file -> file.isDirectory && (namePredicate == null || namePredicate(file.name)) }
}

/**
 * Iterate all over the files inside the directory.
 */
fun File.forEach(
    vararg extensions: String? = emptyArray(),
    selector: ((File) -> Int)? = null,
    block: (File) -> Unit
)
{
    getFiles(*extensions)?.also {
        if (selector != null)
            it.sortedBy(selector).forEach(block)
        else
            it.forEach(block)
    }
}

/**
 * Iterate all over files and sub-directories.
 */
fun File.forEachRecursive(
    vararg extensions: String? = emptyArray(),
    selector: ((File) -> Int)? = null,
    block: (File) -> Unit
)
{
    val action = { file: File ->

        if (file.isDirectory)
            file.forEachRecursive(*extensions, selector = selector, block = block)
        else
            block(file)
    }

    getFiles(*extensions)?.also {
        if (selector != null)
            it.sortedBy(selector).forEach(action)
        else
            it.forEach(action)
    }
}

/**
 * Find a file by its name.
 */
operator fun Array<out File>.get(name: String, ignoreCase: Boolean = true) = find {
    it.name.equals(name, ignoreCase)
}

/**
 * Find a file by its name.
 */
operator fun List<File>.get(name: String, ignoreCase: Boolean = true) = find {
    it.name.equals(name, ignoreCase)
}