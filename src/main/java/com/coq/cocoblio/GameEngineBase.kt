@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.nodes.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

interface EventsHandler {
    fun singleTap(pos: Vector2)
    fun initTouchDrag(posInit: Vector2) // touchDrag est exécuté tout de suite après.
    fun touchDrag(posNow: Vector2)
    fun letTouchDrag(vit: Vector2?)

    fun keyDown(key: KeyboardKey)
    fun keyUp(key: KeyboardKey)

    fun onDrawFrame()
    fun viewReshaped()
    fun configurationChanged()
    fun appPaused()
}


/** Le Game Engine contrôle les interactions entre les noeuds et gère les events.
 * (Structure + Gestion) */
abstract class GameEngineBase : EventsHandler {
    val root: Node = RootNode()
    var activeScreen: ScreenBase? = null
        protected set
    var selectedNode: Node? = null
        protected set
    var changeScreenSoundID: Int? = null

    override fun viewReshaped() {
        println("viewReshape de GameEngineBase.")
        root.reshapeBranch()
    }

    /** Ne fait rien, si on est déjà au bon endroit. */
    fun changeActiveScreen(newScreen: ScreenBase?) {
        // 0. Cas "réouverture" de l'écran. ** Utile, superflu ?? **
        if(activeScreen === newScreen) {
            newScreen?.closeBranch()
            newScreen?.openBranch()
            return
        }
        // 1. Si besoin, fermer l'écran actif.
        activeScreen?.closeBranch()

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
        newScreen.openBranch()
        changeScreenSoundID?.let{SoundManager.play(it, 0, 0.5f)}
    }

    private class RootNode : Node(null,
        0f, 0f, 4f, 4f, 0f,
        Flag1.exposed or Flag1.show or Flag1.branchToDisplay or
                Flag1.selectableRoot or Flag1.reshapableRoot
    ), Reshapable {
        override fun reshape(): Boolean {
            return true
        }
    }
}