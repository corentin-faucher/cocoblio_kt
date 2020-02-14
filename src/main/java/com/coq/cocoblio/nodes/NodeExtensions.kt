package com.coq.cocoblio.nodes

import com.coq.cocoblio.*
import com.coq.cocoblio.maths.*
import com.coq.cocoblio.nodes.Node.Companion.showFrame
import kotlin.math.abs

/*---------------------------------------*/
/*-- Quelques extensions utiles. --------*/
/*---------------------------------------*/

/** Extension/wrapper pratique pour ajouter une surface à un noeud. */
fun Node.addSurface(pngResID: Int, x: Float, y: Float, height: Float,
                    lambda: Float = 0f, i: Int = 0, flags: Long = 0) {
    Surface(this, pngResID, x, y, height, lambda, i, flags)
}

/** Ajout d'un frame et string à un noeud.
 * La hauteur devient 1 et ses scales deviennent sa hauteur.
 * (Pour avoir les objets (label...) relatif au noeud.)
 * Delta est un pourcentage de la hauteur. */
fun Node.fillWithFrameAndLocStr(locStrId: Int, frameResID: Int = R.drawable.frame_mocha,
                                       delta: Float = 0.40f, ceiledWidth: Float? = null) {
    if (firstChild != null) { printerror("A déjà quelque chose."); return}

    scaleX.set(height.realPos)
    scaleY.set(height.realPos)
    val scaleCeiledWidth = ceiledWidth?.let{it / height.realPos}
    width.set(1f)
    height.set(1f)
    Frame(this, false, delta,
        0f, frameResID, Flag1.giveSizesToParent)
    LocStrSurf(this, locStrId, 0f, 0f, 1f,
        0f, Flag1.giveSizesToBigBroFrame, scaleCeiledWidth)
}

/** Ajoute la structure root->{frame, locStrSurf} au noeud présent.
 * (Bref, ajoute un noeud en (x,y) init avec addFrameAndLocStr.) */
fun Node.addFramedLocStr(locStrId: Int, framePngId: Int,
                         x: Float, y: Float, height: Float, lambda: Float = 0f,
                         flags: Long = 0, ceiledWidth: Float? = null, delta: Float = 0.4f) {
    Node(this, x, y, ceiledWidth ?: height, height, lambda, flags).apply {
        fillWithFrameAndLocStr(locStrId, framePngId, delta, ceiledWidth)
    }

}

fun <T: Node> T.alsoAddCstStrWithFrame(str: String, frameResID: Int = R.drawable.frame_mocha,
                                       delta: Float = 0.40f) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this}

    scaleX.set(height.realPos)
    scaleY.set(height.realPos)
    width.set(1f)
    height.set(1f)

    Frame(this, false, delta,
        0f, frameResID, Flag1.giveSizesToParent)
    CstStrSurf(this, str, 0f, 0f, 1f,
        0f, Flag1.giveSizesToBigBroFrame)
    return this
}

/** Ajout d'un frame et string à un noeud.
 * La hauteur devient 1 et ses scales deviennent sa hauteur. Delta est un pourcentage. */
fun <T: Node> T.alsoAddEdtStrWithFrame(id: Int, frameResID: Int = R.drawable.frame_mocha,
                                       delta: Float = 0.40f) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this}

    scaleX.set(height.realPos)
    scaleY.set(height.realPos)
    width.set(1f)
    height.set(1f)

    Frame(this, false, delta,
        0f, frameResID, Flag1.giveSizesToParent)
    EdtStrSurf(this, id, 0f, 0f, 1f,
        0f, Flag1.giveSizesToBigBroFrame)
    return this
}

/** !Debug Option!
 * Ajout d'une surface "frame" pour visualiser la taille d'un "bloc".
 * L'option Node.showFrame doit être "true". */
fun Node.tryToAddFrame() {
    if (!showFrame) return
    TestFrame(this)
}

