@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.nodes.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

/** Le Game Engine contrôle les interactions entre les noeuds et gère les events.
 * (Structure + Gestion) */
abstract class GameEngineBase  {
    val root: RootNode = RootNode()
    var activeScreen: ScreenBase? = null
        protected set
    var selectedNode: Node? = null
        protected set
    var changeScreenSoundID: Int? = null

    abstract fun singleTap(pos: Vector2)
    abstract fun initTouchDrag(posInit: Vector2) // touchDrag est exécuté tout de suite après.
    abstract fun touchDrag(posNow: Vector2)
    abstract fun letTouchDrag(vit: Vector2?)

    abstract fun keyDown(key: KeyboardKey)
    abstract fun keyUp(key: KeyboardKey)

    open fun willDrawFrame(fullWidth: Float, fullHeight: Float) {
        root.fullWidth = fullWidth
        root.fullHeight = fullHeight
    }
    open fun viewReshaped(usableWidth: Float, usableHeight: Float) {
        root.width.set(usableWidth)
        root.height.set(usableHeight)
        root.reshapeBranch()
    }
    abstract fun configurationChanged()
    abstract fun appPaused()

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
}