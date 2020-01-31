package com.coq.cocoblio.nodes

import com.coq.cocoblio.R
import com.coq.cocoblio.maths.Vector2
import kotlin.math.max
import kotlin.math.min

/** Classe de base des boutons.
 * Par défaut un bouton n'est qu'un carré sans surface.
 * Un bouton est un SearchableNode avec l'interface Selectionable/Actionable. */
abstract class Button(refNode: Node?,
    x: Float, y: Float, height: Float,
    lambda: Float = 0f, flags: Long = 0
) : SearchableNode(refNode, Flag1.selectableRoot, Flag1.selectable,
    x, y, height, height, lambda, flags), Actionable

/** Classe de base pour un bouton qui doit être reshapé dans le cadre de son parent. */
abstract class ReshapableButton(refNode: Node?,
                                x: Float, y: Float, height: Float,
                                lambda: Float = 0f, flags: Long = 0
) : SearchableNode(refNode,
    Flag1.selectableRoot or Flag1.reshapableRoot,
    Flag1.selectable,
    x, y, height, height, lambda, flags), Actionable, Reshapable


/** Classe de base des boutons de type "on/off".
 * Contient déjà les sous-noeuds de surface d'une switch. */
@Suppress("LeakingThis")
abstract class SwitchButton(refNode: Node?, var isOn: Boolean,
                            x: Float, y: Float, height: Float, lambda: Float = 0f, flags: Long = 0
) : Button(refNode, x, y, height, lambda, flags), Draggable {
    private val back: Surface
    private val nub: Surface

    init {
        scaleX.realPos = height
        scaleY.realPos = height
        this.height.realPos = 1f
        width.realPos = 2f
        back = Surface( this, R.drawable.switch_back, 0f, 0f, 1f)
        nub = Surface(this, R.drawable.switch_front,
            if(isOn) 0.375f else -0.375f, 0f, 1f, 10f)
        setBackColor()
    }

    fun fix(isOn: Boolean) {
        this.isOn = isOn
        nub.x.realPos = if(isOn) 0.375f else -0.375f
        setBackColor()
    }
    /** Simple touche. Permute l'état présent (n'effectue pas l'"action") */
    fun justTapNub() {
        isOn = !isOn
        setBackColor()
        letGo(null)
    }

    override fun grab(posInit: Vector2) : Boolean {
        return false
    }
    /** Déplacement en cours du "nub", aura besoin de letGoNub.
     * newX doit être dans le ref. du SwitchButton.
     * Retourne true si l'état à changer (i.e. action requise ?) */
    override fun drag(posNow: Vector2) : Boolean {
        // 1. Ajustement de la position du nub.
        nub.x.pos = min(max(posNow.x, -0.375f), 0.375f)
        // 2. Vérif si changement
        if(isOn != (posNow.x > 0f)) {
            isOn = posNow.x > 0f
            setBackColor()
            return true
        }
        return false
    }
    /** Ne fait que placer le nub comme il faut. (À faire après avoir dragué.) */
    override fun letGo(speed: Vector2?) : Boolean {
        nub.x.pos = if(isOn) 0.375f else -0.375f
        return false
    }

    private fun setBackColor() {
        if(isOn) {
            back.piu.color[0] = 0.2f; back.piu.color[1] = 1f; back.piu.color[2] = 0.5f
        } else {
            back.piu.color[0] = 1f; back.piu.color[1] = 0.3f; back.piu.color[2] = 0.1f
        }
    }
}

