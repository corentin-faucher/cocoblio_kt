@file:Suppress("JoinDeclarationAndAssignment", "ConvertSecondaryConstructorToPrimary", "unused", "MemberVisibilityCanBePrivate", "PropertyName")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.divers.printerror
import com.coq.cocoblio.divers.printwarning
import com.coq.cocoblio.maths.*
import java.lang.Float.min
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess


/** Noeud racine de la structure affichable.
 * width et height sont ajuster par le renderer pour correspondre à la région utilisable
 * de l'écran (sans les bord).
 * Le renderer met à jour à chaque frame les vrai dimension de la vue dans fullWidth
 * et fullHeight. */
@Suppress("ConvertSecondaryConstructorToPrimary")
open class RootNode : Node {
    val camera: Camera
    // Un root node gère le framing de ses descendants
    // width et height sont le "usable frame".
    private var frameWidthPx: Float = 100f
    var frameWidth: Float = 2f
        private set
    private var frameHeightPx: Float = 100f
    var frameHeight: Float = 2f
        private set
    private var yShift: Float = 0f
    private val parentRoot: RootNode?

    // Une root peut, par ex., être descendant d'un screen et avoir un grand-par. RootNode.
    constructor(parent: Node?, parentRoot: RootNode?
    ) : super(parent, 0f, 0f, 4f, 4f, 10f,
        Flag1.exposed or Flag1.show or Flag1.branchToDisplay or
                Flag1.selectableRoot or Flag1.isRoot
    ) {
        if (parent != null) {
            if (parentRoot == null) {
                printerror("Pas de parent root et n'est pas la root absolue.")
            }
            this.parentRoot = parentRoot
        } else {
            if (parentRoot != null) {
                printwarning("Pas besoin de parent root si est la root absolue")
            }
            this.parentRoot = null
        }
        camera = Camera(0f, 0f, 4f)
    }

    fun setModelMatrix() {
        piu.model = camera.getLookAtModelMatrix(yShift)
    }

    fun getProjectionMatrix() : FloatArray {
        return getPerspective(0.1f, 50f, camera.z.pos,
            frameWidth, frameHeight)
    }

    fun updateFrameSize(newWidth: Int, newHeight: Int,
        marginTop: Float, left: Float, bottom: Float, right: Float)
    {
        frameWidthPx = newWidth.toFloat()
        frameHeightPx = newHeight.toFloat()
        val frameRatio = frameWidthPx / frameHeightPx
        val ratioT = marginTop / frameHeightPx + 0.005f // La marge en haut.
        val ratioB = bottom / frameHeightPx + 0.005f // La marge en bas.
        val ratioLR = (left + right) / frameWidthPx + 0.01f // La marge gauche/droite.
        // 1. Full Frame
        if (newWidth > newHeight) { // Landscape
            frameHeight = 2f / (1f - ratioT - ratioB)
            frameWidth = frameRatio * frameHeight
        } else { // Potrait
            frameWidth = 2f / (1f - ratioLR)
            frameHeight = frameWidth / frameRatio
        }
        // 2. Usable Frame
        if (newWidth > newHeight) { // Landscape
            width.set(min((1f - ratioLR) * frameWidth, 2f * ratioMax))
            height.set(2f)
        } else { // Potrait
            width.set(2f)
            height.set(min((1f - ratioT - ratioB) * frameHeight, 2f / ratioMin))
        }
        // 3. Camera center en fonction des marges
        yShift = (ratioT - ratioB) * frameHeight / 2f
        // 4. Reshape la structure...
        reshapeBranch()
    }
    // (Axe des y inversé : facteur -1 pour les y.)
    fun getPositionFrom(locationInWindowX: Float, locationInWindowY: Float)
        = Vector2((locationInWindowX / frameWidthPx - 0.5f) * frameWidth,
            -(locationInWindowY / frameHeightPx - 0.5f) * frameHeight + yShift)

    override fun reshape(): Boolean {
        if(parentRoot == null) {
            return true
        }
        frameWidth = parentRoot.frameWidth
        frameWidthPx = parentRoot.frameHeightPx
        frameHeight = parentRoot.frameHeight
        frameHeightPx = parentRoot.frameHeightPx
        width.set(parentRoot.width.realPos)
        height.set(parentRoot.height.realPos)
        yShift = parentRoot.yShift
        return true
    }

    companion object {
        private const val ratioMin = 0.54f
        private const val ratioMax = 1.85f
    }
}

abstract class AppRootBase : RootNode(null, null)
{
    var activeScreen: ScreenBase? = null
        private set
    var selectedNode: Node? = null
    var grabbedNode: Draggable? = null
    var changeScreenAction: (()->Unit)? = null

    abstract fun willDrawFrame()


    fun changeActiveScreen(newScreen: ScreenBase?) {
        // 0. Cas "réouverture" de l'écran. ** Utile, superflu ?? **
        if(activeScreen === newScreen) {
            newScreen?.openBranch()
            return
        }
        // 1. Fermer l'écran actif (déconnecter si evanescent)
        activeScreen?.let { lastscreen ->
            lastscreen.closeBranch()
            if (!lastscreen.containsAFlag(Flag1.persistentScreen)) {
                Timer(true).schedule(1000) {
                    lastscreen.disconnect()
                }
            }
        }

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
        changeScreenAction?.invoke()
    }
}

/** Une caméra pourrait faire partir de l'arborescence...
 * Mais pour le moment c'est un noeud "à part"...*/
class Camera: Node {
    val x_up : SmoothPos
    val y_up : SmoothPos
    val z_up : SmoothPos
    val x_center : SmoothPos
    val y_center : SmoothPos
    val z_center : SmoothPos

    constructor(x: Float, y: Float, z: Float, lambda: Float = 10f
    ) : super(null, x, y, 1f, 1f, lambda)
    {
        x_up = SmoothPos(0f, lambda)
        y_up = SmoothPos(1f, lambda)
        z_up = SmoothPos(0f, lambda)
        x_center = SmoothPos(0f, lambda)
        y_center = SmoothPos(0f, lambda)
        z_center = SmoothPos(0f, lambda)
        this.z.set(z)
    }

    fun getLookAtModelMatrix(yShift: Float) : FloatArray {
        // (avec des rotations, ce serait plutôt eye = pos + yShift * up, et center = center + yShift * up...)
        return getLookAt(
                Vector3(x.pos, y.pos + yShift, z.pos),
                Vector3(x_center.pos, y_center.pos + yShift, z_center.pos),
                Vector3(x_up.pos, y_up.pos, z_up.pos)
        )
    }
}