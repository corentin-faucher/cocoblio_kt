package com.coq.cocoblio.maths

import com.coq.cocoblio.SmallChrono
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.PI
import kotlin.math.cos

/** Smooth Transition est un flag "On/Off" analogique (transition smooth entre 0 et 1). */
class SmTrans {
    var transTime: Short
    val isActive: Boolean
        get() = (transEnum != TransEnum.IsDown)
    fun setAndGet(isOn: Boolean) : Float {
        // 1. Update du state.
        set(isOn)
        return privateGet()
    }
    fun get() : Float {
        // Doit vérifier si la transition est terminée.
        when (transEnum) {
            TransEnum.GoingDown -> {
                if (chrono.elapsedMS16 > transTime)
                    transEnum = TransEnum.IsDown
            }
            TransEnum.GoingUp -> {
                if (chrono.elapsedMS16 > transTime)
                    transEnum = TransEnum.IsUp
            }
            else -> {}
        }
        return privateGet()
    }
    fun set(isOn: Boolean) {
        transEnum = if (isOn) {
            when(transEnum) {
                TransEnum.IsDown -> {
                    if(isHard) {
                        TransEnum.IsUp
                    } else {
                        chrono.start()
                        TransEnum.GoingUp
                    }
                }
                TransEnum.GoingUp -> {
                    if (chrono.elapsedMS16 > transTime)
                        TransEnum.IsUp
                    else
                        TransEnum.GoingUp
                }
                TransEnum.GoingDown -> {
                    val time = chrono.elapsedMS16
                    if (time < transTime) {
                        chrono.setElapsedTo(transTime - time)
                    } else {
                        chrono.start()
                    }
                    TransEnum.GoingUp
                }
                TransEnum.IsUp -> TransEnum.IsUp
            }
        } else {
            when (transEnum) {
                TransEnum.IsUp -> {
                    if (isHard) {
                        TransEnum.IsDown
                    } else {
                        chrono.start()
                        TransEnum.GoingDown
                    }
                }
                TransEnum.GoingDown -> {
                    if (chrono.elapsedMS16 > transTime)
                        TransEnum.IsDown
                    else
                        TransEnum.GoingDown
                }
                TransEnum.GoingUp -> {
                    val time = chrono.elapsedMS16
                    if (time < transTime ){
                        chrono.setElapsedTo(transTime - time)
                    } else {
                        chrono.start()
                    }
                    TransEnum.GoingDown
                }
                TransEnum.IsDown -> TransEnum.IsDown
            }
        }
    }
    fun hardSet(isOn: Boolean) {
        transEnum = if(isOn) TransEnum.IsUp else TransEnum.IsDown
    }
    fun setOptions(isHard: Boolean, isPoping: Boolean) {
        extraState = (if(isHard) hard else 0) or (if(isPoping) poping else 0) //or (if(isSemi) semi else 0)
    }
    constructor() {
        transTime = defaultTransTime
        chrono = SmallChrono()
        transEnum = TransEnum.IsDown
        extraState = 0
    }
    constructor(smTransRef: SmTrans) {
        transTime = smTransRef.transTime
        chrono = SmallChrono()
        transEnum = TransEnum.IsDown
        extraState = smTransRef.extraState
    }

    private var chrono: SmallChrono
    private var transEnum: TransEnum
    private var extraState: Byte

    private fun privateGet() : Float {
        fun pipPop() : Float {
            val ratio: Float = chrono.elapsedMS16.toFloat() / transTime.toFloat()
            return  a + b * cos(PI.toFloat() * ratio) +
                    (0.5f - a) * cos(2f * PI.toFloat() * ratio) +
                    (-0.5f - b) * cos(3f * PI.toFloat() * ratio)
        }
        fun smooth() : Float {
            val ratio: Float = chrono.elapsedMS16.toFloat() / transTime.toFloat()
            return (1f - cos(PI.toFloat() * ratio)) / 2f
        }
        fun smoothDown() : Float {
            val ratio: Float = chrono.elapsedMS16.toFloat() / transTime.toFloat()
            return (1f + cos(PI.toFloat() * ratio)) / 2f
        }

        return when(transEnum) {
            TransEnum.IsDown -> 0f
            TransEnum.IsUp -> 1f // if(isSemi) semiFact else 1f
            TransEnum.GoingUp -> (if(isPoping) pipPop() else smooth()) //(if(isSemi) semiFact else 1f) *

            TransEnum.GoingDown ->  smoothDown() // (if (isSemi) semiFact else 1f) *
        }
    }
    private val isHard: Boolean
        get() = ((extraState and hard) == hard)
    private val isPoping: Boolean
        get() = ((extraState and poping) == poping)

    private enum class TransEnum {IsDown, IsUp, GoingUp, GoingDown}

    companion object {
        var globalPopFactor = 0.2f
            set(value) {
                a = 0.75f + globalPopFactor * 0.2f
                b = -0.43f + globalPopFactor * 0.43f
                field = value
            }
        var defaultTransTime: Short = 500

        private const val poping: Byte = 1
        private const val hard: Byte = 2

        private var a: Float = 0.75f + globalPopFactor * 0.2f
        private var b: Float = -0.43f + globalPopFactor * 0.43f
    }
}
