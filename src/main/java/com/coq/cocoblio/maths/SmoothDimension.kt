@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio.maths

import com.coq.cocoblio.GlobalChrono
import kotlin.math.*

/** Classe pour gérer les déplacement "smooth", i.e. avec courbe de lissage.
 * Utile pour faire des transition et déplacements graduels des objets (nodes)
 * dans l'écran. */
abstract class SmoothDimension : Cloneable {
    /** Mémoire/position par défaut. */
    var defPos: Float
    /** Dernière position entrée. */
    var realPos: Float
        protected set
    /** Le getter/setter par défaut pour les SmoothDimension.
     * set: setter "smooth", i.e. set(newPos, fix = false, setAsDef = false).
     * get: getter "smooth", i.e. utilise la courbe de lissage (et non realPos directement). */
    open var pos: Float
        get() {
            return getDelta(elapsedSec) + realPos
        }
        set(newPos) {
            set(newPos, fix = false, setAsDef = false)
        }
    /** Getter de la vitesse actuelle. */
    open val speed: Float
        get() = getSlope(elapsedSec)

    constructor(posInit: Float) {
        defPos = posInit
        realPos = posInit
    }
    constructor(posInit: Float, lambda: Float) : this(posInit) {
        setLambdaBetaType(2f * lambda, lambda * lambda)
    }

    /*-- !! Setter Important !! --*/
    /** Set : Par défaut on "initialise", i.e. fixe la position et
     * l'enregistre comme position par défaut. */
    open fun set(newPos: Float, fix: Boolean = true, setAsDef: Boolean = true) {
        if (setAsDef)
            defPos = newPos
        if (fix) {
            a = 0.0f; b = 0.0f
        } else {
            val deltaT = elapsedSec
            setAB(getDelta(deltaT) + realPos - newPos, getSlope(deltaT))
            setTime = GlobalChrono.elapsedMS32
        }
        realPos = newPos
    }
    /*-----------------------------*/

    /*-- Sous-Setters de "conveniance". --*/
    /** Se place par rapport à defPos. */
    fun setRelToDef(shift: Float, fix: Boolean) {
        set(defPos + shift, fix, false)
    }
    /** move: place par rapport à realPos (i.e. un déplacement)
     * Pourrait être "setRelToRealPos"... */
    fun move(shift: Float, fix: Boolean = false, setAsDef: Boolean = true) {
        set(realPos + shift, fix, setAsDef)
    }
    fun fadeIn(delta: Float? = null) {
        setRelToDef(delta ?: defaultFadeDelta, true)
        set(defPos, fix = false, setAsDef = false)
    }
    fun fadeOut(delta: Float? = null) {
        set(realPos -(delta ?: defaultFadeDelta), fix = false, setAsDef = false)
    }
    /** Met à jour comme amortissement-critique
     * où lambda est le paramètre de l'exp. décroissante. */
    fun updateCurve(lambda: Float) {
        updateCurve(2.0f * lambda, lambda * lambda)
    }
    /** Met à jour comme un systeme masse/ressort/amortisement. (m=1)
     * gamma : constante d'amortissement.
     * k : constante du ressort. */
    fun updateCurve(gamma: Float, k: Float) {
        // 1. Enregistrer delta et pente avant de modifier la courbe.
        val deltaT = elapsedSec
        val slope = getSlope(deltaT)
        val delta = getDelta(deltaT)
        // 2. Mise à jour des paramètres de la courbe
        setLambdaBetaType(gamma, k)
        // 3. Réévaluer a/b pour nouveau lambda/beta
        setAB(delta, slope)
        // 4. Reset time
        setTime = GlobalChrono.elapsedMS32
    }

    /*-- Changements de référentiel... --*/
    /** Changement de référentiel quelconques (avec positions et scales absolues). */
    fun newReferential(pos: Float, destPos: Float,
                       posScale: Float, destScale: Float) {
        realPos = (pos - destPos) / destScale
        a = a * posScale / destScale
        b = b * posScale / destScale
    }
    fun newReferentialAsDelta(posScale: Float, destScale: Float) {
        realPos = realPos * posScale / destScale
        a = a * posScale / destScale
        b = b * posScale / destScale
    }
    /** Simple changement de référentiel vers le haut.
     *  Se place dans le référentiel du grand-parent. */
    fun referentialUp(oldParentPos: Float, oldParentScaling: Float) {
        realPos = realPos * oldParentScaling + oldParentPos
        a *= oldParentScaling
        b *= oldParentScaling
    }
    fun referentialUpAsDelta(oldParentScaling: Float) {
        realPos *= oldParentScaling
        a *= oldParentScaling
        b *= oldParentScaling
    }
    /** Simple changement de référentiel vers le bas.
     *  Se place dans le reférentiel d'un frère qui devient parent. */
    fun referentialDown(newParentPos: Float, newParentScaling: Float) {
        realPos = (realPos - newParentPos) / newParentScaling
        a /= newParentScaling
        b /= newParentScaling
    }
    fun referentialDownAsDelta(newParentScaling: Float) {
        realPos /= newParentScaling
        a /= newParentScaling
        b /= newParentScaling
    }

