@file:Suppress("unused")

package com.coq.cocoblio.graphs

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import com.coq.cocoblio.R
import com.coq.cocoblio.divers.*
import com.coq.cocoblio.maths.*
import com.coq.cocoblio.nodes.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer(private val coqActivity: CoqActivity,
               private val customVertShadResID: Int?,
               private val customFragShadResID: Int?) : GLSurfaceView.Renderer
{

    /** Les propriétés d'affichage d'un objet/instance: matrice du modèle, tile, couleur, flags... */
    class PerInstanceUniforms {
        var model: FloatArray
        var color: FloatArray
        var i: Float
        var j: Float
        var emph: Float
        var flags: Int

        constructor() {
            model = FloatArray(16)
            i = 0.0f
            j = 0.0f
            color = floatArrayOf(1f, 1f, 1f, 1f) // alpha pour l'apparition de l'objet (0,caché) -> (1,montré)
            emph = 0.0f
            flags = 0
        }

        constructor(piu: PerInstanceUniforms) {
            model = piu.model.copyOf()
            i = piu.i
            j = piu.j
            color = piu.color.copyOf()
            emph = piu.emph
            flags = piu.flags
        }

        @Suppress("unused")
        companion object {
            /** Exemple de flag pour les shaders (ajouter à piu.flags). */
            const val isOneSided: Int = 1
        }
    }

    // Fonction à utiliser pour dessiner un noeud (customizable)
    var setForDrawing : (Node.() -> Surface?) = Node::defaultSetNodeForDrawing

    private lateinit var root: AppRootBase
    private var currentMesh: Mesh? = null
    private var currentTexture: Texture? = null

    override fun onDrawFrame(gl: GL10?) {
        // 1. Update du temps.
        GlobalChrono.update()

        // 2. Matrice de projection

        GLES20.glUniformMatrix4fv(
            pfuProjectionID, 1,
            false, root.getProjectionMatrix(), 0)

        // 3. Mise à jour du temps des shaders
        if (shadersTime.elapsedSec > 24f) {
            shadersTime.removeSec(24f)
        }
        GLES20.glUniform1f(pfuTimeID, shadersTime.elapsedSec)

        // 4. Action sur la structure avant l'affichage
        root.willDrawFrame()

        // 5. Mise à jour de la couleur de fond.
        GLES20.glClearColor(smR.pos, smG.pos, smB.pos, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 6. Boucle d'affichage
        currentTexture = null
        currentMesh = null
        val sq = Squirrel(root)
        do {
            sq.pos.setForDrawing()?.draw()
        } while (sq.goToNextToDisplay())
    }
    override fun onSurfaceChanged(gl: GL10?, newWidth: Int, newHeight: Int) {
        GLES20.glViewport(0, 0, newWidth, newHeight)
        printdebug("Reshape gl surface to $newWidth x $newHeight.")
        root.updateFrameSize(newWidth, newHeight, 0f, 0f, 0f, 0f)
        if (!Texture.loaded) {
            Texture.resume(coqActivity)
        }
    }
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        fun loadShader(type: Int, shaderResource: Int) : Int {
            val inputStream = coqActivity.resources.openRawResource(shaderResource)
            val shaderCode = inputStream.bufferedReader().use { it.readText() }
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                print(GLES20.glGetShaderInfoLog(shader))
            }
        }
        // 1. Création du program.
        printdebug("Création de la surface GL.")
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER,
            customVertShadResID ?: R.raw.shadervert
        )
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER,
            customFragShadResID ?: R.raw.shaderfrag
        )
        programID = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            GLES20.glUseProgram(it)
        }
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 2. Obtenir les ID OpenGL des variables des shaders.
        pfuProjectionID = GLES20.glGetUniformLocation(programID, "projection")
        pfuTimeID =  GLES20.glGetUniformLocation(programID, "time")
        piuModelID = GLES20.glGetUniformLocation(programID, "model")
        piuTexIJID = GLES20.glGetUniformLocation(programID, "texIJ")
        piuColorID = GLES20.glGetUniformLocation(programID, "color")
        piuEmphID  = GLES20.glGetUniformLocation(programID, "emph")
        piuFlagsID = GLES20.glGetUniformLocation(programID, "flags")
        ptuTexWHID = GLES20.glGetUniformLocation(programID, "texWH")
        ptuTexMNID = GLES20.glGetUniformLocation(programID, "texMN")

        // 3. Init de Texture
        Texture.init(coqActivity)

        // 4. Init des vertex attributes.
        Mesh.init(programID)

        // 5. Init du temps des shaders.
        shadersTime.start()

        // 6. Init de la structure
        root = coqActivity.getTheAppRoot()
    }
    fun onPause() {
        Texture.suspend()
    }

    // Ne semble pas marcher...? On fait le resume dans onSurfaceChanged...
