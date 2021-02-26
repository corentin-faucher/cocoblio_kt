@file:Suppress("unused")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.divers.printerror
import com.coq.cocoblio.graphs.Texture
import com.coq.cocoblio.graphs.TextureType
import com.coq.cocoblio.nodes.Node.Companion.showFrame
import kotlin.math.abs

/*---------------------------------------*/
/*-- Quelques extensions utiles. --------*/
/*---------------------------------------*/

fun Node.fillWithFramedString(strTex: Texture, pngResId: Int,
                              ceiledWidth: Float? = null, relDelta: Float = 0.2f
) : StringSurface?
{
    if (firstChild != null) { printerror("A déjà quelque chose."); return null}

    scaleX.set(height.realPos)
    scaleY.set(height.realPos)
    val scaleCeiledWidth = ceiledWidth?.let{it / height.realPos}
    Frame(this, Framing.inside, relDelta, 0f, pngResId,
            0f, 0f, Flag1.giveSizesToParent)
    return StringSurface(this, strTex, 0f, 0f, 1f, 0f,
        Flag1.giveSizesToBigBroFrame, scaleCeiledWidth).also { stringSurface ->
        stringSurface.updateRatio(true)
    }
}

/** Convenience version... */
fun Node.fillWithFramedString(locStrId: Int, pngResId: Int,
                              ceiledWidth: Float? = null, relDelta: Float = 0.2f
) : StringSurface?
{
    return fillWithFramedString(Texture.getLocalizedString(locStrId), pngResId,
            ceiledWidth, relDelta)
}

fun Node.addFramedString(strTex: Texture, pngResID: Int,
                         x: Float, y: Float, height: Float,
                         lambda: Float = 0f, flags: Long = 0L,
                         ceiledWidth: Float? = null, relDelta: Float = 0.2f
) : StringSurface?
{
    if (strTex.type == TextureType.Png) {
        printerror("Not a string texture."); return null
    }
    return Node(this, x, y, ceiledWidth ?: height, height,
        lambda, flags).fillWithFramedString(strTex, pngResID, ceiledWidth, relDelta)
}

fun Node.addFramedString(locStrId: Int, pngResID: Int,
                         x: Float, y: Float, height: Float,
                         lambda: Float = 0f, flags: Long = 0L,
                         ceiledWidth: Float? = null, relDelta: Float = 0.2f
) : StringSurface?
{
    return addFramedString(Texture.getLocalizedString(locStrId), pngResID, x, y, height,
        lambda, flags, ceiledWidth, relDelta)
}


fun Node.addFramedString(cstString: String, pngResID: Int,
                         x: Float, y: Float, height: Float,
                         lambda: Float = 0f, flags: Long = 0L,
                         ceiledWidth: Float? = null, relDelta: Float = 0.2f
) : StringSurface?
{
    return addFramedString(Texture.getConstantString(cstString), pngResID, x, y, height,
            lambda, flags, ceiledWidth, relDelta)
}

fun Node.addFramedTiledSurface(tiledTex: Texture, frameResId: Int,
                               x: Float, y: Float, height: Float, i: Int,
                               lambda: Float = 0f, flags: Long = 0L,
                               delta: Float = 0.4f
) : TiledSurface?
{
    if (tiledTex.type != TextureType.Png) {
        printerror("Not a png texture."); return null
    }
    return Node(this, x, y, height, height,
            lambda, flags).let { node ->
        Frame(node, Framing.inside, delta * height, 0f, frameResId,
                0f, 0f, Flag1.giveSizesToParent)
        TiledSurface(node, tiledTex, 0f, 0f, height,
                0f, i, Flag1.giveSizesToBigBroFrame)
    }
}
/** !Debug Option!
 * Ajout d'une surface "frame" pour visualiser la taille d'un "bloc".
 * L'option Node.showFrame doit être "true". */
fun Node.tryToAddFrame() {
    if (!showFrame) return
    TestFrame(this)
}

/*-- Ajustements de position/taille --*/

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

fun Node.setRelativelyToParent(fix: Boolean) {
    val theParent = parent ?: return
    var xDec = 0.0f
    var yDec = 0.0f
    if(containsAFlag(Flag1.relativeToRight))
        xDec = theParent.width.realPos * 0.5f
    else if(containsAFlag(Flag1.relativeToLeft))
        xDec = -theParent.width.realPos * 0.5f
    if(containsAFlag(Flag1.relativeToTop))
        yDec = theParent.height.realPos * 0.5f
    else if(containsAFlag(Flag1.relativeToBottom))
        yDec = -theParent.height.realPos * 0.5f
    x.setRelToDef(xDec, fix)
    y.setRelToDef(yDec, fix)
}

/** Aligner les descendants d'un noeud. Retourne le nombre de descendants traités. */
fun Node.alignTheChildren(alignOpt: Int, ratio: Float = 1f, spacingRef: Float = 1f) : Int {
    var sq = Squirrel(this)
    if (!sq.goDownWithout(Flag1.hidden or Flag1.notToAlign)) {
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
        } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))
    }
    else {
        do {
            h += sq.pos.deltaY * 2f * spacingRef
            n += 1
            if (sq.pos.deltaX*2f > w) {
                w = sq.pos.deltaX*2f
            }
        } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))
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
    if (!sq.goDownWithout(Flag1.hidden or Flag1.notToAlign)) {
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
        } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))
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
    } while (sq.goRightWithout(Flag1.hidden or Flag1.notToAlign))

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