    /*-- Relatif à la courbe de lissage... --*/
    protected var a: Float =0f
    protected var b: Float = 0f
    protected var lambda: Float = 0f
    protected var beta: Float = 0f
    protected var type: SmDimType = SmDimType.Static
    protected var setTime: Int = GlobalChrono.elapsedMS32
    /** Evalue le temps écoulé depuis le dernier "set". */
    protected val elapsedSec: Float
        get() = (GlobalChrono.elapsedMS32 - setTime).toFloat() * 0.001f
    protected fun setLambdaBetaType(gamma: Float, k: Float) {
        if (gamma == 0.0f && k == 0.0f) {
            type = SmDimType.Static
            lambda = 0.0f
            beta = 0.0f
            return
        }

        val discr = gamma * gamma - 4 * k

        if (discr > 0.001) {
            type = SmDimType.SurAmorti
            lambda = gamma + sqrt(discr) / 2.0f
            beta = gamma - sqrt(discr) / 2.0f
            return
        }

        if (discr < -0.001) {
            type = SmDimType.OscAmorti
            lambda = gamma / 2.0f
            beta = sqrt(-discr)
            return
        }

        type = SmDimType.AmortiCrit
        lambda = gamma / 2.0f
        beta = gamma / 2.0f
    }
    protected fun setAB(delta: Float, slope: Float) {
        when (type) {
            SmDimType.OscAmorti -> {
                a = delta
                b = (slope + lambda * a) / beta
            }
            SmDimType.AmortiCrit -> {
                a = delta
                b = slope + lambda * a
            }
            SmDimType.SurAmorti -> {
                a = (beta * delta + slope) / (beta - lambda)
                b = delta - a
            }
            SmDimType.Static -> {
                a = 0.0f; b = 0.0f
            }
        }
    }
    protected fun getSlope(deltaT: Float) : Float {
        return when(type) {
            SmDimType.OscAmorti ->
                exp(-lambda * deltaT) * ( cos(beta * deltaT) * (beta*b - lambda*a)
                        - sin(beta * deltaT) * (lambda*b + beta*a) )
            SmDimType.AmortiCrit ->
                exp(-lambda * deltaT) * (b*(1 - lambda*deltaT) - lambda*a)
            SmDimType.SurAmorti ->
                -lambda * a * exp(-lambda * deltaT) - beta * b * exp(-beta * deltaT)
            SmDimType.Static ->
                0.0f
        }
    }
    /** Delta est la position relative par rapport à realPos la "vrai" position entrée.
     * Delta typiquement tend vers 0 quand T -> infty. */
    protected fun getDelta(deltaT: Float) : Float {
        return when(type) {
            SmDimType.OscAmorti ->
                exp(-lambda * deltaT) * (a * cos(beta * deltaT) + b * sin(beta * deltaT))
            SmDimType.AmortiCrit ->
                (a + b * deltaT) * exp(-lambda * deltaT)
            SmDimType.SurAmorti ->
                a * exp(-lambda * deltaT) + b * exp(-beta * deltaT)
            SmDimType.Static -> 0f
        }
    }

    protected enum class SmDimType {
        Static, OscAmorti, AmortiCrit, SurAmorti
    }

    companion object {
        var defaultFadeDelta = 3f
    }
}

/** Position "smooth" (évolue doucement dans le temps).
 * defPos est une mémoire de l'emplacement par défaut et/ou
 * de l'emplacement relatif au parent
 * (e.g. position par rapport au côté droit du cadre du parent).
 * */
class SmoothPos : SmoothDimension {
    constructor(posInit: Float) : super(posInit)
    constructor(posInit: Float, lambda: Float) : super(posInit, lambda)
    public override fun clone(): SmoothPos {
        return super.clone() as SmoothPos
    }
}

open class SmoothAngle : SmoothDimension {
    constructor(posInit: Float) : super(posInit)
    constructor(posInit: Float, lambda: Float) : super(posInit, lambda)

