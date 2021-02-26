@file:Suppress("ConvertSecondaryConstructorToPrimary")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.R
import com.coq.cocoblio.divers.Chrono
import com.coq.cocoblio.maths.SmoothPos
import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.divers.printerror
import com.coq.cocoblio.graphs.Texture
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.*

interface Scrollable {
    /** Scrolling with wheel. */
    fun scroll(up: Boolean)
    /** Scrolling with trackpad. */
    fun trackpadScrollBegan()
    fun trackpadScroll(deltaY: Float)
    fun trackpadScrollEnded()
}


/** Menu déroulant: root->menu->(item1, item2,... )
 * Vide au départ, doit être rempli quand on veut l'afficher.
 * if(spacing < 1) -> Recouvrement, if(spacing > 1) -> espacement.
 * addNewItem : Typiquement un constructeur de noeud-bouton.
 * checkItem : Methode/ext de noeud pour mettre à jour les noeud-boutons.
 * getIndicesRangeAtOpening : exécuter à l'ouverture du sliding menu et retourne le range attendu des items.
 * getPosIndex : la position de l'indice où on est centré à l'ouverture. */
class SlidingMenu(refNode: Node, private val nDisplayed: Int,
                  x: Float, y: Float, width: Float, height: Float, private val spacing: Float,
                  val addNewItem: ((menu: Node, index: Int) -> Unit),
                  val getIndicesRangeAtOpening: (() -> IntRange),
                  val getPosIndex: (() -> Int)
) : Node(refNode, x, y, width, height, 10f
), Draggable, Scrollable
{
    private var menuGrabPosY: Float? = null
    private var indicesRange: IntRange = IntRange.EMPTY
    private val menu: Node // Le menu qui "glisse" sur le noeud racine.
    private val scrollBar: SlidingMenuScrollBar
    private val vitY = SmoothPos(0f, 4f) // La vitesse lors du "fling"
    private var vitYm1: Float = 0f
    private val deltaT = Chrono() // Pour la distance parcourue
    private val flingChrono = Chrono() // Temps de "vol"
    /** Le déplacement maximal du menu en y. 0 si n <= nD. */
    private val menuDeltaYMax: Float
        get() = 0.5f * itemHeight * max(indicesRange.count() - nDisplayed, 0)
    private val itemHeight: Float
        get() = height.realPos / nDisplayed

    init {
        makeSelectable()
        tryToAddFrame()
        val scrollBarWidth = width * 0.025f
        menu = Node(this, 0f, 0f, width, height, 20f)
        scrollBar = SlidingMenuScrollBar(this, scrollBarWidth)
    }

    /*-- Scrollable --*/
    override fun scroll(up: Boolean) {
        setMenuYpos(menu.y.realPos + if(up) -itemHeight else itemHeight,
                snap = true, fix = false)
        checkItemsVisibility(true)
    }

    override fun trackpadScrollBegan() {
        flingChrono.stop()
        vitYm1 = 0f
        vitY.set(0f)
        deltaT.start()
    }

    override fun trackpadScroll(deltaY: Float) {
        val menuDeltaY = -0.015f * deltaY
        setMenuYpos(menu.y.realPos + menuDeltaY, snap = true, fix = false)
        checkItemsVisibility(true)
        if(deltaT.elapsedSec > 0f) {
            vitYm1 = vitY.realPos
            vitY.set(menuDeltaY / deltaT.elapsedSec)
        }
        deltaT.start()
    }

    override fun trackpadScrollEnded() {
        vitY.set((vitY.realPos + vitYm1)/2)
        if (abs(vitY.realPos) < 6f) {
            setMenuYpos(menu.y.realPos, snap = true, fix = false)
            return
        }
        flingChrono.start()
        deltaT.start()

        checkFling()
    }

    /*-- Draggable --*/
    override fun grab(posInit: Vector2) {
        flingChrono.stop()
        deltaT.stop()
        menuGrabPosY = posInit.y - menu.y.realPos
    }
    /** Scrooling vertical de menu. (déplacement en cours, a besoin d'un letGoWith) */
    override fun drag(posNow: Vector2) {
        menuGrabPosY?.let {
            setMenuYpos(posNow.y - it, snap = false, fix = false)
        } ?: printerror("drag pas init.")

        checkItemsVisibility(true)
    }
    override fun letGo(speed: Vector2?) {
        // 0. Cas stop. Lâche sans bouger.
        if(speed == null) {
            setMenuYpos(menu.y.realPos, snap = true, fix = false)
            checkItemsVisibility(true)
            return
        }
        // 1. Cas on laisse en "fling" (checkItemVisibilty s'occupe de mettre à jour la position)
        vitY.set(speed.y/2f, fix = true, setAsDef = false)
        flingChrono.start()
        deltaT.start()

        checkFling()
    }

    override fun justTap() {
        // (pass)
    }

    /*-- Override open node --*/
    override fun open() {
        fun placeToOpenPos() {
            val normalizedID = max(min(getPosIndex(), indicesRange.last), indicesRange.first) - indicesRange.first
            val y0 = itemHeight * normalizedID.toFloat() - menuDeltaYMax
            setMenuYpos(y0, snap = true, fix = true)
        }
        // Mettre tout de suite le flag "show".
        if(!menu.containsAFlag(Flag1.hidden))
            menu.addFlags(Flag1.show)
        // 0. Cas pas de changements pour le IntRange,
        flingChrono.stop()
        deltaT.stop()
        val newIndicesRange = getIndicesRangeAtOpening()
        if (indicesRange == newIndicesRange) {
            placeToOpenPos()
            checkItemsVisibility(false)
            return
        }
        // 1. Changement. Reset des noeuds s'il y en a...
        indicesRange = newIndicesRange
        while (menu.firstChild != null) {
            menu.disconnectChild(true)
        }
        // 2. Ajout des items avec lambda addingItem
        if (indicesRange.isEmpty()) {
            scrollBar.setNubHeightWithRelHeight(1f)
        } else {
            val heightRatio = nDisplayed.toFloat() / max(1f, indicesRange.count().toFloat())
            scrollBar.setNubHeightWithRelHeight(heightRatio)
            for (i in indicesRange) {
                addNewItem(menu, i)
            }
        }
        // 3. Normaliser les hauteurs pour avoir itemHeight
        val sq = Squirrel(menu)
        if (!sq.goDown()) {
            return
        }
        val smallItemHeight = itemHeight / spacing
        do {
            // Scaling -> taille attendu / taille actuelle
            val scale = smallItemHeight / sq.pos.height.realPos
            sq.pos.scaleX.set(scale)
            sq.pos.scaleY.set(scale)
        } while (sq.goRight())

        // 4. Aligner les éléments et placer au bon endroit.
        menu.alignTheChildren(AlignOpt.vertically or AlignOpt.fixPos, 1f, spacing)
        placeToOpenPos()
        checkItemsVisibility(false)
        // 5. Open "node"
        super.open()
    }

    private fun checkFling() {
        // 1. Mise à jour automatique de la position en y.
        if (flingChrono.elapsedMS > 100L) {
            vitY.pos = 0f // Ralentissement...
            // OK, on arrête le "fling" après une seconde...
            if (flingChrono.elapsedMS > 1000L) {
                flingChrono.stop()
                deltaT.stop()
                setMenuYpos(menu.y.realPos, snap = true, fix = false)
            }
        }
        if (deltaT.elapsedMS > 30L) {
            setMenuYpos(menu.y.realPos + deltaT.elapsedSec * vitY.pos,
                snap = false, fix = false)
            deltaT.start()
        }
        // 2. Vérifier la visibilité des éléments.
        checkItemsVisibility(true)
        // 3. Callback
        if (deltaT.isActive) {
            Timer().schedule(40L) {
                checkFling()
            }
        }
    }

    /** Mise à jour continuelle du menu déroulant... */
    private fun checkItemsVisibility(openNode: Boolean) {
        // 0. Sortir s'il n'y a rien.
        val sq = Squirrel(menu)
        if(!sq.goDown() || !menu.containsAFlag(Flag1.show)) {
            flingChrono.stop()
            deltaT.stop()
            return
        }
        // 1. Ajuster la visibilité des items
        val yActual = //if (deltaT.isActive) menu.y.pos
//        else
            menu.y.realPos // TODO: Toujours realPos ??
        do {
            val toShow = abs(yActual + sq.pos.y.realPos) < 0.5f * height.realPos

            if (toShow && sq.pos.containsAFlag(Flag1.hidden)) {
                sq.pos.removeFlags(Flag1.hidden)
                if(openNode) {
                    sq.pos.openBranch()
                }
            }
            if (!toShow && !sq.pos.containsAFlag(Flag1.hidden)) {
                sq.pos.addFlags(Flag1.hidden)
                if(openNode) {
                    sq.pos.closeBranch()
                }
            }
        } while (sq.goRight())
    }

    /** Ajuste la position de menu et vérifie les contraintes (snap, max/min). */
    private fun setMenuYpos(yCandIn: Float, snap: Boolean, fix: Boolean) {
        val yCand = if (snap) { // Il faut "snapper" à une position.
            round((yCandIn - menuDeltaYMax)/itemHeight) * itemHeight + menuDeltaYMax
        } else { yCandIn }
        menu.y.set(max(min(yCand, menuDeltaYMax), -menuDeltaYMax), fix, setAsDef = false)
    }


}

