@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.R
import com.coq.cocoblio.divers.Language
import com.coq.cocoblio.divers.printdebug
import com.coq.cocoblio.graphs.Mesh
import com.coq.cocoblio.maths.SmTrans
import com.coq.cocoblio.graphs.Texture
import com.coq.cocoblio.divers.printerror
import com.coq.cocoblio.graphs.TextureType
import kotlin.math.min

/** Un noeud "surface". Noeud qui est affiché. Possède une texture (image png par exemple)
 * et une mesh (sprite par défaut). */
open class Surface : Node {
    var tex: Texture
    var mesh: Mesh
    val trShow: SmTrans

    /** Init comme une surface ordinaire avec texture directement. */
    protected constructor(refNode: Node?, tex: Texture,
                          x: Float, y: Float, height: Float,
                          lambda: Float = 0f, flags: Long = 0,
                          mesh: Mesh = Mesh.defaultSprite,
                          asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigbro) {
        this.tex = tex
        this.mesh = mesh
        trShow = SmTrans()
    }

    /** Constructeur de copie. */
    constructor(refNode: Node?, toCloneNode: Surface,
                asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, toCloneNode, asParent, asElderBigbro) {
        tex = toCloneNode.tex
        mesh = toCloneNode.mesh
        trShow = SmTrans(toCloneNode.trShow)
    }
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = Surface(refNode, this, asParent, asElderBigbro)

    /** S'il n'y a pas le flag surfaceDontRespectRatio, la largeur est ajustée.
     * Sinon, on ne fait que vérifier le frame voisin
     * et le parent. */
    fun updateRatio(fix: Boolean) {
        if (!containsAFlag(Flag1.surfaceDontRespectRatio)) {
            if (containsAFlag(Flag1.surfaceWithCeiledWidth)) {
                width.set(
                    min(height.realPos * tex.ratio, width.defPos),
                    fix = fix, setAsDef = false)
            } else {
                width.set(height.realPos * tex.ratio,
                    fix = fix, setAsDef = true)
            }
        }
        (bigBro as? Frame)?.let { frame ->
            if (containsAFlag(Flag1.giveSizesToBigBroFrame))
                frame.update(width.realPos, height.realPos, fix)
        }
        parent?.let{ parent ->
            if (containsAFlag(Flag1.giveSizesToParent)) {
                parent.width.set(width.realPos)
                parent.height.set(height.realPos)
            }
        }
    }

    override fun isDisplayActive() : Boolean {
        return trShow.isActive
    }
}

class StringSurface: Surface
{
    constructor(refNode: Node?, strTex: Texture,
                x: Float, y: Float, height: Float,
                lambda: Float, flags: Long = 0, ceiledWidth: Float? = null,
                asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, strTex, x, y, height, lambda, flags, Mesh.defaultSprite,
        asParent, asElderBigbro)
    {
        width.set(ceiledWidth ?: height)
        if(strTex.type == TextureType.Png) {
            printerror("Pas une texture de string.")
            tex = Texture.getConstantString("?")
        }
        if(ceiledWidth != null) {
            addFlags(Flag1.surfaceWithCeiledWidth)
        }
        piu.color = floatArrayOf(0f, 0f, 0f, 1f)  // (Text noir par défaut.)
    }
    /** "Convenience init" pour string constante */
    constructor(refNode: Node?, cstString: String,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0, ceiledWidth: Float? = null
    ) : this(refNode, Texture.getConstantString(cstString),
        x, y, height, lambda, flags, ceiledWidth)

    constructor(refNode: Node?, locResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0, ceiledWidth: Float? = null
    ) : this(refNode, Texture.getLocalizedString(locResId),
            x, y, height, lambda, flags, ceiledWidth)