    override fun set(newPos: Float, fix: Boolean, setAsDef: Boolean) {
        if (setAsDef)
            defPos = newPos.toNormalizedAngle()
        if (fix) {
            a = 0.0f; b = 0.0f
        } else {
            val deltaT = elapsedSec
            setAB((getDelta(deltaT) + realPos - newPos).toNormalizedAngle(),
                getSlope(deltaT))
            setTime = GlobalChrono.elapsedMS32
        }
        realPos = newPos.toNormalizedAngle()
    }

    public override fun clone(): SmoothAngle {
        return super.clone() as SmoothAngle
    }
}

class SmoothAngleWithDrift : SmoothAngle {
    var drift: Float = 0f
        private set

    override var pos: Float
        get() {
            val deltaT = elapsedSec
            return getDelta(deltaT) + drift * deltaT + realPos
        }
        set(newPos) {
            set(newPos, fix = false, setAsDef = false)
        }
    override val speed: Float
        get() = getSlope(elapsedSec) + drift

    constructor(posInit: Float) : super(posInit)
    constructor(posInit: Float, lambda: Float) : super(posInit, lambda)

    override fun set(newPos: Float, fix: Boolean, setAsDef: Boolean) {
        if (setAsDef)
            defPos = newPos.toNormalizedAngle()

        if (fix) {
            a = 0.0f; b = 0.0f
        } else {
            val deltaT = elapsedSec
            setAB(
                (getDelta(deltaT) + drift * deltaT + realPos - newPos).toNormalizedAngle(),
                getSlope(deltaT) + drift)
            setTime = GlobalChrono.elapsedMS32
        }
        realPos = newPos.toNormalizedAngle()
        drift = 0f
    }
    fun set(newPos: Float, newDrift: Float) {
        val deltaT = elapsedSec
        setAB((getDelta(deltaT) + drift * deltaT + realPos - newPos).toNormalizedAngle(),
            getSlope(deltaT) + drift - newDrift)
        setTime = GlobalChrono.elapsedMS32
        realPos = newPos.toNormalizedAngle()
        drift = newDrift
    }

    override fun clone(): SmoothAngleWithDrift {
        return super.clone() as SmoothAngleWithDrift
    }
}



