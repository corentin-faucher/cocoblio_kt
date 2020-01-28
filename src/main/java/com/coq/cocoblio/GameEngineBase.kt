@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import android.content.Context
import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.nodes.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

/** Le Game Engine contrôle les interactions entre les noeuds et gère les events. */
abstract class GameEngineBase(val ctx: Context) {
    val root: Node = Node(null, 0f, 0f, 4f, 4f, 0f,
        Flag1.exposed or Flag1.show or Flag1.branchToDisplay or Flag1.selectableRoot)
    var activeScreen: ScreenBase? = null
        protected set
    var selectedNode: Node? = null
        protected set
    var changeScreenSoundID: Int? = null

    /*-- Gestion des events. À définir pour un projet particulier --*/
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
        activeScreen?.reshape(false)
    }

    /** Ne fait rien, si on est déjà au bon endroit. */
    fun changeActiveScreen(newScreen: ScreenBase?) {
        // 0. Cas "réouverture" de l'écran -> Reouvre sans extra.
        if(activeScreen === newScreen) {
            newScreen?.closeBranch()
            newScreen?.openBranch()
            return
        }
        // 1. Si besoin, fermer l'écran actif.
        activeScreen?.closeBranch(::extraCheckNodeAtClosing)

        // 2. Si null -> fermeture de l'app.
        if (newScreen == null) {
            activeScreen = null
            print("newScreen == null -> exit")
            Timer(true).schedule(1000) {
                exitProcess(0)
            }
            return
        }

        // 3. Ouverture du nouvel écran.
        activeScreen = newScreen
        newScreen.openBranch(::extraCheckNodeAtOpening)
        changeScreenSoundID?.let{SoundManager.play(it, 0, 0.5f)}
    }
}