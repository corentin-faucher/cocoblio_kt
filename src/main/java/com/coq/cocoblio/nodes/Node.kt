@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.*
import com.coq.cocoblio.maths.SmPos
import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.maths.printerror
import kotlin.math.*

/** Un noeud est la structure de base de l'app... */
open class Node {
    /*-- Données de bases --*/
    /** Flags : Les options sur le noeud. */
    private var flags: Long
    /** Retirer des flags au noeud. */
    fun removeFlags(toRemove: Long) {
        flags = flags and toRemove.inv()
    }
    /** Ajouter des flags au noeud. */
    fun addFlags(toAdd: Long) {
        flags = flags or toAdd
    }
    fun addRemoveFlags(toAdd: Long, toRemove: Long) {
        flags = (flags or toAdd) and toRemove.inv()
    }
    fun containsAFlag(flagsRef: Long) = (flags and flagsRef != 0L)
    fun isDisplayActive() = (this is Surface && trShow.isActive) ||
            containsAFlag(Flag1.show or Flag1.branchToDisplay)

    /** Positions, tailles, etc. */
    val x : SmPos
    val y : SmPos
    val z : SmPos
    val width  : SmPos
    val height : SmPos
    val scaleX : SmPos
    val scaleY : SmPos
    /** Demi espace occupé en x. (width * scaleX) / 2 */
    val deltaX: Float
        get() = width.realPos * scaleX.realPos / 2.0f
    /** Demi espace occupé en y. (height * scaleY) / 2 */
    val deltaY: Float
        get() = height.realPos * scaleY.realPos / 2.0f

    /** Données d'affichage. */
    val piu : CoqRenderer.PerInstanceUniforms

    // Liens
    var parent: Node? = null
    var firstChild: Node? = null
    var lastChild: Node? = null
    var littleBro: Node? = null
    var bigBro: Node? = null

    /*-- Positions absolue et relative du noeud. --*/
    /** Obtenir la position absolue d'un noeud. */
    @Suppress("ControlFlowWithEmptyBody")
    fun getAbsPos() : Vector2 {
        val sq = Squirrel(this)
        while (sq.goUpP()) {}
        return sq.v
    }
    /** La position obtenue est dans le référentiel du noeud présent,
     *  i.e. au niveau des node.children.
     * (Si node == nil -> retourne absPos tel quel,
     * cas où node est aNode.parent et parent peut être nul.) */
    @Suppress("ControlFlowWithEmptyBody")
    fun relativePosOf(absPos: Vector2) : Vector2 {
        val sq = Squirrel(this, Squirrel.RSI.Scales)
        while (sq.goUpPS()) {}
        // Maintenant, sq contient la position absolue de theNode.
        return sq.getRelPosOf(absPos)
    }
    fun relativeDeltaOf(absDelta: Vector2) : Vector2 {
        val sq = Squirrel(this, Squirrel.RSI.Scales)
        @Suppress("ControlFlowWithEmptyBody")
        while (sq.goUpPS()) {}
        return sq.getRelDeltaOf(absDelta)
    }

    /*-- Constructeurs... --*/
    /** Noeud "vide" et "seul" */
    constructor(parent: Node?) {
        flags = 0L
        x = SmPos(0f)
        y = SmPos(0f)
        z = SmPos(0f)
        width = SmPos(4f)
        height = SmPos(4f)
        scaleX = SmPos(1f)
        scaleY = SmPos(1f)
        piu = CoqRenderer.PerInstanceUniforms()
        // 2. Ajustement des références
        parent?.let {
            connectToParent(it, false)
        }
    }
    /** Constructeur standard. */
    constructor(refNode: Node?,
                x: Float, y: Float, width: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, asParent:Boolean = true, asElderBigbro: Boolean = false) {
        // 1. Set data...
        this.flags = flags
        this.x = SmPos(x, lambda)
        this.y = SmPos(y, lambda)
        this.z = SmPos(0f, lambda)
        this.width = SmPos(width, lambda)
        this.height = SmPos(height, lambda)
        scaleX = SmPos(1f, lambda)
        scaleY = SmPos(1f, lambda)
        piu = CoqRenderer.PerInstanceUniforms()
        // 2. Ajustement des références
        refNode?.let {
            if (asParent) {
                connectToParent(it, asElderBigbro)
            } else {
                connectToBro(it, asElderBigbro)
            }
        }
    }
    /** Création d'une copie. ** Façon plus simple ??? ** */
    constructor(refNode: Node?, toCloneNode: Node,
                asParent: Boolean = true, asElderBigbro: Boolean = false) {
        // 1. Données de base
        this.flags = toCloneNode.flags
        this.x = toCloneNode.x.clone()
        this.y = toCloneNode.y.clone()
        this.z = toCloneNode.z.clone()
        this.width = toCloneNode.width.clone()
        this.height = toCloneNode.height.clone()
        scaleX = toCloneNode.scaleX.clone()
        scaleY = toCloneNode.scaleY.clone()
        piu = CoqRenderer.PerInstanceUniforms(toCloneNode.piu)
        // 2. Ajustement des références
        refNode?.let {
            if (asParent) {
                connectToParent(it, asElderBigbro)
            } else {
                connectToBro(it, asElderBigbro)
            }
        }
    }
    open fun copy(refNode: Node?, asParent: Boolean = true, asElderBigbro: Boolean = false)
        = Node(refNode, this, asParent, asElderBigbro)