/*
class SmoothPos(var defPos: Float) : Cloneable {
    /** Vrai position (dernière entrée). Le setter FIXE la position. */
    var realPos: Float
        private set
        /*
        get() = lastPos
        set(newPos) {
            lastPos = newPos
            a = 0.0f; b = 0.0f
        }*/

    /** Position estimée au temps présent.
     * Pour le setter: équivalent de set(newPos, fix = false, setDef = false),
     * i.e. met à jour la "real" pos. et crée une nouvelle estimation. */
    var pos: Float
        get() {
            val deltaT = elapsedSec
            return when(type) {
                SmDimType.OscAmorti ->
                    exp(-lambda * deltaT) * (a * cos(beta * deltaT) + b * sin(beta * deltaT)) + realPos
                SmDimType.AmortiCrit ->
                    (a + b * deltaT) * exp(-lambda * deltaT) + realPos
                SmDimType.SurAmorti ->
                    a * exp(-lambda * deltaT) + b * exp(-beta * deltaT) + realPos
                SmDimType.Static -> realPos
            }
        }
        set(newPos) {
            set(newPos, fix = false, setAsDef = false)
            /*
            evalAB(pos - newPos, vit)
            realPos = newPos
            setTime = GlobalChrono.elapsedMS32
             */
        }

    /** Vitesse estimée au temps présent. */
    val vit: Float
        get() {
            val deltaT = elapsedSec
            return when(type) {
                SmDimType.OscAmorti ->
                    exp(-lambda * deltaT) * ( cos(beta * deltaT) * (beta*b - lambda*a)
                            - sin(beta * deltaT) * (lambda*b + beta*a) )
                SmDimType.AmortiCrit ->
                    exp(-lambda * deltaT) * (b*(1 - lambda*deltaT) - lambda*a)
                SmDimType.SurAmorti ->
                    -lambda * a * exp(-lambda * deltaT) - beta * b * exp(-beta * deltaT)
                SmDimType.Static ->
                    0.0f
            }
        }
    private var setTime: Int

    init {
        realPos = this.defPos
        setTime = GlobalChrono.elapsedMS32
    }
    constructor(posInit: Float, lambda: Float) : this(posInit) {
        setConstants(2.0f * lambda, lambda * lambda)
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
    fun set(newPos: Float, fix: Boolean = true, setAsDef: Boolean = true) {
        if (setAsDef)
            defPos = newPos
        if (fix) {
            a = 0.0f; b = 0.0f
        } else {
            evalAB(pos - newPos, vit)
            setTime = GlobalChrono.elapsedMS32
        }
        realPos = newPos
    }
    /** Se place à defPos + dec. */
    fun setRelToDef(shift: Float, fix: Boolean) {
        if (fix) {
            a = 0.0f; b = 0.0f
        } else {
            evalAB(pos - (defPos + shift), vit)
            setTime = GlobalChrono.elapsedMS32
        }
        realPos = defPos + shift
    }
    /** Se place à realPos + delta puis se remet où on était.
     * (Donne l'effet d'arriver par la "droite" (si pos en x)). */
    fun fadeIn(delta: Float? = null) {
        setRelToDef(delta ?: defaultFadeDelta, true)
        pos = defPos
    }
    /** Tasse l'objet en dehors... */
    fun fadeOut(delta: Float? = null) {
        pos = realPos - (delta ?: defaultFadeDelta)
    }

    /** Changement de référentiel quelconques (avec positions et scales absolues). */
    fun newReferential(pos: Float, destPos: Float,
                       posScale: Float, destScale: Float) {
        realPos = (pos - destPos) / destScale
        a = a * posScale / destScale
        b = b * posScale / destScale
    }
    fun newReferentialAsDelta(posScale: Float, destScale: Float) {
        realPos = realPos * posScale / destScale
        a = a * posScale / destScale
        b = b * posScale / destScale
    }
    /** Simple changement de référentiel vers le haut.
     *  Se place dans le référentiel du grand-parent. */
    fun referentialUp(oldParentPos: Float, oldParentScaling: Float) {
        realPos = realPos * oldParentScaling + oldParentPos
        a *= oldParentScaling
        b *= oldParentScaling
    }
    fun referentialUpAsDelta(oldParentScaling: Float) {
        realPos *= oldParentScaling
        a *= oldParentScaling
        b *= oldParentScaling
    }
    /** Simple changement de référentiel vers le bas.
     *  Se place dans le reférentiel d'un frère qui devient parent. */
    fun referentialDown(newParentPos: Float, newParentScaling: Float) {
        realPos = (realPos - newParentPos) / newParentScaling
        a /= newParentScaling
        b /= newParentScaling
    }
    fun referentialDownAsDelta(newParentScaling: Float) {
        realPos /= newParentScaling
        a /= newParentScaling
        b /= newParentScaling
    }

    /*----------------------*/
    /*-- Private stuff... --*/
//    private var lastPos: Float // Dernière position entrée (vrai realPos...).


    // Private stuff
    private fun setConstants(gamma: Float, k: Float) {
        if (gamma == 0.0f && k == 0.0f) {
            type = SmDimType.Static
            lambda = 0.0f
            beta = 0.0f
            return
        }

        val discr = gamma * gamma - 4 * k

        if (discr > 0.001) {
            type = SmDimType.SurAmorti
            lambda = gamma + sqrt(discr) / 2.0f
            beta = gamma - sqrt(discr) / 2.0f
            return
        }

        if (discr < -0.001) {
            type = SmDimType.OscAmorti
            lambda = gamma / 2.0f
            beta = sqrt(-discr)
            return
        }

        type = SmDimType.AmortiCrit
        lambda = gamma / 2.0f
        beta = gamma / 2.0f
    }
    private fun evalAB(deltaX: Float, xp: Float) {
        when (type) {
            SmDimType.OscAmorti -> {
                a = deltaX
                b = (xp + lambda * a) / beta
            }
            SmDimType.AmortiCrit -> {
                a = deltaX
                b = xp + lambda * a
            }
            SmDimType.SurAmorti -> {
                a = (beta * deltaX + xp) / (beta - lambda)
                b = deltaX - a
            }
            SmDimType.Static -> {
                a = 0.0f; b = 0.0f
            }
        }
    }

    private val elapsedSec: Float
        get() = (GlobalChrono.elapsedMS32 - setTime).toFloat() * 0.001f



    private var a: Float = 0.0f
    private var b: Float = 0.0f
    private var lambda: Float = 0.0f
    private var beta: Float = 0.0f
    private var type: SmDimType = SmDimType.Static

    public override fun clone(): SmoothPos {
        return super.clone() as SmoothPos
    }
    companion object {
        var defaultFadeDelta = 2.2f
    }
    protected enum class SmDimType {
        Static, OscAmorti, AmortiCrit, SurAmorti
    }
}
*/
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