private class SlidingMenuScrollBar : Node {
    private val nub: Node
    private val nubTop: TiledSurface
    private val nubMid: TiledSurface
    private val nubBot: TiledSurface

    constructor(parent: SlidingMenu, width: Float
    ) : super(parent, parent.width.realPos/2f - width/2f, 0f, width, parent.height.realPos)
    {
        val parHeight = parent.height.realPos
        val backTex = Texture.getPng(R.drawable.scroll_bar_back)
        val frontTex = Texture.getPng(R.drawable.scroll_bar_front)

        // Back of scrollBar
        TiledSurface(this, backTex, 0f, parHeight/2f - width/2f, width)
        TiledSurface(this, backTex, 0f, 0f, width,
                0f, 1, Flag1.surfaceDontRespectRatio).also { midSec ->
            midSec.height.set(parHeight - 2f*width)
        }
        TiledSurface(this, backTex, 0f, -parHeight/2f + width/2f, width,
                0f, 2)

        // Nub (sliding)
        nub = Node(this, 0f, parHeight/4, width, width*3f, 30f)
        nubTop = TiledSurface(nub, frontTex, 0f, width, width)
        nubMid = TiledSurface(nub, frontTex, 0f, 0f, width, 0f, 1, Flag1.surfaceDontRespectRatio)
        nubBot = TiledSurface(nub, frontTex, 0f, -width, width, 0f, 2)
    }

    fun setNubHeightWithRelHeight(newRelHeight: Float) {
        if (newRelHeight >= 1 || newRelHeight <= 0) {
            addFlags(Flag1.hidden)
            closeBranch()
            return
        }
        removeFlags(Flag1.hidden)
        val w = width.realPos
        val heightTmp = height.realPos * newRelHeight
        val heightMid = max(0f, heightTmp - 2f * w)
        nub.height.set(heightMid * 2f * w)
        nubTop.y.set((heightMid + w)/2f)
        nubBot.y.set(-(heightMid + w)/2f)
        nubMid.height.set(heightMid)
    }

    /*
    fun setNubRelY(newRelY: Float) {
        val deltaY = (height.realPos - nub.height.realPos)/2f
        nub.y.pos = -newRelY * deltaY
    }
    */
}



