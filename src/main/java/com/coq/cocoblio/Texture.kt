@file:Suppress("unused")

package com.coq.cocoblio

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils

/** Ensemble des info relatives à une texture.
 *  Contient l'ID de resource, l'ID openGL et les dimensions de la texture. */
class Texture {
    var m: Int = 1
        private set
    var n: Int = 1
        private set
    var string: String = ""
    var glID: Int = -1
    var width: Float = 1f
    var height: Float = 1f
    var ratio: Float = 1f

    internal fun freeOpenGLTexture() {
        if (glID < 0 ) return
        val tmp = IntArray(1)
        tmp[0] = glID
        GLES20.glDeleteTextures(1, tmp, 0)
        glID = -1
    }

    private fun initAsString() {
        fun textToBitmap(text: String) : Bitmap? {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textSize = textSize
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.SERIF
            val baseline = -paint.ascent()
            val widthI: Int = (paint.measureText(text) + 0.5f + textSize * extraWidth).toInt()
            val heightI: Int = (baseline + paint.descent() + 0.5f).toInt()

            if(widthI < 2 || heightI < 2) return null

            val image = Bitmap.createBitmap(widthI, heightI, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            canvas.drawText(text, 0.5f * textSize * extraWidth, baseline, paint)

            return image
        }
        // 0. Check si libre...
        if (glID >=0)
            freeOpenGLTexture()
        // 1. Générer une nouvelle texture.
        val textureIDarrayTmp = IntArray(1)
        GLES20.glGenTextures(1, textureIDarrayTmp, 0)
        if (textureIDarrayTmp[0] == 0) {
            printerror("Ne peut générer de texture. (string)"); return
        }
        // 2. Création de la texture...
        val bitmap : Bitmap = textToBitmap(if(string.isEmpty()) " " else string) ?: run {
            freeOpenGLTexture(); return }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIDarrayTmp[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER, glFilter
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER, glFilter
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // 3. Enregistrer l'ID de la texture OpenGL et ses info.
        glID = textureIDarrayTmp[0]
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        ratio = bitmap.width.toFloat() / bitmap.height.toFloat()

        bitmap.recycle()
    }

    companion object {
        /*-- Méthodes de bases --*/
        /** Chargement des pngs "de base" et uniforms des textures. --*/
        internal fun init(programID: Int, context: Context) {
            ptuTexWHID = GLES20.glGetUniformLocation(programID, "texWH")
            ptuTexMNID = GLES20.glGetUniformLocation(programID, "texMN")
            currentTexture = null

            pngList.clear()
            cstStringList.clear()
            localizedStringList.clear()
            editableStringList.clear()
            currentFreeEditableStringID = 0

            initPngTex(R.drawable.digits_black, 12,2, context)
            initPngTex(R.drawable.frame_mocha, 3,3, context)
            initPngTex(R.drawable.frame_white_back, 3,3, context)
            initPngTex(R.drawable.language_flags, 4,4, context)
            initPngTex(R.drawable.some_animals, 4,4, context)
            initPngTex(R.drawable.sparkle_stars, 3,2, context)
            initPngTex(R.drawable.switch_back, 1,1, context)
            initPngTex(R.drawable.switch_front, 1,1, context)
            initPngTex(R.drawable.test_frame, 1, 1, context)
            initPngTex(R.drawable.the_cat, 1,1, context)
        }
        /** Tout effacer (à caller lorsque l'activité est détruit (onDestroy) */
        /*fun clear() {
            println("cleaning Texture")
            for ((_, texture) in pngList) {
                texture.freeOpenGLTexture()
            }
            pngList.clear()
            for ((_, texture) in cstStringList) {
                texture.freeOpenGLTexture()
            }
            cstStringList.clear()
            for ((_, texture) in localizedStringList) {
                texture.freeOpenGLTexture()
            }
            localizedStringList.clear()
            for ((_, texture) in editableStringList) {
                texture.freeOpenGLTexture()
            }
            editableStringList.clear()
            println("End cleaning Texture")
        } */
        /*-- Options sur les textures --*/
        fun setGLFilter(asLinear: Boolean) {
            glFilter = if(asLinear) GLES20.GL_LINEAR else GLES20.GL_NEAREST
        }
        private var glFilter = GLES20.GL_LINEAR
        fun setTextSizeAndExtraWidth(newTextSize: Float, newExtraWidth: Float) {
            textSize = newTextSize
            extraWidth = newExtraWidth
        }
        private var textSize = 48f
        private var extraWidth = 0.15f

        /*-- Gestion des pngs --*/
        /** Init d'un png quelconque (image dans un projet spécifique) */
        fun initPngTex(resID: Int, m: Int, n: Int, context: Context) {
            val newTex: Texture = pngList[resID]?.also{printerror("$resID déjà init ?")}
                ?: Texture()
            newTex.m = m
            newTex.n = n

            // 0. Check si libre...
            if (newTex.glID >=0)
                newTex.freeOpenGLTexture()
            // 1. Générer une nouvelle texture.
            val textureIDarrayTmp = IntArray(1)
            GLES20.glGenTextures(1, textureIDarrayTmp, 0)
            if (textureIDarrayTmp[0] == 0) {
                printerror("Ne peut générer de texture."); return
            }
            // 2. Création de la texture...
            val options = BitmapFactory.Options().apply {
                inScaled = false
            }
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                resID, options
            )

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIDarrayTmp[0])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, glFilter
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, glFilter
            )
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            // 3. Enregistrer l'ID de la texture OpenGL et ses info.
            newTex.glID = textureIDarrayTmp[0]
            newTex.width = bitmap.width.toFloat()
            newTex.height = bitmap.height.toFloat()
            newTex.ratio = bitmap.width.toFloat() / bitmap.height.toFloat() *
                    n.toFloat() / m.toFloat()
            bitmap.recycle()

            pngList[resID] = newTex
        }
        fun getPngTex(resID: Int) : Texture {
            pngList[resID]?.let {
                return it
            }
            printerror("Texture pas encore init?")
            return Texture().also {
                pngList[resID] = it
            }
        }
        private val pngList = mutableMapOf<Int, Texture>()

