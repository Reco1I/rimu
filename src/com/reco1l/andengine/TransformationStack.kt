package com.reco1l.andengine

import org.anddev.andengine.util.Transformation
import java.util.Stack

private val outputArray = FloatArray(2)

operator fun Transformation.invoke(x: Float, y: Float): FloatArray {
    outputArray[0] = x
    outputArray[1] = y
    transform(outputArray)
    return outputArray
}

object TransformationStack : Stack<Transformation>() {

    private fun readResolve(): Any = TransformationStack

}