    /*-----------------------------*/
    /*-- Effacement (discconnect) ---*/
    /** Se retire de sa chaine de frère et met les optionals à nil.
     *  Sera effacé par l'ARC, si n'est pas référencié(swift) ou ramassé par le GC?(Kotlin) */
    fun disconnect() {
        // 1. Retrait
        bigBro?.let{it.littleBro = littleBro} ?: run{parent?.firstChild = littleBro}
        littleBro?.let{it.bigBro = bigBro} ?: run{parent?.lastChild = bigBro}
        // 2. Déconnexion
//        parent = null
//        littleBro = null
//        bigBro = null
    }
    /** Deconnexion d'un descendant, i.e. Effacement direct.
     *  Retourne "true" s'il y a un descendant a effacer. */
    fun disconnectChild(elder: Boolean) : Boolean {
        if(elder) { firstChild?.disconnect() ?: return false }
        else      { lastChild?.disconnect() ?: return false }
        return true
    }
    /** Deconnexion d'un frère, i.e. Effacement direct.
     *  Retourne "true" s'il y a un frère a effacer. */
    fun disconnectBro(big: Boolean) : Boolean {
        if(big) { bigBro?.disconnect() ?: return false }
        else    { littleBro?.disconnect() ?: return false }
        return true

    }

