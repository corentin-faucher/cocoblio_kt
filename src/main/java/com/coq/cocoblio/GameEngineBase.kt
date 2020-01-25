@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import android.content.Context
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

/** Utile ? abstract ? interface ? */
abstract class GameEngineBase(val ctx: Context) {
    val root: Node = Node(null, 0f, 0f, 4f, 4f, 0f,
        Flag1.exposed or Flag1.show or Flag1.branchToDisplay or Flag1.selectableRoot)
    var activeScreenOpt: ScreenBase? = null
        protected set
    var selectedNodeOpt: Node? = null
        protected set
    var changeScreenSoundID: Int? = null

    /*-- Fonctions à définir pour un projet particulier --*/
    abstract fun everyFrameAction()
    abstract fun initTouchDrag(posInit: Vector2) // touchDrag est exécuté tout de suite après.
    abstract fun touchDrag(posNow: Vector2)
    abstract fun letTouchDrag(vit: Vector2?)
    abstract fun singleTap(pos: Vector2)
    abstract fun keyDown(key: KeyboardKey)
    abstract fun keyUp(key: KeyboardKey)
    abstract fun configurationChanged()
    abstract fun appPaused()
    abstract fun extraCheckNodeAtOpening(node: Node)
    abstract fun extraCheckNodeAtClosing(node: Node)

    /*-- Fonctions avec implémentation rudimentaire. --*/
    open fun reshapeAction() {
        activeScreenOpt?.reshape(false)
    }


    /** Ne fait rien, si on est déjà au bon endroit. */
    fun changeActiveScreen(newScreen: ScreenBase?) {
        // 0. Cas "réouverture" de l'écran -> Reouvre sans extra.
        if(activeScreenOpt == newScreen) {
            newScreen?.closeBranch()
            newScreen?.openBranch()
            return
        }
        // 1. Si besoin, fermer l'écran actif.
        activeScreenOpt?.closeBranch(::extraCheckNodeAtClosing)

        // 2. Si null -> fermeture de l'app.
        if (newScreen == null) {
            activeScreenOpt = null
            print("newScreen == null -> exit")
            Timer(true).schedule(1000) {
                exitProcess(0)
            }
            return
        }

        // 3. Ouverture du nouvel écran.
        activeScreenOpt = newScreen
        newScreen.openBranch(::extraCheckNodeAtOpening)
        changeScreenSoundID?.let{SoundManager.play(it, 0, 0.5f)}
    }

/* OBSOLETE

    open fun checkScreenForOpening(screen: Screen) {}
    open fun checkScreenForClosing(screen: Screen) {}
    open class CheckNodeForOpening: ((Node) -> Unit) {
        override fun invoke(node: Node) {}
    }
    open class CheckNodeForClosing: ((Node) -> Unit) {
        override fun invoke(node: Node) {}
    }
    open class CheckScreenForOpening: ((Screen) -> Unit) {
        override fun invoke(screen: Screen) {}
    }
    open class CheckScreenForClosing: ((Screen) -> Unit) {
        override fun invoke(screen: Screen) {}
    }
    protected var checkNodeForOpening = CheckNodeForOpening()
    protected var checkNodeForClosing = CheckNodeForClosing()
    protected var checkScreenForOpening = CheckScreenForOpening()
    protected var checkScreenForClosing = CheckScreenForClosing()
    */
}