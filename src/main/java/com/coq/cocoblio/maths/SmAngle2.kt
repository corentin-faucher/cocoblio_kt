@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio.maths

import com.coq.cocoblio.GlobalChrono
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** Angle "smooth" (évolue doucement dans le temps).
 * SmAngle2 tient compte de la vitesse estimée lors du set.
 * l'angle est entre -pi et pi (voir toNormalizedAngle). */
class SmAngle2 : Cloneable {
    /** Vrai position (dernière entrée). Le setter FIXE la position. */
    var realPos: Float
        get() = lastPos
        set(newPos) {
            lastPos = newPos.toNormalizedAngle()
            lastVit = 0f
            a = 0.0f; b = 0.0f
        }

    /** Position estimée au temps présent. Setter met à jour la "real" pos. et crée une nouvelle estimation. */
    val pos: Float
        get() {
            val deltaT = elapsedSec
            return when(type) {
                SmPosType.OscAmorti ->
                    exp(-lambda * deltaT) * (a * cos(beta * deltaT) + b * sin(beta * deltaT)) + lastPos + lastVit * deltaT
                SmPosType.AmortiCrit ->
                    (a + b * deltaT) * exp(-lambda * deltaT) + lastPos + lastVit * deltaT
                SmPosType.SurAmorti ->
                    a * exp(-lambda * deltaT) + b * exp(-beta * deltaT) + lastPos + lastVit * deltaT
                SmPosType.Static -> lastPos + lastVit * deltaT
            }
        }

    fun setPos(newPos: Float, newVit: Float) {
        evalAB((pos - newPos).toNormalizedAngle(), vit - newVit)
        lastPos = newPos.toNormalizedAngle()
        lastVit = newVit
        setTime = GlobalChrono.elapsedMS32
    }

    /** Vitesse estimée au temps présent. */
    val vit: Float
        get() {
            val deltaT = elapsedSec
            return when(type) {
                SmPosType.OscAmorti ->
                    exp(-lambda * deltaT) * ( cos(beta * deltaT) * (beta*b - lambda*a)
                            - sin(beta * deltaT) * (lambda*b + beta*a) ) + lastVit
                SmPosType.AmortiCrit ->
                    exp(-lambda * deltaT) * (b*(1 - lambda*deltaT) - lambda*a) + lastVit
                SmPosType.SurAmorti ->
                    -lambda * a * exp(-lambda * deltaT) - beta * b * exp(-beta * deltaT) + lastVit
                SmPosType.Static ->
                    lastVit
            }
        }


    fun updateLambda(lambda: Float) {
        updateConstants(2.0f * lambda, lambda * lambda)
    }
    fun updateConstants(gamma: Float, k: Float) {
        // 1. Enregistrer vit et deltaX avant de changer les constantes
        val deltaVit = vit - lastVit
        val deltaX = (pos - realPos).toNormalizedAngle()
        // 2. Changer les constantes lambda / beta.
        setConstants(gamma, k)
        // 3. Réévaluer a/b pour nouveau lambda/beta
        evalAB(deltaX, deltaVit)
        // 4. Reset time
        setTime = GlobalChrono.elapsedMS32
    }

    /** Set avec options : fixer ou non, setter aussi la position par défaut ou non. */
    fun setPos(newPos: Float, newVit: Float, fix: Boolean, setDef: Boolean) {
        if (setDef)
            defPos = newPos.toNormalizedAngle()
        if (fix) {
            realPos = newPos.toNormalizedAngle()
        } else {
            setPos(newPos, newVit)
        }
    }

    /*----------------------*/
    /*-- Private stuff... --*/
    private var defPos: Float
    private var lastPos: Float
    private var lastVit: Float
    private var setTime: Int
    constructor(posInit: Float) {
        this.defPos = posInit.toNormalizedAngle()
        lastPos = this.defPos
        lastVit = 0f
        setTime = GlobalChrono.elapsedMS32
    }
    constructor(posInit: Float, lambda: Float) : this(posInit) {
        setConstants(2.0f * lambda, lambda * lambda)
    }

    // Private stuff
    private fun setConstants(gamma: Float, k: Float) {
        if (gamma == 0.0f && k == 0.0f) {
            type = SmPosType.Static
            lambda = 0.0f
            beta = 0.0f
            return
        }

        val discr = gamma * gamma - 4 * k

        if (discr > 0.001) {
            type = SmPosType.SurAmorti
            lambda = gamma + sqrt(discr) / 2.0f
            beta = gamma - sqrt(discr) / 2.0f
            return
        }

        if (discr < -0.001) {
            type = SmPosType.OscAmorti
            lambda = gamma / 2.0f
            beta = sqrt(-discr)
            return
        }

        type = SmPosType.AmortiCrit
        lambda = gamma / 2.0f
        beta = gamma / 2.0f
    }
    private fun evalAB(deltaX: Float, deltaVit: Float) {
        when (type) {
            SmPosType.OscAmorti -> {
                a = deltaX
                b = (deltaVit + lambda * a) / beta
            }
            SmPosType.AmortiCrit -> {
                a = deltaX
                b = deltaVit + lambda * a
            }
            SmPosType.SurAmorti -> {
                a = (beta * deltaX + deltaVit) / (beta - lambda)
                b = deltaX - a
            }
            SmPosType.Static -> {
                a = 0.0f; b = 0.0f
            }
        }
    }
/*
    private fun evalAB(newPos: Float, newVit: Float) {
//        val deltaX = pos - newPos
        val deltaX = pos - lastPos + normalizeAngle(lastPos - newPos)
        val deltaVit = vit - newVit
        when (type) {
            SmPosType.OscAmorti -> {
                a = deltaX
                b = (deltaVit + lambda * a) / beta
            }
            SmPosType.AmortiCrit -> {
                a = deltaX
                b = deltaVit + lambda * a
            }
            SmPosType.SurAmorti -> {
                a = (beta * deltaX + deltaVit) / (beta - lambda)
                b = deltaX - a
            }
            SmPosType.Static -> {
                a = 0.0f; b = 0.0f
            }
        }
    }*/

    private val elapsedSec: Float
        get() = (GlobalChrono.elapsedMS32 - setTime).toFloat() * 0.001f

    private enum class SmPosType {
        Static, OscAmorti, AmortiCrit, SurAmorti
    }

    private var a: Float = 0.0f
    private var b: Float = 0.0f
    private var lambda: Float = 0.0f
    private var beta: Float = 0.0f
    private var type: SmPosType = SmPosType.Static

    public override fun clone(): SmPos {
        return super.clone() as SmPos
    }
}