    /*-- Déplacements --*/
    /** Change un frère de place dans sa liste de frère. */
    fun moveWithinBrosTo(bro: Node, asBigBro: Boolean) {
        if (bro === this) {return}
        val parent = bro.parent ?: run{ printerror("Pas de parent."); return}
        if (parent !== this.parent) {
            printerror("Parent pas commun."); return}
        // Retrait
        bigBro?.let{it.littleBro = littleBro} ?: run{parent.firstChild = littleBro}
        littleBro?.let{it.bigBro = bigBro} ?: run{parent.lastChild = bigBro}

        if (asBigBro) {
            // Insertion
            littleBro = bro
            bigBro = bro.bigBro
            // Branchement
            littleBro?.bigBro = this
            bigBro?.littleBro = this
            if (bigBro == null) {
                parent.firstChild = this
            }
        } else {
            // Insertion
            littleBro = bro.littleBro
            bigBro = bro
            // Branchement
            littleBro?.bigBro = this
            bigBro?.littleBro = this
            if (littleBro == null) {
                parent.lastChild = this
            }
        }
    }
    fun moveAsElderOrCadet(asElder: Boolean) {
        // 0. Checks
        if(asElder and (bigBro == null))
            return
        if(!asElder and (littleBro == null))
            return
        val theParent = parent ?: run{ printerror("Pas de parent."); return}
        // 1. Retrait
        bigBro?.let{it.littleBro = littleBro} ?: run{parent?.firstChild = littleBro}
        littleBro?.let{it.bigBro = bigBro} ?: run{parent?.lastChild = bigBro}
        // 2. Insertion
        if (asElder) {
            bigBro = null
            littleBro = theParent.firstChild
            // Branchement
            littleBro?.bigBro = this
            theParent.firstChild = this
        } else { // Ajout à la fin de la chaine
            littleBro = null
            bigBro = theParent.lastChild
            // Branchement
            bigBro?.littleBro = this
            theParent.lastChild = this
        }
    }
    /** Change de noeud de place (et ajuste sa position relative). */
    fun moveToBro(bro: Node, asBigBro: Boolean) {
        val newParent = bro.parent ?: run { printerror("Bro sans parent."); return}
        setInReferentialOf(newParent)
        disconnect()
        connectToBro(bro, asBigBro)
    }
    /** Change de noeud de place (sans ajuster sa position relative). */
    fun simpleMoveToBro(bro: Node, asBigBro: Boolean) {
        disconnect()
        connectToBro(bro, asBigBro)
    }
    /** Change de noeud de place (et ajuste sa position relative). */
    fun moveToParent(newParent: Node, asElder: Boolean) {
        setInReferentialOf(newParent)
        disconnect()
        connectToParent(newParent, asElder)
    }
    /** Change de noeud de place (sans ajuster sa position relative). */
    fun simpleMoveToParent(newParent: Node, asElder: Boolean) {
        disconnect()
        connectToParent(newParent, asElder)
    }
    /** "Monte" un noeud au niveau du parent. Cas particulier (simplifier) de moveTo(...).
     *  Si c'est une feuille, on ajuste width/height, sinon, on ajuste les scales. */
    fun moveUp(asBigBro: Boolean) : Boolean {
        val theParent = parent ?: run {
            printerror("Pas de parent."); return false
        }
        disconnect()
        connectToBro(theParent, asBigBro)
        x.referentialUp(theParent.x.realPos, theParent.scaleX.realPos)
        y.referentialUp(theParent.y.realPos, theParent.scaleY.realPos)
        if (firstChild == null) {
            width.referentialUpAsDelta(theParent.scaleX.realPos)
            height.referentialUpAsDelta(theParent.scaleY.realPos)
        } else {
            scaleX.referentialUpAsDelta(theParent.scaleX.realPos)
            scaleY.referentialUpAsDelta(theParent.scaleY.realPos)
        }
        return true
    }
    /** "Descend" dans le référentiel d'un frère. Cas particulier (simplifier) de moveTo(...).
     *  Si c'est une feuille, on ajuste width/height, sinon, on ajuste les scales. */
    fun moveDownIn(bro: Node, asElder: Boolean) : Boolean {
        if (bro === this) {return false}
        val oldParent = bro.parent ?: run { printerror("Manque parent."); return false}
        if (oldParent !== this.parent) {
            printerror("Parent pas commun."); return false}
        disconnect()
        connectToParent(bro, asElder)

        x.referentialDown(bro.x.realPos, bro.scaleX.realPos)
        y.referentialDown(bro.y.realPos, bro.scaleY.realPos)

        if (firstChild == null) {
            width.referentialDownAsDelta(bro.scaleX.realPos)
            height.referentialDownAsDelta(bro.scaleY.realPos)
        } else {
            scaleX.referentialDownAsDelta(bro.scaleX.realPos)
            scaleY.referentialDownAsDelta(bro.scaleY.realPos)
        }
        return true
    }
    /** Échange de place avec "node". */
    fun permuteWith(node: Node) {
        val oldParent = parent ?: run { printerror("Manque le parent."); return}
        if (node.parent === null) {
            printerror("Manque parent 2."); return}

        if (oldParent.firstChild === this) { // Cas ainé...
            moveToBro(node, true)
            node.moveToParent(oldParent, true)
        } else {
            val theBigBro = bigBro ?: run { printerror("Pas de bigBro."); return}
            moveToBro(node, true)
            node.moveToBro(theBigBro, false)
        }
    }

