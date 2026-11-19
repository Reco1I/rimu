package com.reco1l.javaserialization

/**
 * Decodes [bytes] from the modified UTF-8 Java writes strings in (`DataOutput.writeUTF`): as UTF-8,
 * except that the NUL character is the two bytes `C0 80` and characters outside the Basic
 * Multilingual Plane are their two UTF-16 surrogates, each encoded on three bytes.
 *
 * @throws JavaSerializationException If [bytes] aren't valid modified UTF-8.
 */
internal fun decodeModifiedUtf8(bytes: ByteArray): String {
    val chars = CharArray(bytes.size)
    var count = 0
    var i = 0

    fun continuation(index: Int): Int {
        if (index >= bytes.size) {
            throw JavaSerializationException("Malformed modified UTF-8: truncated character at byte $index")
        }

        val byte = bytes[index].toInt() and 0xFF
        if (byte and 0xC0 != 0x80) {
            throw JavaSerializationException("Malformed modified UTF-8: invalid continuation byte at $index")
        }

        return byte and 0x3F
    }

    while (i < bytes.size) {
        val byte = bytes[i].toInt() and 0xFF

        when (byte shr 4) {
            in 0..7 -> {
                chars[count++] = byte.toChar()
                i++
            }

            12, 13 -> {
                chars[count++] = (((byte and 0x1F) shl 6) or continuation(i + 1)).toChar()
                i += 2
            }

            14 -> {
                chars[count++] = (((byte and 0x0F) shl 12) or (continuation(i + 1) shl 6) or continuation(i + 2)).toChar()
                i += 3
            }

            else -> throw JavaSerializationException("Malformed modified UTF-8: invalid byte at $i")
        }
    }

    return chars.concatToString(0, count)
}
