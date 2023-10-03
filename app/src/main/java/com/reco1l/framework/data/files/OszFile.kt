/*
 * @author Reco1l
 */

package com.reco1l.framework.data.files

import com.reco1l.framework.extensions.isExtension
import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * osu! zip file wrapper.
 *
 * @throws InvalidZipException if it's not a valid zip file specified by [isValidZipFile].
 * @throws InvalidExtensionException if the file is not an OSZ file.
 */
class OszFile(file: File) : ZipFile(file)
{

    init
    {
        if (!file.isExtension(EXTENSION))
            throw InvalidExtensionException(file.extension)

        if (!isValidZipFile)
            throw InvalidZipException()
    }

    companion object
    {
        /**
         * The file extension without the dot.
         */
        const val EXTENSION = "osz"
    }
}