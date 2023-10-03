package com.reco1l.framework.data

import java.io.Closeable
import java.io.File

/**
 * Wrap a file into a [Closeable] form to be used in a `try-with-resources` or `use {}` statement.
 */
class TemporalFile(parentPath: String, path: String) : File(parentPath, path), Closeable
{
    override fun close()
    {
        if (exists())
            delete()
    }
}