        /*-- Les strings constantes --*/
        fun getConstantStringTex(string: String): Texture {
            cstStringList[string]?.let {
                return it
            }
            val newTexture = Texture()
            newTexture.string = string
            newTexture.initAsString()
            cstStringList[string] = newTexture
            return newTexture
        }
        private val cstStringList = mutableMapOf<String, Texture>()

        /**-- Les strings localisées.
         * On pourrait ne pas avoir besoin du context si les string étaient initialisées... ? --*/
        fun initLocalizedStringTex(resStrID: Int, ctx: Context) {
            val str = ctx.getString(resStrID)
            localizedStringList[resStrID]?.let {
                printerror("Localized string $resStrID $str déjà init.")
                return
            }
            val newTex = Texture()
            newTex.string = str
            newTex.initAsString()
            localizedStringList[resStrID] = newTex
        }
        fun getLocalizedStringTex(resStrID: Int): Texture {
            localizedStringList[resStrID]?.let {
                return it
            }
            printerror("Localized string $resStrID non init.")

            return getConstantStringTex("?")
        }
        fun updateAllLocalizedStrings(context: Context) {
            localizedStringList.forEach {
                it.value.string = context.getString(it.key)
                it.value.initAsString()
            }
        }
        private val localizedStringList = mutableMapOf<Int, Texture>()

        /*-- Les strings éditables --*/
        fun setEditableString(id: Int, newString: String) {
            editableStringList[id]?.let {
                it.string = newString
                it.initAsString()
                return
            }
            val newTextureInfo = Texture()
            newTextureInfo.string = newString
            newTextureInfo.initAsString()
            editableStringList[id] = newTextureInfo
        }
        fun getEditableStringTex(id: Int): Texture {
            editableStringList[id]?.let {
                return it
            }
            val newTextureInfo = Texture()
            editableStringList[id] = newTextureInfo
            return newTextureInfo
        }
        fun getEditableString(id: Int): String {
            editableStringList[id]?.let {
                return it.string
            }
            printerror("EditableString pas dans la liste.")
            return "I am error"
        }
        fun getNewEditableStringID() : Int {
            while (editableStringList[currentFreeEditableStringID] != null) {
                currentFreeEditableStringID += 1
            }
            val returnID = currentFreeEditableStringID
            currentFreeEditableStringID +=1
            return returnID
        }
        private val editableStringList = mutableMapOf<Int, Texture>()
        private var currentFreeEditableStringID = 0

        /*-- Changement de la texture utilisée par OpenGL. --*/
        internal fun setTexture(tex: Texture) {
            currentTexture = tex
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.glID)
            GLES20.glUniform2f(ptuTexWHID, tex.width, tex.height)
            GLES20.glUniform2f(ptuTexMNID, tex.m.toFloat(), tex.n.toFloat())
        }
        internal var currentTexture: Texture? = null
        private var ptuTexWHID: Int = -1      // En pixels
        private var ptuTexMNID: Int = -1   // En cases (tiles)
    }
}


/*-- Les chars constantes --*/
/*
fun getConstantCharTex(char: Char): Texture {
    cstCharList[char]?.let {
        return it
    }
    val newTexture = Texture()
    newTexture.string = char.toString()
    newTexture.initAsString()
    cstCharList[char] = newTexture
    return newTexture
}
private val cstCharList = mutableMapOf<Char, Texture>()
*/

