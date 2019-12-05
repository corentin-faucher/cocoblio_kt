@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import java.lang.Exception
import kotlin.math.*

class Squirrel(pos_: Node) {

    enum class RSI {Ones, Scales}

    /*-- Données de bases et computed properties --*/
    /** Position dans l'arbre (noeud) de l'écureuil. */
    var pos: Node
        private set
    private val root: Node
    var x: Float
        private set
    var y: Float
        private set
    val v: Vector2
        get() = Vector2(x, y)
    var sx: Float = 0.0f
        private set
    var sy: Float = 0.0f
        private set
    /** Vérifie si on tombe dans le cadre du noeud présent (pos). */
    val isIn: Boolean
        get() = (abs(x - pos.x.realPos) <= pos.deltaX) &&
                (abs(y - pos.y.realPos) <= pos.deltaY)
    /** Convertir une position en position relative par rapport
     *  a la position présente (et scaling présent) */
    fun getRelPosOf(pos: Vector2) : Vector2 =
        Vector2((pos.x - x) / sx, (pos.y - y) / sy)
    fun getRelDeltaOf(delta: Vector2) : Vector2 =
        Vector2(delta.x / sx, delta.y / sy)

    /*-- Constructeurs... --*/
    init {
        pos = pos_
        root = pos_
        x = pos.x.realPos;  y = pos.y.realPos
        sx = 1.0f;              sy = 1.0f
    }
    // Superflu, si on veut vraiment reinit c'est plus clair de prendre un var sq:Squirrel...
    /*fun reinit(pos: Node) {
        this.pos = pos
        this.root = pos
        x = pos.x.realPos;  y = pos.y.realPos
        sx = 1.0f;              sy = 1.0f
    }*/
    constructor(pos_: Node, scaleInit: RSI) : this(pos_) {
        when(scaleInit) {
            RSI.Ones -> {sx = 1.0f; sy = 1.0f}
            RSI.Scales -> {
                sx = pos.scaleX.realPos
                sy = pos.scaleY.realPos
            }
        }
    }
    /// Initialise avec une position relative au lieu de la position du noeud.
    /// La postion relative est dans le reférentiel de pos.parent (comme l'est la position du noeud).
    constructor(pos_: Node, relPos: Vector2,
                scaleInit: RSI
    ) : this(pos_) {
        x = relPos.x; y = relPos.y
        when(scaleInit) {
            RSI.Ones -> {sx = 1.0f; sy = 1.0f}
            RSI.Scales -> {
                sx = pos.scaleX.realPos
                sy = pos.scaleY.realPos
            }
        }
    }

