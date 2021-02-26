@file:Suppress("ConvertSecondaryConstructorToPrimary")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.divers.printerror
import kotlin.math.min

interface Escapable {
    fun escapeAction()
}

interface Enterable {
    fun enterAction()
}

interface KeyResponder {
    fun keyDown(key: KeyboardKey)
    fun keyUp(key: KeyboardKey)
    fun modifiersChangedTo(newModifiers: UInt)
}

/** Modèle pour les noeuds racine d'un screen.
 * escapeAction: l'action dans cet écran quand on appuie "escape" (e.g. aller au "main" menu).
 * enterAction: l'action quand on tape "enter". */
abstract class ScreenBase(refNode: Node, flags: Long = 0
) : Node(null, 0f, 0f, 4f, 4f, 0f, flags)
{
    /** Les écrans sont toujours ajoutés juste après l'ainé.
     * add 1 : 0->1,  add 2 : 0->{1,2},  add 3 : 0->{1,3,2},  add 4 : 0->{1,4,3,2}, ...
     * i.e. les deux premiers écrans sont le back et le front respectivement,
     * les autres sont au milieu. */
    init {
        (refNode.firstChild as? ScreenBase)?.let { elder ->
            simpleMoveToBro(elder, false)
        } ?: run {
            simpleMoveToParent(refNode, false)
        }
    }

    override fun open() {
        alignScreenElements(true)
    }
    override fun reshape() : Boolean {
        alignScreenElements(false)
        return true
    }
    /** En général un écran est constitué de deux "blocs"
     * alignés horizontalement ou verticalement en fonction de l'orientation de l'appareil. */
    private fun alignScreenElements(isOpening:  Boolean) {
        val theParent = parent ?: run { printerror("Pas de parent."); return}
        if (!containsAFlag(Flag1.dontAlignScreenElements)) {
            val ceiledScreenRatio = theParent.width.realPos / theParent.height.realPos
            var alignOpt = AlignOpt.respectRatio or AlignOpt.setSecondaryToDefPos
            if (ceiledScreenRatio < 1f)
                alignOpt = alignOpt or AlignOpt.vertically
            if (isOpening)
                alignOpt = alignOpt or AlignOpt.fixPos

            this.alignTheChildren(alignOpt, ceiledScreenRatio)

            val scale = min(
                theParent.width.realPos/ width.realPos,
                theParent.height.realPos / height.realPos)
            scaleX.set(scale, isOpening)
            scaleY.set(scale, isOpening)
        } else {
            scaleX.set(1f, isOpening)
            scaleY.set(1f, isOpening)
            width.set(theParent.width.realPos, isOpening)
            height.set(theParent.height.realPos, isOpening)
        }
    }
}
