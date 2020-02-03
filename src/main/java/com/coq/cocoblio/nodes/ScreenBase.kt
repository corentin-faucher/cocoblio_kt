package com.coq.cocoblio.nodes

import com.coq.cocoblio.CoqRenderer
import kotlin.math.min

/** Modèle pour les noeuds racine d'un screen.
 * escapeAction: l'action dans cet écran quand on appuie "escape" (e.g. aller au "main" menu).
 * enterAction: l'action quand on tape "enter". */
abstract class ScreenBase(refNode: Node,
                           val escapeAction: (() -> Unit)?, val enterAction: (() -> Unit)?,
                           flags: Long = 0
) : SearchableNode(refNode, Flag1.reshapableRoot, 0L,
    0f, 0f, 4f, 4f, 0f, flags), Reshapable, Openable {
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
        if (!containsAFlag(Flag1.dontAlignScreenElements)) {
            val ceiledScreenRatio = CoqRenderer.frameUsableWidth / CoqRenderer.frameUsableHeight
            var alignOpt = AlignOpt.respectRatio or AlignOpt.setSecondaryToDefPos
            if (ceiledScreenRatio < 1f)
                alignOpt = alignOpt or AlignOpt.vertically
            if (isOpening)
                alignOpt = alignOpt or AlignOpt.fixPos

            this.alignTheChildren(alignOpt, ceiledScreenRatio)

            val scale = min(
                CoqRenderer.frameUsableWidth / width.realPos,
                CoqRenderer.frameUsableHeight / height.realPos)
            scaleX.set(scale, isOpening)
            scaleY.set(scale, isOpening)
        } else {
            scaleX.set(1f, isOpening)
            scaleY.set(1f, isOpening)
            width.set(CoqRenderer.frameUsableWidth, isOpening)
            height.set(CoqRenderer.frameUsableHeight, isOpening)
        }
    }
}
