package com.reco1l.verktex.desktop

import com.ditchoom.buffer.BaseJvmBuffer
import com.ditchoom.buffer.DirectJvmBuffer
import com.ditchoom.buffer.HeapJvmBuffer
import com.ditchoom.buffer.PlatformBuffer
import com.ditchoom.buffer.ReadBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

// The same as the Android platform's: the core's jvm variant and the android one of the buffer
// library differ at runtime, so these aren't shared through the core.

/**
 * The buffer as one of the engine's, without copying: both share the data, the position and the limit.
 */
fun ByteBuffer.asReadBuffer(): PlatformBuffer = if (isDirect) DirectJvmBuffer(this) else HeapJvmBuffer(this)

/**
 * The [ByteBuffer] behind the buffer, sharing its position and limit. Buffers over a [ByteBuffer],
 * like the ones of [asReadBuffer], aren't copied, others are copied to a direct buffer holding the
 * bytes between their position and limit.
 */
fun ReadBuffer.asByteBuffer(): ByteBuffer {
    (this as? BaseJvmBuffer)?.let { return it.byteBuffer }

    val start = position()
    val copy = ByteBuffer.allocateDirect(limit() - start).order(ByteOrder.nativeOrder())
    for (i in start until limit()) {
        copy.put(get(i))
    }
    copy.flip()
    return copy
}