/** Aligner les descendants d'un noeud. Retourne le nombre de descendants traités. */
fun Node.alignTheChildren(alignOpt: Int, ratio: Float = 1f, spacingRef: Float = 1f) : Int {
    var sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden)) {
        printerror("pas de child.");return 0}
    // 0. Les options...
    val fix = (alignOpt and AlignOpt.fixPos != 0)
    val horizontal = (alignOpt and AlignOpt.vertically == 0)
    val setAsDef = (alignOpt and AlignOpt.setAsDefPos != 0)
    val setSecondaryToDefPos = (alignOpt and AlignOpt.setSecondaryToDefPos != 0)
    // 1. Setter largeur/hauteur
    var w = 0f
    var h = 0f
    var n = 0
    if (horizontal) {
        do {
            w += sq.pos.deltaX * 2f * spacingRef
            n += 1
            if (sq.pos.deltaY*2f > h) {
                h = sq.pos.deltaY*2f
            }
        } while (sq.goRightWithout(Flag1.hidden))
    }
    else {
        do {
            h += sq.pos.deltaY * 2f * spacingRef
            n += 1
            if (sq.pos.deltaX*2f > w) {
                w = sq.pos.deltaX*2f
            }
        } while (sq.goRightWithout(Flag1.hidden))
    }
    // 2. Ajuster l'espacement supplémentaire pour respecter le ratio
    var spacing = 0f
    if (alignOpt and AlignOpt.respectRatio != 0) {
        if(horizontal) {
            if  (w/h < ratio) {
                spacing = (ratio * h - w) / n.toFloat()
                w = ratio * h
            }
        } else {
            if (w/h > ratio) {
                spacing = (w/ratio - h) / n.toFloat()
                h = w / ratio
            }
        }
    }
    // 3. Setter les dims.
    if (alignOpt and AlignOpt.dontUpdateSizes == 0) {
        width.set(w, fix, setAsDef)
        height.set(h, fix, setAsDef)
    }
    // 4. Aligner les éléments
    sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden)) {
        printerror("pas de child2.");return 0}
    // 4.1 Placement horizontal
    if(horizontal) {
        var x = - w / 2f
        do {
            x += sq.pos.deltaX * spacingRef + spacing/2f

            sq.pos.x.set(x, fix, setAsDef)
            if (setSecondaryToDefPos) {
                sq.pos.y.setRelToDef(0f, fix)
            } else {
                sq.pos.y.set(0f, fix, false)
            }

            x += sq.pos.deltaX * spacingRef + spacing/2f
        } while (sq.goRightWithout(Flag1.hidden))
        return n
    }
    // 4.2 Placement vertical
    var y =  h / 2f
    do {
        y -= sq.pos.deltaY * spacingRef + spacing/2f

        sq.pos.y.set(y, fix, setAsDef)
        if (setSecondaryToDefPos) {
            sq.pos.x.setRelToDef(0f, fix)
        } else {
            sq.pos.x.set(0f, fix, false)
        }

        y -= sq.pos.deltaY * spacingRef + spacing/2f
    } while (sq.goRightWithout(Flag1.hidden))

    return n
}

object AlignOpt {
    const val vertically = 1
    const val dontUpdateSizes = 2
    const val respectRatio = 4
    const val fixPos = 8
    /** En horizontal, le "primary" est "x" des children,
     * le "secondary" est "y". (En vertical prim->"y", sec->"x".)
     * Place la position "alignée" comme étant la position par défaut pour le primary des children
     * et pour le width/height du parent. Ne touche pas à defPos du secondary des children. */
    const val setAsDefPos = 16
    /** S'il y a "setSecondaryToDefPos", on place "y" à sa position par défaut,
     * sinon, on le place à zéro. */
    const val setSecondaryToDefPos = 32
}

fun Node.adjustWidthAndHeightFromChildren() {
    var w = 0f
    var h = 0f
    var htmp: Float
    var wtmp: Float
    val sq = Squirrel(this)
    if(!sq.goDownWithout(Flag1.hidden)) { return}
    do {
        htmp = (sq.pos.deltaY + abs(sq.pos.y.realPos)) * 2f
        if (htmp > h)
            h = htmp
        wtmp = (sq.pos.deltaX + abs(sq.pos.x.realPos)) * 2f
        if (wtmp > w)
            w = wtmp
    } while (sq.goRightWithout(Flag1.hidden))
    width.set(w)
    height.set(h)
}
