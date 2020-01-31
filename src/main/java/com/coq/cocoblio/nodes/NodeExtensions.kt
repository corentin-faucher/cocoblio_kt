package com.coq.cocoblio.nodes

import com.coq.cocoblio.*
import com.coq.cocoblio.maths.*
import com.coq.cocoblio.nodes.Node.Companion.showFrame
import kotlin.math.abs

/*---------------------------------------*/
/*-- Quelques extensions utiles. --------*/
/*---------------------------------------*/

/** Ajout d'un frame et string à un noeud.
 * La hauteur devient 1 et ses scales deviennent sa hauteur.
 * (Pour avoir les objets (label...) relatif au noeud.)
 * Delta est un pourcentage de la hauteur. */
fun <T: Node> T.alsoAddLocStrWithFrame(id: Int, frameResID: Int = R.drawable.frame_mocha,
                                       delta: Float = 0.40f, ceiledWidth: Float? = null) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this}

    scaleX.setPos(height.realPos)
    scaleY.setPos(height.realPos)
    val scaleCeiledWidth = ceiledWidth?.let{it / height.realPos}
    width.setPos(1f)
    height.setPos(1f)
    Frame(this, false, delta,
        0f, frameResID, Flag1.giveSizesToParent)
    LocStrSurf(this, id, 0f, 0f, 1f,
        0f, Flag1.giveSizesToBigBroFrame, scaleCeiledWidth)
    return this
}

fun <T: Node> T.alsoAddCstStrWithFrame(str: String, frameResID: Int = R.drawable.frame_mocha,
                                       delta: Float = 0.40f) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this}

    scaleX.setPos(height.realPos)
    scaleY.setPos(height.realPos)
    width.setPos(1f)
    height.setPos(1f)

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

    scaleX.setPos(height.realPos)
    scaleY.setPos(height.realPos)
    width.setPos(1f)
    height.setPos(1f)

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
    Surface(this, R.drawable.test_frame, 0f, 0f, height.realPos,
        0f, 0, Flag1.surfaceDontRespectRatio
    ).also{it.width.setPos(width.realPos)}
}

/** Aligner les descendants d'un noeud. Retourne le nombre de descendants traités. */
fun Node.alignTheChildren(alignOpt: Int, ratio: Float = 1f, spacingRef: Float = 1f) : Int {
    var sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden)) {
        printerror("pas de child.");return 0}
    // 1. Setter largeur/hauteur
    var w = 0f
    var h = 0f
    var n = 0
    if (alignOpt and AlignOpt.vertically == 0) {
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
        if(alignOpt and AlignOpt.vertically == 0) {
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
    val fix = (alignOpt and AlignOpt.fixPos != 0)
    val setDef = (alignOpt and AlignOpt.dontSetAsDef == 0)
    if (alignOpt and AlignOpt.dontUpdateSizes == 0) {
        width.setPos(w, fix, setDef)
        height.setPos(h, fix, setDef)
    }
    // 4. Aligner les éléments
    sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden)) {
        printerror("pas de child2.");return 0}
    if(alignOpt and AlignOpt.vertically == 0) {
        var x = - w / 2f
        do {
            x += sq.pos.deltaX * spacingRef + spacing/2f

            sq.pos.x.setPos(x, fix, setDef)
            if(setDef)
                sq.pos.y.setPos(0f, fix, setDef)
            else
                sq.pos.y.setToDef()

            x += sq.pos.deltaX * spacingRef + spacing/2f
        } while (sq.goRightWithout(Flag1.hidden))
        return n
    }

    var y =  h / 2f
    do {
        y -= sq.pos.deltaY * spacingRef + spacing/2f

        if(setDef)
            sq.pos.x.setPos(0f, fix, setDef)
        else
            sq.pos.x.setToDef()
        sq.pos.y.setPos(y, fix, setDef)

        y -= sq.pos.deltaY * spacingRef + spacing/2f
    } while (sq.goRightWithout(Flag1.hidden))

    return n
}

object AlignOpt {
    const val vertically = 1
    const val dontUpdateSizes = 2
    const val respectRatio = 4
    const val fixPos = 8
    const val dontSetAsDef = 16
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
    width.setPos(w)
    height.setPos(h)
}
