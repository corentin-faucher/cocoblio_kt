@file:Suppress("unused")

package com.coq.cocoblio

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.coq.cocoblio.maths.*
import com.coq.cocoblio.nodes.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class CoqRenderer(private val coqActivity: CoqActivity,
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

    /*-- ??? Peut-on le mettre dans Activity ??? --*/
    // Le gestionnaire/contrôleur des actions
//    private var ge: GameEngineBase? = null
    private var eh: EventsHandler? = null
    private var root: Node? = null

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

    /*-- Gestion des events (de Activity) --*/
    fun onKeyDown(key: KeyboardKey) {
        eh?.keyDown(key)
    }
    fun onKeyUp(key: KeyboardKey) {
        eh?.keyUp(key)
    }

    fun onConfigurationChanged() {
        eh?.configurationChanged()
    }

    fun onTouchUp(vit: Vector2?) {
//        println("onTouchUp $vit")
        eh?.letTouchDrag(vit)
        touchDragStarted = false
    }
    private var touchDragStarted = false
    fun onTouchDrag(posInit: Vector2, posNow: Vector2) {
//        println("onTouchDrag $posInit, $posNow")
        if(!touchDragStarted) {
            eh?.initTouchDrag(posInit)
            touchDragStarted = true
        }
        eh?.touchDrag(posNow)
    }
    fun onSingleTap(pos: Vector2) {
//        println("onSingleTap $pos")
        eh?.singleTap(pos)
        touchDragStarted = false
    }
    fun onPause() {
        eh?.appPaused()
    }



    override fun onDrawFrame(gl: GL10?) {
        // 0. Update du temps.
        GlobalChrono.update()
        // 1. Mise à jour de la couleur de fond.
        GLES20.glClearColor(smR.pos, smG.pos, smB.pos, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // 2. Mise à jour de la matrice de projection
        if (width > height) { // Landscape
            frameFullHeight = 2f / heightRatio.pos
            frameFullWidth = width / height * frameFullHeight
        } else {
            frameFullWidth = 2f / widthRatio.pos
            frameFullHeight = height / width * frameFullWidth
        }
        val projection = getPerspective(
            0.1f, 50f, smCameraZ.pos,
            frameFullWidth, frameFullHeight)
        GLES20.glUniformMatrix4fv(
            pfuProjectionID, 1,
            false, projection, 0)
        // 3. Mise à jour du temps des shaders
        if (shadersTime.elsapsedSec > 24f) {
            shadersTime.removeSec(24f)
        }
        GLES20.glUniform1f(pfuTimeID, shadersTime.elsapsedSec)
        // 4. Action falcultative du gameEngine avec frameFullWidth à jour.
        eh?.onDrawFrame()
        // 5. Dessiner les noeuds
        val sq = Squirrel(root ?: run { printerror("Rien à afficher."); return})
        do {
            sq.pos.setNodeForDrawing()?.draw()
        } while (sq.goToNextToDisplay())
    }
    override fun onSurfaceChanged(gl: GL10?, newWidth: Int, newHeight: Int) {
        GLES20.glViewport(0, 0, newWidth, newHeight)
        height = newHeight.toFloat()
        width = newWidth.toFloat()
        val ratio = width / height
        println("onSurfaceChanged portrait: ${portrait}")
        portrait = ratio < 1f

        if (portrait) {
            widthRatio.pos = bordRatio
            heightRatio.pos = bordRatio * min(1f, ratio / usableRatioMin)
            frameUsableWidth = 2f
            frameUsableHeight = 2f * (heightRatio.realPos / widthRatio.realPos) / ratio
        } else {
            heightRatio.pos = bordRatio
            widthRatio.pos = bordRatio * min(1f, usableRatioMax / ratio)
            frameUsableHeight = 2f
            frameUsableWidth = 2f * ratio * (widthRatio.realPos / heightRatio.realPos)
        }
        println("calling viewReshape")
        eh?.viewReshaped()
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

        // 6. Init de la structure/eventsHandler (i.e. gameengine)
        if (eh == null) {
            eh = coqActivity.getEventstHandler()
        }
        if (root == null) {
            root = coqActivity.getStructureRoot()
        }
    }
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
    /** Les static / global access */
    companion object {
        fun initClearColor(r: Float, g: Float, b: Float) {
            smR.realPos = r; smG.realPos = g; smB.realPos = b
        }
        fun updateClearColor(r: Float, g: Float, b: Float) {
            smR.pos = r; smG.pos = g; smB.pos = b
        }
        fun getPositionFrom(locationInWindowX: Float, locationInWindowY: Float,
                            invertedY: Boolean) = Vector2(
            (locationInWindowX / width - 0.5f) * frameFullWidth,
            (if (invertedY) -1f else 1f) * (locationInWindowY / height - 0.5f) * frameFullHeight)

        var setNodeForDrawing : (Node.() -> Surface?) = Node::defaultSetNodeForDrawing
        // Dimensions de la vue (global access)
        var portrait: Boolean = false
            private set
        var width: Float = 1.0f // En pixels
            private set
        var height: Float = 1.0f
            private set
        var frameUsableWidth = 2f
            private set
        var frameUsableHeight = 2f
            private set
        var frameFullWidth = 2f
            private set
        var frameFullHeight = 2f
            private set
        private const val usableRatioMin = 0.54f
        private const val usableRatioMax = 1.85f
        var bordRatio = 0.95f
        /** La position en z de la camera (libre d'accès) */
        val smCameraZ = SmPos(2f, 5f)
        val shadersTime = Chrono()
        private var smR = SmPos(0f, 8f)
        private var smG = SmPos(0f, 8f)
        private var smB = SmPos(0f, 8f)
        private var widthRatio = SmPos(1f, 8f)
        private var heightRatio = SmPos(1f, 8f)

        private var gameEngineCount = 0
    }
}

/** La fonction utilisé par défaut pour CoqRenderer.setNodeForDrawing.
 * Retourne la surface à afficher (le noeud présent si c'est une surface). */
private fun Node.defaultSetNodeForDrawing() : Surface? {
    // 1. Init de la matrice model avec le parent.
    parent?.let {
        System.arraycopy(it.piu.model, 0, piu.model, 0, 16)
    } ?: run {
        // Cas racine -> model est la caméra.
        piu.model = getLookAt(
            Vector3(0f, 0f, CoqRenderer.smCameraZ.pos),
            Vector3(0f, 0f, 0f),
            Vector3(0f, 1f, 0f)
        )
    }

    // 2. Cas branche
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

    if(mesh === Mesh.defaultFan) {
        mesh.updateAsAFanWith(0.5f + 0.5f*sin(CoqRenderer.shadersTime.elsapsedSec))
    }

    return this
}

