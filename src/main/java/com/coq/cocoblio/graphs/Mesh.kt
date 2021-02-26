@file:Suppress("unused")

package com.coq.cocoblio.graphs

import android.opengl.GLES20
import com.coq.cocoblio.divers.printerror
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Structure utilisée pour les maillages de surfaces. */
class Mesh(val vertices: FloatArray, internal val indices: IntArray?, private val primitiveType: Int) {
    // Info pour le vertex Attributes (constant pour l'instant...).
    private val posPerVertex = 3
    private val normalsPerVertex = 3
    private val normalOffset = 3 * 4 // En bytes
    private val uvPerVertex = 2
    private val uvOffset = 6 * 4 // En bytes
    private val vertexSize: Int = 8 * 4 // En bytes (stride)
    private val floatsPerVertex = 8

    private var verticesBufferID: Int
    private var verticesBuffer: Buffer
    /** Les indices des vertex (optionnel, si pas de liste d'indices,
     * on peut faire un triangle strip par exemple...) */
    private var indicesBufferID: Int = -1
    private var indicesBuffer: Buffer? = null

    fun setXofVertex(x: Float, vertexID: Int) {
        vertices[vertexID * floatsPerVertex] = x
    }
    fun setYofVertex(y: Float, vertexID: Int) {
        vertices[vertexID * floatsPerVertex+1] = y
    }
    fun setZofVertex(z: Float, vertexID: Int) {
        vertices[vertexID * floatsPerVertex+2] = z
    }

    init {
        // 1. Buffer des vertices
        verticesBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }
        val vboIDarray = IntArray(1)
        GLES20.glGenBuffers(1, vboIDarray, 0)
        verticesBufferID = vboIDarray[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBufferID)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
            vertices.size * 4, verticesBuffer, GLES20.GL_STATIC_DRAW)
        // 2. Buffer des indices
        indices?.let {
            indicesBuffer = ByteBuffer.allocateDirect(it.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply{
                    put(indices)
                    position(0)
                }
            }
            val vboIndIDarray = IntArray(1)
            GLES20.glGenBuffers(1, vboIndIDarray, 0)
            indicesBufferID = vboIndIDarray[0]
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBufferID)
            GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                indices.size * 4, indicesBuffer, GLES20.GL_STATIC_DRAW)
        }
    }

    constructor(other: Mesh) : this(other.vertices.clone(), other.indices?.clone(),
            other.primitiveType)

    fun updateVerticesBuffer() {
        verticesBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBufferID)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * 4, verticesBuffer, GLES20.GL_STATIC_DRAW)
    }

    fun updateAsAFanWith(ratio: Float) {
        if (vertices.size < 80) {
            printerror("Bad size."); return}
        for(i in 1..9) {
            vertices[i*8]   = -0.5f * sin(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
            vertices[i*8+1] = 0.5f * cos(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
            vertices[i*8+6] = 0.5f - 0.5f * sin(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
            vertices[i*8+7] = 0.5f - 0.5f * cos(ratio*2f* PI.toFloat() * (i-1).toFloat()/8f)
        }
        updateVerticesBuffer()
    }
    /*-- Statics: meshes de bases --*/
    companion object {
        lateinit var defaultSprite: Mesh
            private set
        lateinit var defaultTriangle: Mesh
            private set
        lateinit var defaultFan: Mesh
            private set
        internal fun init(programID: Int) {
            // Vertex Attributes
            attrPositionID = GLES20.glGetAttribLocation(programID, "position").also {
                GLES20.glEnableVertexAttribArray(it)
            }
            attrNormalID = GLES20.glGetAttribLocation(programID, "normal").also {
                if (it >= 0)
                    GLES20.glEnableVertexAttribArray(it)
            }
            attrUVID = GLES20.glGetAttribLocation(programID, "uv").also {
                GLES20.glEnableVertexAttribArray(it)
            }
            currentMesh = null
            currentPrimitiveType = -1
            currentVertexCount = -1

            // Les meshes par défaut
            defaultSprite = Mesh(floatArrayOf(
                -0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
                0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f),
                null, GLES20.GL_TRIANGLE_STRIP)
            defaultTriangle =  Mesh(floatArrayOf(
                0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 0.0f,
                0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.067f, 0.75f,
                -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 0.933f, 0.75f),
                null, GLES20.GL_TRIANGLES)
            defaultFan = Mesh(FloatArray(10*8) {0f}.also {
                // Centre de la fan.
                it[5] = 1f
                it[6] = 0.5f
                it[7] = 0.5f
                for(i in 1..9) {
                    it[i*8]   = -0.5f * sin(2f* PI.toFloat() * (i-1).toFloat()/8f)
                    it[i*8+1] = 0.5f * cos(2f* PI.toFloat() * (i-1).toFloat()/8f)
                    it[i*8+5] = 1f
                    it[i*8+6] = 0.5f - 0.5f * sin(2f* PI.toFloat() * (i-1).toFloat()/8f)
                    it[i*8+7] = 0.5f - 0.5f * cos(2f* PI.toFloat() * (i-1).toFloat()/8f)
                }
            }, intArrayOf(
                0, 1, 2,
                0, 2, 3,
                0, 3, 4,
                0, 4, 5,
                0, 5, 6,
                0, 6, 7,
                0, 7, 8,
                0, 8, 9
            ), GLES20.GL_TRIANGLES)

        }

        /*-- Gestion des meshes --*/
        internal fun setMesh(newMesh: Mesh) {
            currentMesh = newMesh
            currentPrimitiveType = newMesh.primitiveType
            currentVertexCount = newMesh.vertices.size / newMesh.floatsPerVertex
            // Mise à jour du vertex buffer.
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, newMesh.verticesBufferID)
            GLES20.glVertexAttribPointer(
                attrPositionID,
                newMesh.posPerVertex, GLES20.GL_FLOAT,
                false, newMesh.vertexSize, 0
            )
            if (attrNormalID >= 0) {
                GLES20.glVertexAttribPointer(
                    attrNormalID, newMesh.normalsPerVertex, GLES20.GL_FLOAT,
                    false, newMesh.vertexSize, newMesh.normalOffset
                )
            }
            GLES20.glVertexAttribPointer(
                attrUVID,
                newMesh.uvPerVertex, GLES20.GL_FLOAT,
                false, newMesh.vertexSize, newMesh.uvOffset
            )
            // Fini si pas indexé.
            if (newMesh.indices == null) return
            // Lier la liste d'indices.
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, newMesh.indicesBufferID)
        }
        private var attrPositionID: Int = -1
        private var attrNormalID: Int = -1
        private var attrUVID: Int = -1
        internal var currentMesh: Mesh? = null
        internal var currentPrimitiveType: Int = -1
        internal var currentVertexCount: Int = -1

    }
}