package com.coq.cocoblio.nodes

import com.coq.cocoblio.Language
import com.coq.cocoblio.Mesh
import com.coq.cocoblio.maths.SmTrans
import com.coq.cocoblio.Texture
import kotlin.math.min

/** Un noeud "surface". Noeud qui est affiché. Possède une texture (image png par exemple)
 * et une mesh (sprite par défaut). */
open class Surface : Node {
    var tex: Texture
    val mesh: Mesh
    val trShow: SmTrans

    /** Init comme une surface ordinaire (png) avec resource id. */
    constructor(refNode: Node?, pngResID: Int,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                i: Int = 0, flags: Long = 0,
                asParent: Boolean = true, asElderBigbro: Boolean = false,
                mesh: Mesh = Mesh.defaultSprite
    ) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigbro) {
        this.tex = Texture.getPngTex(pngResID)
        this.mesh = mesh
        trShow = SmTrans()

        updateTile(i, 0)
        updateRatio()
    }
    /** Init comme une surface ordinaire avec texture directement. */
    protected constructor(refNode: Node?, tex: Texture,
                          x: Float, y: Float, height: Float, lambda: Float = 0f,
                          i: Int = 0, flags: Long = 0, ceiledWidth: Float? = null,
                          asParent: Boolean = true, asElderBigbro: Boolean = false,
                          mesh: Mesh = Mesh.defaultSprite
    ) : super(refNode, x, y, ceiledWidth ?: height, height, lambda, flags, asParent, asElderBigbro) {
        this.tex = tex
        this.mesh = mesh
        trShow = SmTrans()
        ceiledWidth?.let {
            addFlags(Flag1.surfaceWithCeiledWidth)
        }

        updateTile(i, 0)
        updateRatio()
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

    fun updateForTexResID(newTexResID: Int) {
        this.tex = Texture.getPngTex(newTexResID)
        updateRatio()
    }
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
    /** S'il n'y a pas le flag surfaceDontRespectRatio, la largeur est ajustée.
     * Sinon, on ne fait que vérifier le frame voisin
     * et le parent. */
    protected fun updateRatio() {
        if (!containsAFlag(Flag1.surfaceDontRespectRatio)) {
            if (containsAFlag(Flag1.surfaceWithCeiledWidth)) {
                width.setPos(
                    min(height.realPos * tex.ratio, width.defPos),
                    true, false)
            } else {
                width.setPos(height.realPos * tex.ratio, true, true)
            }
        }
        (bigBro as? Frame)?.let { frame ->
            if (containsAFlag(Flag1.giveSizesToBigBroFrame))
                frame.update(width.realPos, height.realPos, true)
        }
        parent?.let{ parent ->
            if (containsAFlag(Flag1.giveSizesToParent)) {
                parent.width.setPos(width.realPos)
                parent.height.setPos(height.realPos)
            }
        }
    }
}

/** LanguageSurface : cas particulier de Surface. La tile est fonction de la langue. */
open class LanguageSurface : Surface, OpenableNode {
    override fun open() {
        updateTile(Language.currentLanguageID, 0)
    }

    /** Init comme une surface ordinaire (png) avec resource id. */
    constructor(refNode: Node?, texResID: Int,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0,
                asParent: Boolean = true, asElderBigbro: Boolean = false,
                mesh: Mesh = Mesh.defaultSprite
    ) : super(refNode, texResID, x, y, height, lambda,
        Language.currentLanguageID, flags, asParent, asElderBigbro, mesh)

    /** Constructeur de copie. */
    constructor(refNode: Node?, toCloneNode: LanguageSurface,
                asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = LanguageSurface(refNode, this, asParent, asElderBigbro)
}

/** Surface d'une string constante. (non localisée, définie "on the fly".) */
class CstStrSurf: Surface {
    constructor(refNode: Node?, string: String,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, ceiledWidth: Float? = null, asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, Texture.getConstantStringTex(string),
        x, y, height,  lambda, 0, flags, ceiledWidth, asParent, asElderBigbro) {
        piu.color = floatArrayOf(0f, 0f, 0f, 1f)  // (Text noir par défaut.)
    }

    /** Changement pour une autre string constante. */
    fun updateForCstStr(newString: String) {
        this.tex = Texture.getConstantStringTex(newString)
        updateRatio()
    }

    override  fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = CstStrSurf(refNode, this, asParent, asElderBigbro)
    private constructor(refNode: Node?, toCloneNode: CstStrSurf,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
}

/** Surface d'une string localisable.
 * (ne garde en mémoire ni la string ni la locStrID) */
class LocStrSurf : Surface, OpenableNode {
    override fun open() {
        updateRatio()
    }
    constructor(refNode: Node, resStrID: Int,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, ceiledWidth: Float? = null, asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, Texture.getLocalizedStringTex(resStrID),
        x, y, height, lambda, 0, flags, ceiledWidth, asParent, asElderBigbro) {
        piu.color = floatArrayOf(0f, 0f, 0f, 1f)
    }
    /** Changement d'une string localisée. */
    fun updateForLocStr(resStrID: Int) {
        this.tex = Texture.getLocalizedStringTex(resStrID)
        updateRatio()
    }
    override  fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = LocStrSurf(refNode, this, asParent, asElderBigbro)
    private constructor(refNode: Node?, toCloneNode: LocStrSurf,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
}

/** Surface d'une string editable. */
class EdtStrSurf : Surface, OpenableNode {
    override fun open() {
        updateRatio()
    }
    constructor(refNode: Node, id: Int,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, ceiledWidth: Float? = null, asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, Texture.getEditableStringTex(id),
        x, y, height, lambda, 0, flags, ceiledWidth, asParent, asElderBigbro) {
        piu.color = floatArrayOf(0f, 0f, 0f, 1f)
    }
    /** Changement pour une autre string editable */
    fun updateForEdtStr(id: Int) {
        this.tex = Texture.getEditableStringTex(id)
        updateRatio()
    }
    fun update() {
        updateRatio()
    }

    constructor(refNode: Node, id: Int, string: String,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, ceiledWidth: Float? = null, asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : this(refNode, id, x, y, height, lambda, flags, ceiledWidth, asParent, asElderBigbro) {
        piu.color = floatArrayOf(0f, 0f, 0f, 1f)
        Texture.setEditableString(id, string)
        updateRatio()
    }

    override  fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = EdtStrSurf(refNode, this, asParent, asElderBigbro)
    private constructor(refNode: Node?, toCloneNode: EdtStrSurf,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
}
