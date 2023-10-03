/*
 * @author Reco1l
 */

package com.reco1l.framework.data.files

class InvalidExtensionException(private val extension: String): Exception()
{
    override val message = "Invalid file extension: $extension"
}