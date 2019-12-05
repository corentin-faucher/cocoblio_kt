package com.coq.cocoblio

import kotlin.math.max

/** Noeud racine servant de cadre à une surface.
 * (La surface étant placé en petit-frère.)
 * Les enfants de Frame sont 9 surfaces créant le cadre. */
class Frame : Node {
    private val delta: Float
    private val lambda: Float
    private val pngResID: Int
    private val isInside: Boolean

    constructor(refNode: Node?, isInside: Boolean = false,
                delta: Float = 0.1f, lambda: Float = 0f,
                framePngResID: Int = R.drawable.frame_mocha, flags: Long = 0L
    ) : super(refNode, 0f, 0f, delta, delta, 0f, flags) {
        this.delta = delta
        this.lambda = lambda
        this.pngResID = framePngResID
        this.isInside = isInside
    }
    /** Constructeur de copie. */
    constructor(refNode: Node?, toCloneNode: Frame,
                asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro) {
        delta = toCloneNode.delta
        lambda = toCloneNode.lambda
        pngResID = toCloneNode.pngResID
        isInside = toCloneNode.isInside
    }
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean) : Frame {
        return Frame(refNode, this, asParent, asElderBigbro)
    }

    /** Init ou met à jour un noeud frame
     * (Ajoute les descendants si besoin) */
    fun update(width: Float, height: Float, fix: Boolean) {
        val refSurf = Surface(null, pngResID, 0f, 0f, delta, lambda)

        val sq = Squirrel(this)
        val deltaX = if (isInside) max(width/2 - delta/2f, delta/2f) else width/2f + delta/2f
        val deltaY = if (isInside) max(height/2 - delta/2f, delta/2f) else height/2f + delta/2f
        val smallWidth = if(isInside) max(width - delta, 0f) else width
        val smallHeight = if(isInside) max(height - delta, 0f) else height

        // Mise à jour des dimensions.
        this.width.setPos(smallWidth + 2f * delta)
        this.height.setPos(smallHeight + 2f * delta)
        parent?.let{ parent ->
            if (containsAFlag(Flag1.giveSizesToParent)) {
                parent.width.setPos(this.width.realPos)
                parent.height.setPos(this.height.realPos)
            }
        }
        run {
            sq.goDownForced(refSurf) // tl
            (sq.pos as? Surface)?.updateTile(0, 0)
            sq.pos.x.setPos(-deltaX, fix, true)
            sq.pos.y.setPos(deltaY, fix, true)
            sq.goRightForced(refSurf) // t
            (sq.pos as? Surface)?.updateTile(1, 0)
            sq.pos.x.setPos(0f, fix, true)
            sq.pos.y.setPos(deltaY, fix, true)
            sq.pos.width.setPos(smallWidth, fix, true)
            sq.goRightForced(refSurf) // tr
            (sq.pos as? Surface)?.updateTile(2, 0)
            sq.pos.x.setPos(deltaX, fix, true)
            sq.pos.y.setPos(deltaY, fix, true)
            sq.goRightForced(refSurf) // l
            (sq.pos as? Surface)?.updateTile(3, 0)
            sq.pos.x.setPos(-deltaX, fix, true)
            sq.pos.y.setPos(0f, fix, true)
            sq.pos.height.setPos(smallHeight, fix, true)
            sq.goRightForced(refSurf) // c
            (sq.pos as? Surface)?.updateTile(4, 0)
            sq.pos.x.setPos(0f, fix, true)
            sq.pos.y.setPos(0f, fix, true)
            sq.pos.width.setPos(smallWidth, fix, true)
            sq.pos.height.setPos(smallHeight, fix, true)
            sq.goRightForced(refSurf) // r
            (sq.pos as? Surface)?.updateTile(5, 0)
            sq.pos.x.setPos(deltaX, fix, true)
            sq.pos.y.setPos(0f, fix, true)
            sq.pos.height.setPos(smallHeight, fix, true)
            sq.goRightForced(refSurf) // bl
            (sq.pos as? Surface)?.updateTile(6, 0)
            sq.pos.x.setPos(-deltaX, fix, true)
            sq.pos.y.setPos(-deltaY, fix, true)
            sq.goRightForced(refSurf) // b
            (sq.pos as? Surface)?.updateTile(7, 0)
            sq.pos.x.setPos(0f, fix, true)
            sq.pos.y.setPos(-deltaY, fix, true)
            sq.pos.width.setPos(smallWidth, fix, true)
            sq.goRightForced(refSurf) // br
            (sq.pos as? Surface)?.updateTile(8, 0)
            sq.pos.x.setPos(deltaX, fix, true)
            sq.pos.y.setPos(-deltaY, fix, true)
        }
    }
}