    /*-- Private stuff... --*/
    /** Connect au parent. (Doit être fullyDeconnect -> optionals à nil.) */
    private fun connectToParent(parent: Node, asElder: Boolean) {
        // Dans tout les cas, on a le parent:
        this.parent = parent
        // Cas parent pas d'enfants
        if (parent.firstChild == null) {
            parent.firstChild = this
            parent.lastChild = this
            return
        }
        // Ajout au début
        if (asElder) {
            // Insertion
            this.littleBro = parent.firstChild
            // Branchement
            parent.firstChild?.bigBro = this
            parent.firstChild = this
        } else { // Ajout à la fin de la chaine
            // Insertion
            this.bigBro = parent.lastChild
            // Branchement
            parent.lastChild?.littleBro = this
            parent.lastChild = this
        }
    }
    private fun connectToBro(bro: Node, asBigBro: Boolean) {
        if (bro.parent == null) {println("Boucle sans parents")}
        parent = bro.parent
        if (asBigBro) {
            // Insertion
            littleBro = bro
            bigBro = bro.bigBro
            // Branchement
            bro.bigBro = this // littleBro.bigBro = this
            if (bigBro != null) {
                bigBro?.littleBro = this
            } else {
                parent?.firstChild = this
            }
        } else {
            // Insertion
            littleBro = bro.littleBro
            bigBro = bro
            // Branchement
            bro.littleBro = this // bigBro.littleBro = this
            if (littleBro != null) {
                littleBro?.bigBro = this
            } else {
                parent?.lastChild = this
            }
        }
    }
    /** Change le référentiel. Pour moveTo de node. */
    @Suppress("ControlFlowWithEmptyBody")
    private fun setInReferentialOf(node: Node) {
        val sqP = Squirrel(this, Squirrel.RSI.Ones)
        while (sqP.goUpPS()) {}
        val sqQ = Squirrel(node, Squirrel.RSI.Scales)
        while (sqQ.goUpPS()) {}

        x.newReferential(sqP.v.x, sqQ.v.x, sqP.sx, sqQ.sx)
        y.newReferential(sqP.v.y, sqQ.v.y, sqP.sy, sqQ.sy)

        if (firstChild != null) {
            scaleX.newReferentialAsDelta(sqP.sx, sqQ.sx)
            scaleY.newReferentialAsDelta(sqP.sy, sqQ.sy)
        } else {
            width.newReferentialAsDelta(sqP.sx, sqQ.sx)
            height.newReferentialAsDelta(sqP.sy, sqQ.sy)
        }
        String.CASE_INSENSITIVE_ORDER
    }

    companion object {
        var showFrame = false

    }
}


/*---------------------------------------*/
/*-- Les interfaces / protocoles. -------*/
/*---------------------------------------*/

/** KeyboardKey peut être lié à un noeud-bouton ou simplement un event du clavier. */
interface KeyboardKey {
    val scancode: Int
    val keycode: Int
    val keymod: Int
    val isVirtual: Boolean
}
/** Pour les noeuds "déplaçable".
 * 1. On prend le noeud : "grab",
 * 2. On le déplace : "drag",
 * 3. On le relâche : "letGo".
 * Retourne s'il y a une "action / event". */
interface DraggableNode {
    fun grab(posInit: Vector2) : Boolean
    fun drag(posNow: Vector2, ge: GameEngineBase) : Boolean
    fun letGo(speed: Vector2?) : Boolean
}
/** Pour les type de noeud devant être vérifié à l'ouverture. */
interface OpenableNode {
    fun open()
}
/** Pour les noeuds pouvant être "activés", i.e. les boutons. */
interface ActionableNode {
    fun action()
}

/*---------------------------------------*/
/*-- Les sous-classes importantes. ------*/
/*---------------------------------------*/

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


/** Classe de base des boutons.
 * Par défaut un bouton n'est qu'un carré sans surface.
 * Un bouton est sélectionnable. */
abstract class Button : Node, ActionableNode {
    constructor(refNode: Node?, x: Float, y: Float, height: Float,
                lambda: Float = 0f, flags: Long = 0
    ) : super(refNode, x, y, height, height, lambda,
        Flag1.selectable or flags) {
        addRootFlag(Flag1.selectableRoot)
    }
    constructor(refNode: Node?, toCloneNode: Button,
                asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro) {
        addRootFlag(Flag1.selectableRoot)
    }
}

/** Classe de base des boutons de type "on/off".
 * Contient déjà les sous-noeuds de surface d'une switch. */
