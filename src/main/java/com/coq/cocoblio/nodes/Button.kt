@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.R
import com.coq.cocoblio.maths.Vector2
import kotlin.math.max
import kotlin.math.min


/** Classe de base des boutons.
 * Par défaut un bouton n'est qu'un carré sans surface.
 * Un bouton est un SelectableNode qui est Actionable. */
abstract class Button(refNode: Node?,
    x: Float, y: Float, height: Float,
    lambda: Float = 0f, flags: Long = 0
) : Node(refNode, x, y, height, height, lambda, flags)
{
    init {
        makeSelectable()
    }
    abstract fun action()
}

/** Pour les noeuds "déplaçable".
 * 1. On prend le noeud : "grab",
 * 2. On le déplace : "drag",
 * 3. On le relâche : "letGo".
 * Un noeud Draggable doit être dans une classe descendante de SelectableNode.
 * On utilise les flags selectable et selectableRoot pour les trouver.
 * (On peut être draggable mais pas actionable, e.g. le sliding menu.) */
interface Draggable {
    fun grab(posInit: Vector2)
    fun drag(posNow: Vector2)
    fun letGo(speed: Vector2?)
    fun justTap()
}


/** Classe de base des boutons de type "on/off".
 * Contient déjà les sous-noeuds de surface d'une switch. */
@Suppress("LeakingThis")
abstract class SwitchButton(refNode: Node?, var isOn: Boolean,
                            x: Float, y: Float, height: Float, lambda: Float = 0f, flags: Long = 0
) : Node(refNode, x, y, height, height, lambda, flags), Draggable
{
    private val back: Surface
    private val nub: Surface

    init {
        makeSelectable()
        scaleX.set(height)
        scaleY.set(height)
        this.height.set(1f)
        width.set(2f)
        back = TiledSurface(this, R.drawable.switch_back, 0f, 0f, 1f)
        nub = TiledSurface(this, R.drawable.switch_front,
            if(isOn) 0.375f else -0.375f, 0f, 1f, 10f)
        setBackColor()
    }

    fun fix(isOn: Boolean) {
        this.isOn = isOn
        nub.x.set(if(isOn) 0.375f else -0.375f)
        setBackColor()
    }

    abstract fun action()

    /** Simple touche. Permute l'état présent (n'effectue pas l'"action") */
    override fun justTap() {
        isOn = !isOn
        setBackColor()
        letGo(null)
        action()
    }

    override fun grab(posInit: Vector2) {
        // (pass)
    }
    /** Déplacement en cours du "nub", aura besoin de letGoNub.
     * newX doit être dans le ref. du SwitchButton.
     * Retourne true si l'état à changer (i.e. action requise ?) */
    override fun drag(posNow: Vector2) {
        // 1. Ajustement de la position du nub.
        nub.x.pos = min(max(posNow.x, -0.375f), 0.375f)
        // 2. Vérif si changement
        if(isOn != (posNow.x > 0f)) {
            isOn = posNow.x > 0f
            setBackColor()
            action()
        }
    }
    /** Ne fait que placer le nub comme il faut. (À faire après avoir dragué.) */
    override fun letGo(speed: Vector2?) {
        nub.x.pos = if(isOn) 0.375f else -0.375f
    }

    private fun setBackColor() {
        if(isOn) {
            back.piu.color[0] = 0.2f; back.piu.color[1] = 1f; back.piu.color[2] = 0.5f
        } else {
            back.piu.color[0] = 1f; back.piu.color[1] = 0.3f; back.piu.color[2] = 0.1f
        }
    }
}

abstract class SliderButton(parent: Node, var value: Float, val actionAtLetGo: Boolean,
                            x: Float, y: Float, height: Float, val slideWidth: Float,
                            lambda: Float = 0f, flags: Long = 0
) : Node(parent, x, y, slideWidth + height, height, lambda, flags), Draggable
{
    private lateinit var nub: Surface

    init {
        initStructure()
    }
    private fun initStructure() {
        makeSelectable()
        Bar(this, Framing.inside, 0.25f * this.height.realPos, slideWidth,
                R.drawable.bar_in)
        val xPos: Float = (value - 0.5f) * slideWidth
        nub = TiledSurface(this, R.drawable.switch_front,
                xPos, 0f, this.height.realPos, 20f)
    }

    abstract fun action()

    override fun grab(posInit: Vector2) {
        // (pass)
    }

    override fun drag(posNow: Vector2) {
        // 1. Ajustement de la position du nub.
        nub.x.pos = min(max(posNow.x, -slideWidth/2), slideWidth/2)
        value = nub.x.realPos / slideWidth + 0.5f
        // 2. Action!
        if (!actionAtLetGo)
            action()
    }

    override fun letGo(speed: Vector2?) {
        if(actionAtLetGo)
            action()
    }

    override fun justTap() {
        // (pass)
    }

}