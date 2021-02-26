@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.maths

import com.coq.cocoblio.divers.GlobalChrono
import com.coq.cocoblio.divers.printerror

/** Angle "moyen" interpollé en fonction des dernières valeurs entrées.
 * (Pour un roulement smooth.) */
class InterpolatedAngle(size: Int, posRef: Float) {

    fun push(newPos: Float) {
        val time = GlobalChrono.elapsedMS
        if (time == vT[lastIndex])
            return

        vX[currIndex] = newPos.toNormalizedAngle()
        vT[currIndex] = time

        for (i in vX.indices) {
            vXr[i] = (vX[i] - vX[currIndex]).toNormalizedAngle()
            vTr[i] = - (vT[currIndex] - vT[i]).toFloat() / 1000f
        }

        val n = vX.size.toFloat()
        var sumPrTX = 0f
        var sumT = 0f
        var sumX = 0f
        var sumT2 = 0f
        for (i in vX.indices) {
            sumPrTX += vXr[i] * vTr[i]
            sumT += vTr[i]
            sumX += vXr[i]
            sumT2 += vTr[i] * vTr[i]
        }
        val det = n * sumT2 - sumT * sumT
        if (det == 0f || sumT == 0f) {
            printerror(" Erreur interpolation..."); return
        }

        // Interpolation
        vit = (n * sumPrTX - sumT * sumX) / det
        pos = (sumPrTX - vit * sumT2) / sumT + vX[currIndex]

        lastIndex = currIndex
        currIndex = (currIndex + 1) % vX.size
    }

    var pos : Float = 0f
        private set
    var vit : Float = 0f
        private set

    private var lastIndex = 0
    private var currIndex = 0
    private val vX: FloatArray = FloatArray(size) {posRef}
    private val vXr: FloatArray = FloatArray(size) {posRef}
    private val vT: LongArray = LongArray(size) {0L}
    private val vTr: FloatArray = FloatArray(size) {0f}
}