@Suppress("LeakingThis")
abstract class SwitchButton(refNode: Node?, var isOn: Boolean,
                            x: Float, y: Float, height: Float, lambda: Float = 0f, flags: Long = 0
) : Button(refNode, x, y, height, lambda, flags), DraggableNode {
    private val back: Surface
    private val nub: Surface

    init {
        scaleX.realPos = height
        scaleY.realPos = height
        this.height.realPos = 1f
        width.realPos = 2f
        back = Surface( this, R.drawable.switch_back, 0f, 0f, 1f)
        nub = Surface(this, R.drawable.switch_front,
            if(isOn) 0.375f else -0.375f, 0f, 1f, 10f)
        setBackColor()
    }

    fun fix(isOn: Boolean) {
        this.isOn = isOn
        nub.x.realPos = if(isOn) 0.375f else -0.375f
        setBackColor()
    }

    override fun grab(posInit: Vector2) : Boolean {
        return false
    }
    /** Déplacement en cours du "nub", aura besoin de letGoNub.
     * newX doit être dans le ref. du SwitchButton.
     * Retourne true si l'état à changer (i.e. action requise ?) */
    override fun drag(posNow: Vector2, ge: GameEngineBase) : Boolean {
        // 1. Ajustement de la position du nub.
        nub.x.pos = min(max(posNow.x, -0.375f), 0.375f)
        // 2. Vérif si changement
        if(isOn != posNow.x > 0f) {
            isOn = posNow.x > 0f
            setBackColor()
            return true
        }
        return false
    }
    /** Ne fait que placer le nub comme il faut. (À faire après avoir dragué.) */
    override fun letGo(speed: Vector2?) : Boolean {
        nub.x.pos = if(isOn) 0.375f else -0.375f
        return false
    }

    /** Simple touche. Permute l'état présent (n'effectue pas l'"action") */
    fun justTapNub() {
        isOn = !isOn
        setBackColor()
        letGo(null)
    }

    private fun setBackColor() {
        if(isOn) {
            back.piu.color[0] = 0.2f; back.piu.color[1] = 1f; back.piu.color[2] = 0.5f
        } else {
            back.piu.color[0] = 1f; back.piu.color[1] = 0.3f; back.piu.color[2] = 0.1f
        }
    }
}

/*----------------------------------------------*/
/*-- Les flags de base pour l'état d'un noeud.--*/
/*----------------------------------------------*/

/** Les flags "de base" pour les noeuds. */
@Suppress("unused")
object Flag1 {
    const val show = 1L
    const val hidden = 2L  // N'apparait pas quand on "open"
    const val exposed = 4L // Ne disparait pas quand on "close"
    const val selectableRoot = 1L.shl(4)
    const val selectable = 1L.shl(5)
    /** Noeud qui apparaît en grossisant. */
    const val poping = 1L.shl(6)

    /*-- Pour les surfaces --*/
    /** La tile est l'id de la langue actuelle. */
    //const val languageSurface = 1L.shl(7)
    /** Par défaut on ajuste la largeur pour respecter les proportion d'une image. */
    const val surfaceDontRespectRatio = 1L.shl(8)
    const val surfaceWithCeiledWidth = 1L.shl(9)

    /*-- Pour les ajustement de height/width du parent ou du frame --*/
    const val giveSizesToBigBroFrame = 1L.shl(10)
    const val giveSizesToParent = 1L.shl(11)

    /** Ajustement du ratio width/height d'un parent en fonction d'un enfant.
     * Par exemple, bouton qui prend les proportions du frame après sa mise à jour. */
    //const val getChildSurfaceRatio = 1L.shl(10)

    /*-- Pour les screens --*/
    /** Lors du reshape, le screen réaligne les "blocs" (premiers descendants).
     * Par défaut on aligne, il faut préciser seulement si on ne veut PAS aligner. */
    const val dontAlignScreenElements = 1L.shl(12)

    /*-- Affichage de branche --*/
    /** Paur l'affichage. La branche a encore des descendant à afficher. */
    const val branchToDisplay = 1L.shl(13)

    /** Le premier flag pouvant être utilisé dans un projet spécifique. */
    const val firstCustomFlag = 1L.shl(14)
}




// GARBAGE
// Sous classes de strings superflu ???

/*
/** Init comme surface d'une string constante. */
constructor(refNode: Node?, string: String,
x: Float, y: Float, height: Float, lambda: Float = 0f,
flags: Long = 0, asParent: Boolean = true, asElderBigbro: Boolean = false
) : super(refNode, x, y, height, height,  lambda, flags, asParent, asElderBigbro) {
    tex = Texture.getConstantStringTex(string)
    mesh = Mesh.sprite
    trShow = SmTrans()

    updateRatio()
}

