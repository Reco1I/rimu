package com.reco1l.andengine

import org.anddev.andengine.util.Transformation
import java.util.Stack

private val outputArray = FloatArray(2)

fun Transformation?.transform(x: Float, y: Float): FloatArray {
    outputArray[0] = x
    outputArray[1] = y
    this?.transform(outputArray)
    return outputArray
}

object TransformationStack {

    val deque = ArrayDeque<Transformation>()


    fun push(transformation: Transformation) {
        deque.addLast(transformation)
    }

    fun pop() {
        deque.removeLastOrNull()
    }

    fun peek(): Transformation? {
        return deque.lastOrNull()
    }


    private fun readResolve(): Any = TransformationStack

}