//    fun onResume() {
//        Texture.resume(coqActivity)
//    }

    fun initClearColor(r: Float, g: Float, b: Float) {
        smR.set(r); smG.set(g); smB.set(b)
    }
    fun updateClearColor(r: Float, g: Float, b: Float) {
        smR.pos = r; smG.pos = g; smB.pos = b
    }

    /*-- Gestion des events (de Activity) --*/
    /*-- Semble superflu, mais doit être dans la thread OpenGL... ??--*/
    fun onKeyDown(key: KeyboardKey) {
        when (key.keycode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                (root.activeScreen as? Enterable)?.let { enterable ->
                    enterable.enterAction()
                    return
                }
            KeyEvent.KEYCODE_ESCAPE ->
                (root.activeScreen as? Escapable)?.let { escapable ->
                    escapable.escapeAction()
                    return
                }
        }
        (root.activeScreen as? KeyResponder)?.keyDown(key)
    }
    fun onKeyUp(key: KeyboardKey) {
        (root.activeScreen as? KeyResponder)?.keyUp(key)
    }
    fun onConfigurationChanged() {
        printerror("Configuration changed?...")
    }
    fun onTouchUp(vitRawX: Float?, vitRawY: Float?) {
        touchDragStarted = false
        vitRawX?.let { vx -> vitRawY?. let {vy ->
            val vit = root.getPositionFrom(vx, vy)
            (root.selectedNode as? Draggable)?.let { draggable ->
                val relVit = (draggable as Node).relativeDeltaOf(vit)
                draggable.letGo(relVit)
            }
        }}
        root.selectedNode = null
        // TODO : Réviser...
    }
    private var touchDragStarted = false
    fun onTouchDrag(posInitX: Float, posInitY: Float, posNowX: Float, posNowY: Float) {
        val posInit = root.getPositionFrom(posInitX, posInitY)
        val posNow = root.getPositionFrom(posNowX, posNowY)

        if(!touchDragStarted) {
            touchDragStarted = true
            root.selectedNode = null
            root.activeScreen?.searchBranchForSelectable(posInit, null)?.let { toSelect ->
                if(toSelect is Draggable) {
                    root.selectedNode = toSelect
                } else { // Cas grandPa
                    val grandPa = toSelect.parent?.parent
                    if (grandPa is Draggable) {
                        root.selectedNode = grandPa
                    }
                }
                val relPos = root.selectedNode?.relativePosOf(posInit) ?: run {
                    onSingleTap(posInitX, posInitY)
                    return
                }
                (root.selectedNode as? Draggable)?.grab(relPos)
            }
        }
        (root.selectedNode as? Draggable)?.let { draggable ->
            val relPosNow = (draggable as Node).relativePosOf(posNow)
            draggable.drag(relPosNow)
        }
    }
    fun onSingleTap(posX: Float, posY: Float) {
        val pos = root.getPositionFrom(posX, posY)
        root.activeScreen?.searchBranchForSelectable(pos, null)?. let { toSelect ->
            (toSelect as? SwitchButton)?.justTap()
            (toSelect as? Button)?.action()
        }
        touchDragStarted = false
    }

    /** Dessiner une surface */
    private fun Surface.draw() {
        // 1. Mise a jour de la mesh ?
        if (mesh !== Mesh.currentMesh) {
            Mesh.setMesh(mesh)
        }
        // 2. Mise a jour de la texture ?
        if (tex !== currentTexture) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.glID)
            GLES20.glUniform2f(ptuTexWHID, tex.width, tex.height)
            GLES20.glUniform2f(ptuTexMNID, tex.m.toFloat(), tex.n.toFloat())
            currentTexture = tex
        }
        // 3. Mise à jour des "PerInstanceUniforms"
        GLES20.glUniformMatrix4fv(
            piuModelID, 1, false,
            piu.model, 0)
        GLES20.glUniform2f(piuTexIJID, piu.i, piu.j)
        GLES20.glUniform4fv(piuColorID, 1, piu.color, 0)
        GLES20.glUniform1f(piuEmphID, piu.emph)
        GLES20.glUniform1i(piuFlagsID, piu.flags)
        // 4. Dessiner
        if (mesh.indices != null) {
            GLES20.glDrawElements(
                Mesh.currentPrimitiveType,
                mesh.indices!!.size, GLES20.GL_UNSIGNED_INT, 0)
        } else {
            GLES20.glDrawArrays(Mesh.currentPrimitiveType, 0, Mesh.currentVertexCount)
        }
    }

    /*-- Private stuff --*/
    private var smR = SmoothPos(0f, 8f)
    private var smG = SmoothPos(0f, 8f)
    private var smB = SmoothPos(0f, 8f)
    private val shadersTime = Chrono()

    /*---------------------*/
    /*-- Stuff OpenGL... --*/
    // ID des variable de shaders...
    private var programID: Int = -1
    // ID des "per frame uniforms"
    private var pfuProjectionID: Int = -1
    private var pfuTimeID: Int = -1
    // ID des "per instance uniforms"
    private var piuModelID: Int = -1
    private var piuTexIJID: Int = -1
    private var piuColorID: Int = -1
    private var piuEmphID: Int = -1
    private var piuFlagsID: Int = -1
    // ID des "per texture uniforms"
    private var ptuTexWHID: Int = -1
    private var ptuTexMNID: Int = -1
}