    /*--- Déplacements ----*/
    /** Déconnecte où on est et va au petit (pas défaut) frère.
     * Si ne peut aller au frère, va au parent.
     * Throw une erreur si ne peut aller au parent. */
    fun disconnectAndGoToBroOrUp(little: Boolean = true) : Boolean {
        (if(little) pos.littleBro else pos.bigBro)?.let {
            val toDelete = pos
            pos = it
            toDelete.disconnect()
            return true
        }
        pos.parent?.let {
            val toDelete = pos
            pos = it
            toDelete.disconnect()
            return false
        }
        throw Exception("Ne peut deconnecter, nul part où aller.")
    }
    /** Va au petit-frère. S'arrête au cadet (et retourne false). */
    fun goRight() : Boolean {
        pos.littleBro?.let {
            pos = it; return true}
        return false
    }
    /** Va au petit-frère. S'il n'existe pas, on le crée. */
    fun goRightForced(copyRef: Node) {
        pos = pos.littleBro ?: copyRef.copy(pos, asParent = false, asElderBigbro = false)
    }
    /** Revient à l'ainé si arrive en bout de liste (retourne false si ne peut pas y aller, i.e. pas de parent). */
    fun goRightLoop() : Boolean {
        pos.littleBro?.let{pos = it; return true}
        pos.parent?.let{pos = it.firstChild!!; return true}
        return false
    }
    /** Tant que l'on est sur un noeud caché, on bouge vers la droite. Retourne false si abouti en bout de liste.
     * Se déplace au moins une fois. */
    fun goRightWithout(flag: Long) : Boolean {
        do {
            if(!goRight()) {return false}
        } while(pos.containsAFlag(flag))
        return true
    }
    /** Va au petit-frère non-caché et du type voulu. */
    inline fun <reified A:Node> goRightWithoutTyped(flag: Long) : Boolean {
        do {
            if(!goRight()) {return false}
        } while((pos !is A) or pos.containsAFlag(flag))
        return true
    }
    /** Va au grand-frère. S'arrête et retourne false si ne peut y aller (dépassé l'ainé) */
    fun goLeft() : Boolean {
        pos.bigBro?.let { pos = it; return true }
        return false
    }
    /** Va au grand-frère. S'il n'existe pas, on le crée. */
    fun goLeftForced(copyRef: Node) {
        pos = pos.bigBro ?: copyRef.copy(pos, asParent = false, asElderBigbro = true)
    }
    fun goLeftWithout(flag: Long) : Boolean {
        do {
            if(!goLeft()) {return false}
        } while(pos.containsAFlag(flag))
        return true
    }
    /** Va au firstChild. Retourne false si pas de descendants. */
    fun goDown() : Boolean {
        pos.firstChild?.let {pos = it; return true}
        return false
    }
    /** Va au firstChild. S'il n'existe pas on le crée. */
    fun goDownForced(copyRef: Node) {
        pos = pos.firstChild ?: copyRef.copy(pos, asParent = true, asElderBigbro = true)
    }
    /** Va au lastChild. Retourne false si pas de descendants. */
    fun goDownLast() : Boolean {
        pos.lastChild?.let { pos = it; return true }
        return false
    }
    fun goDownWithout(flag: Long) : Boolean {
        pos.firstChild?.let {
            pos = it
            while(pos.containsAFlag(flag)) {
                if(!goRight()) {return false}
            }
            return true
        }
        return false
    }
    fun goDownLastWithout(flag: Long) : Boolean {
        pos.lastChild?.let {
            pos = it
            while(pos.containsAFlag(flag)) {
                if(!goLeft()) {return false}
            }
            return true
        }
        return false
    }
    fun goDownP() : Boolean {
        pos.firstChild?.let {
            x = (x - pos.x.realPos) / pos.scaleX.realPos
            y = (y - pos.y.realPos) / pos.scaleY.realPos
            pos = it
            return true
        }
        return false
    }
    fun goDownPS() : Boolean {
        pos.firstChild?.let {
            x = (x - pos.x.realPos) / pos.scaleX.realPos
            y = (y - pos.y.realPos) / pos.scaleY.realPos
            sx /= pos.scaleX.realPos
            sy /= pos.scaleY.realPos
            pos = it
            return true
        }
        return false
    }
    fun goUp() : Boolean {
        pos.parent?.let { pos = it; return true }
        return false
    }
    fun goUpP() : Boolean {
        pos.parent?.let {
            pos = it
            x = x * pos.scaleX.realPos + pos.x.realPos
            y = y * pos.scaleY.realPos + pos.y.realPos
            return true
        }
        return false
    }
    fun goUpPS() : Boolean {
        pos.parent?.let {
            pos = it
            x = x * pos.scaleX.realPos + pos.x.realPos
            y = y * pos.scaleY.realPos + pos.y.realPos
            sx *= pos.scaleX.realPos
            sy *= pos.scaleY.realPos
            return true
        }
        return false
    }

    fun goToNextNode() : Boolean {
        if (goDown()) {return true}
        if (pos === root) {return false}
        while (!goRight()) {
            if (!goUp()) {
                printerror("Pas de root.")
                return false
            } else if (pos === root) {
                return false
            }
        }
        return true
    }

    /** Déplacement particulier pour l'affichage. */
    fun goToNextToDisplay() : Boolean {
        // 1. Aller en profondeur, pause si branche à afficher.
        if (pos.firstChild != null && pos.containsAFlag(Flag1.show or Flag1.branchToDisplay)) {
            pos.removeFlags(Flag1.branchToDisplay)
            goDown()
            return true
        }
        // 2. Redirection (voisin, parent).
        do {
            // Si le noeud présent est encore actif -> le parent doit l'être aussi.
            if (pos.isDisplayActive()) {
                pos.parent?.addFlags(Flag1.branchToDisplay)
            }
            if (goRight()) {return true}
        } while (goUp())
        return false
    }
}

