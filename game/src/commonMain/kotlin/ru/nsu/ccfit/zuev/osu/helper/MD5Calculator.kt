package ru.nsu.ccfit.zuev.osu.helper

import com.osudroid.utils.md5Checksum
import kotlin.jvm.JvmStatic
import okio.ByteString.Companion.encodeUtf8

object MD5Calculator {
    @JvmStatic
    fun getFileMD5(path: String): String {
        return md5Checksum(path)
    }

    @JvmStatic
    fun getStringMD5(str: String): String {
        return str.encodeUtf8().md5().hex()
    }
}
