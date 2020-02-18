@file:Suppress("unused")

package com.coq.cocoblio

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.coq.cocoblio.maths.*
import com.coq.cocoblio.nodes.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

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

    lateinit var gameEngine: GameEngineBase
    var setForDrawing : (Node.() -> Surface?) = Node::defaultSetNodeForDrawing

    /*-- Gestion des events (de Activity) --*/
    /*-- Semble superflu, mais doit être dans la thread OpenGL... ??--*/
    fun onKeyDown(key: KeyboardKey) {
        gameEngine.keyDown(key)
    }
    fun onKeyUp(key: KeyboardKey) {
        gameEngine.keyUp(key)
    }
    fun onConfigurationChanged() {
        gameEngine.configurationChanged()
    }
    fun onTouchUp(vitRawX: Float?, vitRawY: Float?) {
        touchDragStarted = false
        vitRawX?.let { vx -> vitRawY?. let {vy ->
            gameEngine.letTouchDrag(getPositionFrom(vx, vy, true))
            return
        }}
        gameEngine.letTouchDrag(null)
    }
    private var touchDragStarted = false
    fun onTouchDrag(posInitX: Float, posInitY: Float, posNowX: Float, posNowY: Float) {
        val posInit = getPositionFrom(posInitX, posInitY, true)
        val posNow = getPositionFrom(posNowX, posNowY, true)
        if(!touchDragStarted) {
            gameEngine.initTouchDrag(posInit)
            touchDragStarted = true
        }
        gameEngine.touchDrag(posNow)
    }
    fun onSingleTap(posX: Float, posY: Float) {
        val pos = getPositionFrom(posX, posY, true)
        gameEngine.singleTap(pos)
        touchDragStarted = false
    }
    fun onPause() {
        gameEngine.appPaused()
    }


    override fun onDrawFrame(gl: GL10?) {
        // 1. Update du temps.
        GlobalChrono.update()

        // 2. Matrice de projection et "vrai" cadre de la view.
        if (width > height) { // Landscape
            frameFullHeight = 2f / heightRatio.pos
            frameFullWidth = width / height * frameFullHeight
        } else {
            frameFullWidth = 2f / widthRatio.pos
            frameFullHeight = height / width * frameFullWidth
        }
        val projection = getPerspective(
            0.1f, 50f, gameEngine.root.z.pos,
            frameFullWidth, frameFullHeight)
        GLES20.glUniformMatrix4fv(
            pfuProjectionID, 1,
            false, projection, 0)

        // 3. Mise à jour du temps des shaders
        if (shadersTime.elsapsedSec > 24f) {
            shadersTime.removeSec(24f)
        }
        GLES20.glUniform1f(pfuTimeID, shadersTime.elsapsedSec)

        // 4. Action du game engine avant l'affichage
        gameEngine.willDrawFrame(frameFullWidth, frameFullHeight)

        // 5. Mise à jour de la couleur de fond.
        GLES20.glClearColor(smR.pos, smG.pos, smB.pos, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 6. Boucle d'affichage
        val sq = Squirrel(gameEngine.root)
        do {
            sq.pos.setForDrawing()?.draw()
        } while (sq.goToNextToDisplay())
    }
    override fun onSurfaceChanged(gl: GL10?, newWidth: Int, newHeight: Int) {
        GLES20.glViewport(0, 0, newWidth, newHeight)
        height = newHeight.toFloat()
        width = newWidth.toFloat()
        val ratio = width / height
        val usableHeight: Float
        val usableWidth: Float

        if (height > width) {
            widthRatio.pos = bordRatio
            heightRatio.pos = bordRatio * min(1f, ratio / usableRatioMin)
            usableWidth = 2f
            usableHeight = 2f * (heightRatio.realPos / widthRatio.realPos) / ratio
        } else {
            heightRatio.pos = bordRatio
            widthRatio.pos = bordRatio * min(1f, usableRatioMax / ratio)
            usableHeight = 2f
            usableWidth = 2f * ratio * (widthRatio.realPos / heightRatio.realPos)
        }

        gameEngine.viewReshaped(usableWidth, usableHeight)
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
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER,
            customVertShadResID ?: R.raw.shadervert)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER,
            customFragShadResID ?: R.raw.shaderfrag)
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

        // 3. Init de la classe Texture
        Texture.init(programID, coqActivity)
        // 4. Init des vertex attributes.
        Mesh.init(programID)

        // 5. Init du temps des shaders.
        shadersTime.start()

        // 6. Init du GameEngine...
        gameEngine = coqActivity.getGameEngine()
    }

    fun initClearColor(r: Float, g: Float, b: Float) {
        smR.set(r); smG.set(g); smB.set(b)
    }
    fun updateClearColor(r: Float, g: Float, b: Float) {
        smR.pos = r; smG.pos = g; smB.pos = b
    }
    fun getPositionFrom(locationInWindowX: Float, locationInWindowY: Float,
                        invertedY: Boolean) = Vector2(
        (locationInWindowX / width - 0.5f) * frameFullWidth,
        (if (invertedY) -1f else 1f) * (locationInWindowY / height - 0.5f) * frameFullHeight)

    /** Dessiner une surface */
    private fun Surface.draw() {
        // 1. Mise a jour de la mesh ?
        if (mesh !== Mesh.currentMesh) {
            Mesh.setMesh(mesh)
        }
        // 2. Mise a jour de la texture ?
        if (tex !== Texture.currentTexture) {
            Texture.setTexture(tex)
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
            GLES20.glDrawElements(Mesh.currentPrimitiveType,
                mesh.indices.size, GLES20.GL_UNSIGNED_INT, 0)
        } else {
            GLES20.glDrawArrays(Mesh.currentPrimitiveType, 0, Mesh.currentVertexCount)
        }
    }

    /*-- Private stuff --*/
    private var width: Float = 1.0f // En pixels
    private var height: Float = 1.0f
    // Le vrai espace (toute la vue avec les bords),
    // a priori, change à chaque frame.
    // Le fullWidth/fullHeight de la root est changé en même temps.
    private var frameFullWidth = 2f
    private var frameFullHeight = 2f
    private var widthRatio = SmoothPos(1f, 8f)
    private var heightRatio = SmoothPos(1f, 8f)
    private var smR = SmoothPos(0f, 8f)
    private var smG = SmoothPos(0f, 8f)
    private var smB = SmoothPos(0f, 8f)
    private val shadersTime = Chrono()
    /** Les static / global access */
    companion object {
        private const val usableRatioMin = 0.54f
        private const val usableRatioMax = 1.85f
        private var bordRatio = 0.95f
    }

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
}

/** La fonction utilisé par défaut pour CoqRenderer.setNodeForDrawing.
 * Retourne la surface à afficher (le noeud présent si c'est une surface). */
private fun Node.defaultSetNodeForDrawing() : Surface? {
    // 0.0 Prendre le model du parent.
    parent?.let {
        System.arraycopy(it.piu.model, 0, piu.model, 0, 16)
    } ?: run {
        // 0.1 Cas racine
        (this as? RootNode)?.setModelAsCamera() ?: printerror("Root pas un RootNode.")
        return null
    }

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