/** Init comme surface d'une string localisable. */
constructor(refNode: Node?, resLocStringID: Int, context: Context,
x: Float, y: Float, height: Float, lambda: Float = 0f,
flags: Long = 0, asParent: Boolean = true, asElderBigbro: Boolean = false
) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigbro) {
    tex = Texture.getLocalizedStringTex(resLocStringID, context)
    mesh = Mesh.sprite
    trShow = SmTrans()
    addFlags(Flag1.mutableStringSurface)

    updateRatio()
}

/** Init comme surface d'une string éditable. */
constructor(refNode: Node?, edtStringID: EdtStrID,
x: Float, y: Float, height: Float, lambda: Float = 0f,
flags: Long = 0, asParent: Boolean = true, asElderBigbro: Boolean = false
) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigbro) {
    tex = Texture.getEditableStringTex(edtStringID)
    mesh = Mesh.sprite
    trShow = SmTrans()
    addFlags(Flag1.mutableStringSurface)

    updateRatio()
}
/** Init comme surface d'une string éditable avec valeur initiale. */
constructor(refNode: Node?, edtStringID: EdtStrID, string: String,
x: Float, y: Float, height: Float, lambda: Float = 0f,
flags: Long = 0, asParent: Boolean = true, asElderBigbro: Boolean = false
) : super(refNode, x, y, height, height, lambda, flags, asParent, asElderBigbro) {
    Texture.setEditableString(edtStringID, string)
    tex = Texture.getEditableStringTex(edtStringID)
    mesh = Mesh.sprite
    trShow = SmTrans()
    addFlags(Flag1.mutableStringSurface)

    updateRatio()

}

/* Superflu en fait...
class SurfCharCst : Surface {
    constructor(refNode: Node?, char: Char,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                flags: Long = 0, asParent: Boolean = true, asElderBigbro: Boolean = false
    ) : super(refNode, Texture.getConstantCharTex(char),
        x, y, height,  lambda, 0, flags, asParent, asElderBigbro)

    override  fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean)
            = SurfCharCst(refNode, this, asParent, asElderBigbro)
    private constructor(refNode: Node?, toCloneNode: SurfCharCst,
                        asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro)
}
*/
* /** Ajout de deux surfaces simple à un noeud racine.
 * Sa hauteur devient 1 et ses scales deviennent sa hauteur. */
fun <T:Node> T.alsoAddTwoSurfs(backResID: Int, backTile: Int,
                               frontResID: Int, frontTile: Int, frontFlags: Long = 0) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this }
    addFlags(Flag1.getChildSurfaceRatio)
    scaleX.realPos = height.realPos
    scaleY.realPos = height.realPos
    height.realPos = 1f
    width.realPos = 1f

    Surface(this, backResID, 0f, 0f, 1f, 0f, backTile)
    Surface(this, frontResID, 0f, 0f, 1f, 0f, frontTile, frontFlags)
    return this
}
* /** Ajout d'une surface simple à un noeud racine.
 * Sa hauteur devient 1 et ses scales deviennent sa hauteur.
 * Template car retourne son propre type (et non le type général Node). */
fun <T:Node> T.alsoAddSurf(resTexID: Int, i: Int, flags: Long = 0) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this }
    addFlags(Flag1.getChildSurfaceRatio)
    scaleX.realPos = height.realPos
    scaleY.realPos = height.realPos
    height.realPos = 1f
    width.realPos = 1f

    Surface(this, resTexID, 0f, 0f, 1f, 0f, i, flags)
    return this
}
/** Ajout d'un frame et string à un noeud.
 * La hauteur devient 1 et ses scales deviennent sa hauteur. Delta est un pourcentage. */
fun <T:Node> T.alsoAddFrameAndStrCst(string: String, frameResID: Int = R.drawable.frame_mocha,
                                     delta: Float = 0.25f) : T {
    if (firstChild != null) { printerror("A déjà quelque chose."); return this}
    addFlags(Flag1.getChildSurfaceRatio)
    scaleX.realPos = height.realPos
    scaleY.realPos = height.realPos
    height.realPos = 1f
    width.realPos = 1f

    Frame(this, true, false, delta, 0f, frameResID)
    CstStrSurf(this, string, 0f, 0f, 1f)
    return this
}
*/

// fin de fichier