/** La fonction utilisé par défaut pour CoqRenderer.setNodeForDrawing.
 * Retourne la surface à afficher (le noeud présent si c'est une surface). */
private fun Node.defaultSetNodeForDrawing() : Surface? {
    // 0. Cas racine
    if(containsAFlag(Flag1.isRoot)) {
        (this as RootNode).setModelMatrix()
        return null
    }
    // 0.1 Copy du parent
    val theParent = parent ?: run {
        printerror("Pas de parent pour noeud non root.")
        return null
    }
    System.arraycopy(theParent.piu.model, 0, piu.model, 0, 16)
    // 1. Cas branche
    if (firstChild != null) {
        piu.model.translate(x.pos, y.pos, z.pos)
        piu.model.scale(scaleX.pos, scaleY.pos, 1f)
        return null
    }
    // 3. Cas feuille
    // Laisser faire si n'est pas affichable...
    if (this !is Surface) {return null}
    // Facteur d'"affichage"
    val alpha = trShow.setAndGet(containsAFlag(Flag1.show))
    piu.color[3] = alpha
    // Rien à afficher...
    if (alpha == 0f) { return null }
    piu.model.translate(x.pos, y.pos, z.pos)
    if (containsAFlag(Flag1.poping)) {
        piu.model.scale(width.pos * alpha, height.pos * alpha, 1f)
    } else {
        piu.model.scale(width.pos, height.pos, 1f)
    }
    return this
}

