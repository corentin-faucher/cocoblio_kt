@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import kotlin.math.*

class SmPos(var defPos: Float) : Cloneable {
    /** Vrai position (dernière entrée). Le setter FIXE la position. */
    var realPos: Float
        get() = lastPos
        set(newPos) {
            lastPos = newPos
            a = 0.0f; b = 0.0f
        }

    /** Position estimée au temps présent. Setter met à jour la "real" pos. et crée une nouvelle estimation. */
    var pos: Float
        get() {
            val deltaT = elapsedSec
            return when(type) {
                SmPosType.OscAmorti ->
                    exp(-lambda * deltaT) * (a * cos(beta * deltaT) + b * sin(beta * deltaT)) + lastPos
                SmPosType.AmortiCrit ->
                    (a + b * deltaT) * exp(-lambda * deltaT) + lastPos
                SmPosType.SurAmorti ->
                    a * exp(-lambda * deltaT) + b * exp(-beta * deltaT) + lastPos
                SmPosType.Static -> lastPos
            }
        }
        set(newPos) {
            evalAB(pos - newPos, vit)
            lastPos = newPos
            setTime = GlobalChrono.elapsedMS32
        }

    /** Vitesse estimée au temps présent. */
    val vit: Float
        get() {
            val deltaT = elapsedSec
            return when(type) {
                SmPosType.OscAmorti ->
                    exp(-lambda * deltaT) * ( cos(beta * deltaT) * (beta*b - lambda*a)
                            - sin(beta * deltaT) * (lambda*b + beta*a) )
                SmPosType.AmortiCrit ->
                    exp(-lambda * deltaT) * (b*(1 - lambda*deltaT) - lambda*a)
                SmPosType.SurAmorti ->
                    -lambda * a * exp(-lambda * deltaT) - beta * b * exp(-beta * deltaT)
                SmPosType.Static ->
                    0.0f
            }
        }

    fun updateLambda(lambda: Float) {
        updateConstants(2.0f * lambda, lambda * lambda)
    }
    fun updateConstants(gamma: Float, k: Float) {
        // 1. Enregistrer vit et deltaX avant de changer les constantes
        val xp = vit
        val deltaX = pos - realPos
        // 2. Changer les constantes lambda / beta.
        setConstants(gamma, k)
        // 3. Réévaluer a/b pour nouveau lambda/beta
        evalAB(deltaX, xp)
        // 4. Reset time
        setTime = GlobalChrono.elapsedMS32
    }

    /** Se place à defPos (smooth). */
    fun setToDef() {
        pos = defPos
    }
    /** Set avec options : fixer ou non, setter aussi la position par défaut ou non. */
    fun setPos(newPos: Float, fix: Boolean = true, setDef: Boolean = true) {
        if (setDef)
            defPos = newPos
        if (fix) {
            realPos = newPos
        } else {
            pos = newPos
        }
    }
    /** Se place à defPos + dec. */
    fun setRelToDef(dec: Float) {
        pos = defPos + dec
    }
    /** Se place à defPos + dec avec effet en arrivant par la "droite". */
    fun fadeIn(delta: Float, dec: Float = 0f) {
        realPos = defPos + dec + delta
        pos = defPos + dec
    }
    /** Tasse l'objet en dehors... */
    fun fadeOut(delta: Float) {
        pos = lastPos - delta
    }

    /** Changement de référentiel quelconques (avec positions et scales absolues). */
    fun newReferential(pos: Float, destPos: Float,
                       posScale: Float, destScale: Float) {
        lastPos = (pos - destPos) / destScale
        a = a * posScale / destScale
        b = b * posScale / destScale
    }
    fun newReferentialAsDelta(posScale: Float, destScale: Float) {
        lastPos = lastPos * posScale / destScale
        a = a * posScale / destScale
        b = b * posScale / destScale
    }
    /** Simple changement de référentiel vers le haut.
     *  Se place dans le référentiel du grand-parent. */
    fun referentialUp(oldParentPos: Float, oldParentScaling: Float) {
        lastPos = lastPos * oldParentScaling + oldParentPos
        a *= oldParentScaling
        b *= oldParentScaling
    }
    fun referentialUpAsDelta(oldParentScaling: Float) {
        lastPos *= oldParentScaling
        a *= oldParentScaling
        b *= oldParentScaling
    }
    /** Simple changement de référentiel vers le bas.
     *  Se place dans le reférentiel d'un frère qui devient parent. */
    fun referentialDown(newParentPos: Float, newParentScaling: Float) {
        lastPos = (lastPos - newParentPos) / newParentScaling
        a /= newParentScaling
        b /= newParentScaling
    }
    fun referentialDownAsDelta(newParentScaling: Float) {
        lastPos /= newParentScaling
        a /= newParentScaling
        b /= newParentScaling
    }

    /*----------------------*/
    /*-- Private stuff... --*/
    private var lastPos: Float
    private var setTime: Int
    init {
        lastPos = this.defPos
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
    private fun evalAB(deltaX: Float, xp: Float) {
        when (type) {
            SmPosType.OscAmorti -> {
                a = deltaX
                b = (xp + lambda * a) / beta
            }
            SmPosType.AmortiCrit -> {
                a = deltaX
                b = xp + lambda * a
            }
            SmPosType.SurAmorti -> {
                a = (beta * deltaX + xp) / (beta - lambda)
                b = deltaX - a
            }
            SmPosType.Static -> {
                a = 0.0f; b = 0.0f
            }
        }
    }

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

/*
    private fun evalAB(newPos: Float) {
        val deltaX = pos - newPos
        val xp = vit
        when (type) {
            SmPosType.OscAmorti -> {
                a = deltaX
                b = (xp + lambda * a) / beta
            }
            SmPosType.AmortiCrit -> {
                a = deltaX
                b = xp + lambda * a
            }
            SmPosType.SurAmorti -> {
                a = (beta * deltaX + xp) / (beta - lambda)
                b = deltaX - a
            }
            SmPosType.Static -> {
                a = 0.0f; b = 0.0f
            }
        }
    }
    */