package com.coq.cocoblio.nodes

import com.coq.cocoblio.CoqRenderer
import kotlin.math.min

/** Modèle pour les noeuds racine d'un screen.
 * escapeAction: l'action dans cet écran quand on appuie "escape".
 * enterAction: l'action quand on tape "enter". */
abstract class ScreenBase(refNode: Node,
                          val escapeAction: (() -> Unit)?, val enterAction: (() -> Unit)?,
                          flags: Long = 0
) : Node(refNode, 0f, 0f, 4f, 4f, 0f, flags), OpenableNode {

    override fun open() {
        reshape(true)
    }
    fun reshape(isOpening:  Boolean) {
        if (!containsAFlag(Flag1.dontAlignScreenElements)) {
            val ceiledScreenRatio = CoqRenderer.frameUsableWidth / CoqRenderer.frameUsableHeight
            var alignOpt = AlignOpt.respectRatio or AlignOpt.dontSetAsDef
            if (ceiledScreenRatio < 1f)
                alignOpt = alignOpt or AlignOpt.vertically
            if (isOpening)
                alignOpt = alignOpt or AlignOpt.fixPos

            this.alignTheChildren(alignOpt, ceiledScreenRatio)

            val scale = min(
                CoqRenderer.frameUsableWidth / width.realPos,
                CoqRenderer.frameUsableHeight / height.realPos)
            scaleX.setPos(scale, isOpening)
            scaleY.setPos(scale, isOpening)
        } else {
            scaleX.setPos(1f, isOpening)
            scaleY.setPos(1f, isOpening)
            width.setPos(CoqRenderer.frameUsableWidth, isOpening)
            height.setPos(CoqRenderer.frameUsableHeight, isOpening)
        }
    }
}