    /** Copie... */
    private constructor(refNode: Node?, toCloneNode: StringSurface,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = StringSurface(refNode, this, asParent, asElderBigbro)

    override fun open() {
        updateRatio(true)
        super.open()
    }

    fun updateTexture(newTexture: Texture) {
        if (newTexture.type == TextureType.Png) {
            printerror("Not a string texture.")
            return
        }
        tex = newTexture
    }
    /** "Convenience function": Ne change pas la texture.
     *  Ne fait que mettre à jour la string de la texture. */
    fun updateAsMutableString(newString: String) {
        if(tex.type != TextureType.MutableString) {
            printerror("Not a mutable string texture.")
            return
        }
        tex.updateAsMutableString(newString)
    }
    /** "Convenience function": Remplace la texture actuel pour
     * une texture de string constant (non mutable). */
    fun updateTextureToConstantString(newString: String) {
        tex = Texture.getConstantString(newString)
    }
}

class TiledSurface: Surface {
    constructor(refNode: Node?, pngTex: Texture,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, i: Int = 0, flags: Long = 0,
                asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, pngTex,
            x, y, height, lambda, flags, Mesh.defaultSprite,
            asParent, asElderBigbro)
    {
        updateRatio(true)
        updateTile(i, 0)
    }
    /** Conveniance init */
    constructor(refNode: Node?, pngResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float = 0f, i: Int = 0, flags: Long = 0,
                asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : this(refNode, Texture.getPng(pngResId),
        x, y, height, lambda, i, flags, asParent, asElderBigbro)
    /** Copie... */
    private constructor(refNode: Node?, toCloneNode: TiledSurface,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = TiledSurface(refNode, this, asParent, asElderBigbro)

    /** Si i > m -> va sur les lignes suivantes. */
    fun updateTile(i: Int, j: Int) {
        piu.i = (i % tex.m).toFloat()
        piu.j = ((j + i / tex.m) % tex.n).toFloat()
    }
    /** Ne change que l'index "i" de la tile (ligne) */
    fun updateTileI(index: Int) {
        piu.i = (index % tex.m).toFloat()
    }
    /** Ne change que l'index "j" de la tile (colonne) */
    fun updateTileJ(index: Int) {
        piu.j = (index % tex.n).toFloat()
    }

    /** Ne change que la texture (pas de updateRatio). */
    fun updateTexture(newTexture: Texture) {
        if(newTexture.type != TextureType.Png) {
            printerror("Not a png texture.")
            return
        }
        tex = newTexture
    }
}



/** LanguageSurface : cas particulier de Surface. La tile est fonction de la langue. */
open class LanguageSurface : Surface {
    constructor(refNode: Node?, pngResId: Int,
                x: Float, y: Float, height: Float,
                lambda: Float, flags: Long = 0,
                asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, Texture.getPng(pngResId), x, y, height, lambda, flags, Mesh.defaultSprite,
        asParent, asElderBigbro)
    {
        updateRatio(true)
    }

    /** Copie... */
    private constructor(refNode: Node?, toCloneNode: LanguageSurface,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = LanguageSurface(refNode, this, asParent, asElderBigbro)

    override fun open() {
        super.open()
        val i = Language.currentLanguageID
        piu.i = (i % tex.m).toFloat()
        piu.j = ((i / tex.m) % tex.n).toFloat()
    }

    /** Ne change que la texture (pas de updateRatio). */
    fun updateTexture(newTexture: Texture) {
        if(newTexture.type != TextureType.Png) {
            printerror("Not a png texture.")
            return
        }
        tex = newTexture
    }
}

@Suppress("ConvertSecondaryConstructorToPrimary")
class TestFrame : Surface {
    constructor(refNode: Node) : super(refNode, Texture.getPng(R.drawable.test_frame),
        0f, 0f, refNode.height.realPos, 10f,
        Flag1.surfaceDontRespectRatio or Flag1.notToAlign)
    {
        width.set(refNode.width.realPos)
    }
    /** Copie... */
    private constructor(refNode: Node?, toCloneNode: TestFrame,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = TestFrame(refNode, this, asParent, asElderBigbro)

    override fun open() {
        parent?.let { theParent ->
            height.pos = theParent.height.realPos
            width.pos = theParent.width.realPos
        } ?: run {
            printerror("Pas de parent.")
        }
        super.open()
    }

    override fun reshape(): Boolean {
        open()
        return false
    }
}
