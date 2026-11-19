package com.reco1l.javaserialization

/**
 * The constants of Java's object serialization stream protocol, as `java.io.ObjectStreamConstants`
 * defines them (https://docs.oracle.com/javase/8/docs/platform/serialization/spec/protocol.html).
 */
internal object StreamConstants {
    const val STREAM_MAGIC: Short = 0xACED.toShort()
    const val STREAM_VERSION: Short = 5

    const val TC_NULL: Byte = 0x70
    const val TC_REFERENCE: Byte = 0x71
    const val TC_CLASSDESC: Byte = 0x72
    const val TC_OBJECT: Byte = 0x73
    const val TC_STRING: Byte = 0x74
    const val TC_ARRAY: Byte = 0x75
    const val TC_CLASS: Byte = 0x76
    const val TC_BLOCKDATA: Byte = 0x77
    const val TC_ENDBLOCKDATA: Byte = 0x78
    const val TC_RESET: Byte = 0x79
    const val TC_BLOCKDATALONG: Byte = 0x7A
    const val TC_EXCEPTION: Byte = 0x7B
    const val TC_LONGSTRING: Byte = 0x7C
    const val TC_PROXYCLASSDESC: Byte = 0x7D
    const val TC_ENUM: Byte = 0x7E

    /**
     * The handle the first object of a stream gets, each new one taking the next.
     */
    const val BASE_WIRE_HANDLE = 0x7E0000

    const val SC_WRITE_METHOD = 0x01
    const val SC_BLOCK_DATA = 0x08
    const val SC_SERIALIZABLE = 0x02
    const val SC_EXTERNALIZABLE = 0x04
    const val SC_ENUM = 0x10
}
