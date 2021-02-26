@file:Suppress("EnumEntryName", "JoinDeclarationAndAssignment", "ConvertSecondaryConstructorToPrimary", "unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.nodes

import android.opengl.GLES20
import com.coq.cocoblio.divers.printerror
import com.coq.cocoblio.graphs.Mesh
import com.coq.cocoblio.graphs.Texture
import kotlin.math.max

enum class Framing {
    outside,
    center,
    inside
}

class Bar : Surface {
    private val framing: Framing
    private val delta: Float

    constructor(parent: Node, framing: Framing, delta: Float, width: Float,
                pngResId: Int, lambda: Float = 0f
    ) : super(parent, Texture.getPng(pngResId), 0f, 0f, delta * 2f,
        lambda, Flag1.surfaceDontRespectRatio,
        Mesh(floatArrayOf(
            -0.5000f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.0f,
            -0.5000f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 1.0f,
            -0.1667f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.0f,
            -0.1667f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 1.0f,
             0.1667f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.0f,
             0.1667f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 1.0f,
             0.5000f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.0f,
             0.5000f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 1.0f),
            null, GLES20.GL_TRIANGLE_STRIP))
    {
        this.framing = framing
        this.delta = delta
        this.width.set(delta * 4f)
        update(width, true)
    }
    /** Copie... */
    private constructor(refNode: Node?, toCloneNode: Bar,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro) {
        framing = toCloneNode.framing
        delta = toCloneNode.delta
        mesh = Mesh(toCloneNode.mesh)
    }
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = Bar(refNode, this, asParent, asElderBigbro)

    fun update(width: Float, fix: Boolean) {
        if (width < 0) {
            printerror("deltaX < 0")
            return
        }
        val smallDeltaX: Float = when(framing) {
            Framing.outside -> max(0f, width/2f - 2f * delta)
            Framing.center -> max(0f, width/2f - delta)
            Framing.inside -> width/2f
        }
        val xPos = 0.5f * smallDeltaX / (smallDeltaX + 2f * delta)
        this.width.set(2f * (smallDeltaX + 2f * delta), fix)

        mesh.setXofVertex(-xPos, 2)
        mesh.setXofVertex(-xPos, 3)
        mesh.setXofVertex(xPos, 4)
        mesh.setXofVertex(xPos, 5)

        mesh.updateVerticesBuffer()
    }

    fun updateWithLittleBro(fix: Boolean) {
        littleBro?.let { bro ->
            x.set(bro.x.realPos, fix)
            y.set(bro.y.realPos, fix)
            update(bro.deltaX * 2f, fix)
        }
    }
}

class Frame : Surface {
    private val framing: Framing
    private val delta: Float
    constructor(parent: Node, framing: Framing, delta: Float,
                lambda: Float = 0f, pngResId: Int,
                width: Float, height: Float, flags: Long
    ) : super(parent, Texture.getPng(pngResId), 0f, 0f, delta * 2f,
            lambda, Flag1.surfaceDontRespectRatio or flags,
            Mesh(floatArrayOf(
                    -0.5000f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.000f, // 0
                    -0.5000f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.333f, // 1
                    -0.5000f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 0.667f, // 2
                    -0.5000f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.000f, 1.000f, // 3
                    -0.1667f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.000f, // 4
                    -0.1667f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.333f, // 5
                    -0.1667f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 0.667f, // 6
                    -0.1667f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.333f, 1.000f,
                     0.1667f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.000f,
                     0.1667f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.333f,
                     0.1667f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 0.667f,
                     0.1667f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 0.667f, 1.000f,
                     0.5000f,  0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.000f,
                     0.5000f,  0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.333f,
                     0.5000f, -0.1667f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 0.667f,
                     0.5000f, -0.5000f, 0.0f, 0.0f, 0.0f, 1.0f, 1.000f, 1.000f),
                 intArrayOf(
                     0, 1, 4,  1, 5, 4,
                     1, 2, 5,  2, 6, 5,
                     2, 3, 6,  3, 7, 6,
                     4, 5, 8,  5, 9, 8,
                     5, 6, 9,  6, 10, 9,
                     6, 7, 10, 7, 11, 10,
                     8, 9, 12, 9, 13, 12,
                     9, 10, 13, 10, 14, 13,
                     10, 11, 14, 11, 15, 14
                 ), GLES20.GL_TRIANGLES))
    {
        this.framing = framing
        this.delta = delta
        this.width.set(delta * 4f)
        update(width, height, true)
    }
    /** Copie... */
    private constructor(refNode: Node?, toCloneNode: Frame,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro) {
        framing = toCloneNode.framing
        delta = toCloneNode.delta
        mesh = Mesh(toCloneNode.mesh)
    }
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = Frame(refNode, this, asParent, asElderBigbro)

    fun update(width: Float, height: Float, fix: Boolean) {
        if (width < 0 || height < 0) {
            printerror("width or height < 0")
            return
        }
        val smallDeltaX: Float = when(framing) {
            Framing.outside -> max(0f, width/2f - 2f * delta)
            Framing.center -> max(0f, width/2f - delta)
            Framing.inside -> width/2f
        }
        val smallDeltaY: Float = when(framing) {
            Framing.outside -> max(0f, height/2f - 2f * delta)
            Framing.center -> max(0f, height/2f - delta)
            Framing.inside -> height/2f
        }
        val xPos = 0.5f * smallDeltaX / (smallDeltaX + 2f * delta)
        val yPos = 0.5f * smallDeltaY / (smallDeltaY + 2f * delta)
        this.width.set(2f * (smallDeltaX + 2f * delta), fix)
        this.height.set(2f * (smallDeltaY + 2f * delta), fix)

        mesh.setXofVertex(-xPos, 4)
        mesh.setXofVertex(-xPos, 5)
        mesh.setXofVertex(-xPos, 6)
        mesh.setXofVertex(-xPos, 7)
        mesh.setXofVertex( xPos, 8)
        mesh.setXofVertex( xPos, 9)
        mesh.setXofVertex( xPos, 10)
        mesh.setXofVertex( xPos, 11)

        mesh.setYofVertex( yPos, 1)
        mesh.setYofVertex( yPos, 5)
        mesh.setYofVertex( yPos, 9)
        mesh.setYofVertex( yPos, 13)
        mesh.setYofVertex(-yPos, 2)
        mesh.setYofVertex(-yPos, 6)
        mesh.setYofVertex(-yPos, 10)
        mesh.setYofVertex(-yPos, 14)

        mesh.updateVerticesBuffer()

        if(containsAFlag(Flag1.giveSizesToParent)) parent?.let {
            it.width.set(this.width.realPos)
            it.height.set(this.height.realPos)
        }
    }

    fun updateWithLittleBro(fix: Boolean) {
        littleBro?.let { bro ->
            x.set(bro.x.realPos, fix)
            y.set(bro.y.realPos, fix)
            update(bro.deltaX * 2f, bro.deltaY * 2f, fix)
        }
    }
}
