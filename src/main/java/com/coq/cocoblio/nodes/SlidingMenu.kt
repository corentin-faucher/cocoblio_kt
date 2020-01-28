package com.coq.cocoblio.nodes

import com.coq.cocoblio.*
import com.coq.cocoblio.maths.SmPos
import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.maths.printerror
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.*

/** Menu déroulant: root->menu->(item1, item2,... )
 * Vide au départ, doit être rempli quand on veut l'afficher.
 * if(spacing < 1) -> Recouvrement, if(spacing > 1) -> espacement.
 * addNewItem : Typiquement un constructeur de noeud-bouton.
 * checkItem : Methode/ext de noeud pour mettre à jour les noeud-boutons.
 * getIndicesRangeAtOpening : exécuter à l'ouverture du sliding menu et retourne le range attendu des items.
 * getPosIndex : la position de l'indice où on est centré à l'ouverture. */
class SlidingMenu(refNode: Node, private val nDisplayed: Int,
                  x: Float, y: Float, width: Float, height: Float, private val spacing: Float,
                  val addNewItem: ((menu: Node, index: Int) -> Node),
                  val getIndicesRangeAtOpening: (() -> IntRange),
                  val getPosIndex: (() -> Int),
                  private val openExtraCheck: ((Node) -> Unit)?,
                  private val closeExtraCheck: ((Node) -> Unit)?
) : Node(refNode, x, y, width, height, 10f, Flag1.selectable), DraggableNode, OpenableNode {
    private var menuGrabPosY: Float? = null
    private var indicesRange: IntRange = IntRange.EMPTY
    private val menu: Node // Le menu qui "glisse" sur le noeud racine.
    private val vitY = SmPos(0f, 4f) // La vitesse lors du "fling"
    private val deltaT = Chrono() // Pour la distance parcourue
    private val flingChrono = Chrono() // Temps de "vol"
    /** Le déplacement maximal du menu en y. 0 si n <= nD. */
    private val menuDeltaYMax: Float
        get() = 0.5f * itemHeight * max(indicesRange.count() - nDisplayed, 0)
    private val itemHeight: Float
        get() = height.realPos / nDisplayed

    init {
        tryToAddFrame()
        menu = Node(this, 0f, 0f, width, height, 20f)
    }

    // Respect des "interfaces"
    override fun open() {
        fun placeToOpenPos() {
            val normalizedID = max(min(getPosIndex(), indicesRange.last), indicesRange.first) - indicesRange.first
            setMenuYpos(itemHeight * (normalizedID.toFloat() - 0.5f * (indicesRange.count()-1).toFloat()),
                snap = true, fix = true)
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
        for (i in indicesRange) {
            addNewItem(menu, i)
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
            sq.pos.scaleX.realPos = scale
            sq.pos.scaleY.realPos = scale
        } while (sq.goRight())

        // 4. Aligner les éléments et placer au bon endroit.
        menu.alignTheChildren(AlignOpt.vertically or AlignOpt.fixPos, 1f, spacing)
        placeToOpenPos()
        checkItemsVisibility(false)
    }

    override fun grab(posInit: Vector2) : Boolean {
        flingChrono.stop()
        deltaT.stop()
        menuGrabPosY = posInit.y - menu.y.realPos

        return false
    }
    /** Scrooling vertical de menu. (déplacement en cours, a besoin d'un letGoWith) */
    override fun drag(posNow: Vector2, ge: GameEngineBase) : Boolean {
        menuGrabPosY?.let {
            setMenuYpos(posNow.y - it, snap = false, fix = false)
        } ?: printerror("drag pas init.")

        checkItemsVisibility(true)
        return false
    }
    override fun letGo(speed: Vector2?) : Boolean {
        // 0. Cas stop. Lâche sans bouger.
        if(speed == null) {
            setMenuYpos(menu.y.realPos, snap = true, fix = false)
            checkItemsVisibility(true)
            return false
        }
        // 1. Cas on laisse en "fling" (checkItemVisibilty s'occupe de mettre à jour la position)
        vitY.setPos(speed.y/2f, fix = true, setDef = false)
        flingChrono.start()
        deltaT.start()

        checkFling()
        return false
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
            setMenuYpos(menu.y.realPos + deltaT.elsapsedSec * vitY.pos,
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
                    sq.pos.openBranch(openExtraCheck)
                }
            }
            if (!toShow && !sq.pos.containsAFlag(Flag1.hidden)) {
                sq.pos.addFlags(Flag1.hidden)
                if(openNode) {
                    sq.pos.closeBranch(closeExtraCheck)
                }
            }
        } while (sq.goRight())
    }

    /** Ajuste la position de menu et vérifie les contraintes (snap, max/min). */
    private fun setMenuYpos(yCandIn: Float, snap: Boolean, fix: Boolean) {
        val yCand = if (snap) { // Il faut "snapper" à une position.
            round((yCandIn - menuDeltaYMax)/itemHeight) * itemHeight + menuDeltaYMax
        } else { yCandIn }
        menu.y.setPos(max(min(yCand, menuDeltaYMax), -menuDeltaYMax), fix, setDef = false)
    }
}