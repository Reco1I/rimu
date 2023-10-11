package com.reco1l.framework.data

import android.content.ContentResolver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import android.provider.OpenableColumns.DISPLAY_NAME
import androidx.core.net.toFile
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.InputStream
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


fun Uri.toFile(parent: File, resolver: ContentResolver): File
{
    if (scheme == SCHEME_FILE)
        return toFile()

    return File(parent, resolveFilename(resolver)).apply {

        writeFromContentUri(this@toFile, resolver)
    }
}

/**
 * Write a file from an URI with [content][SCHEME_CONTENT] scheme.
 *
 * Because of Android's safe storage we can't access to these files directly, we have to read their
 * contents from an [InputStream]. This method internally copies the content given by the URI input
 * stream to this file.
 *
 * If a [File] object is not necessary consider using [InputStream] instead. Also consider using [Uri.toFile]
 * instead if you sure that the URI scheme is [SCHEME_FILE].
 */
fun File.writeFromContentUri(uri: Uri, resolver: ContentResolver)
{
    if (uri.scheme != SCHEME_CONTENT)
        throw IllegalArgumentException("The URI scheme does not equal \"content\".")

    delete()
    createNewFile()

    resolver.openInputStream(uri)?.use { outputStream().use { out -> it.copyTo(out) } }
}

fun Uri.resolveFilename(resolver: ContentResolver): String
{
    if (scheme != SCHEME_CONTENT)
        throw IllegalArgumentException("The URI scheme does not equal \"content\".")

    return resolver.query(this, null, null, null, null)!!.use {

        it.moveToFirst()
        it.getString(it.getColumnIndexOrThrow(DISPLAY_NAME))
    }
}
