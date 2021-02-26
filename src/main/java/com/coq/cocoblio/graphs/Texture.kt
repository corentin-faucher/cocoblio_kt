@file:Suppress("unused", "ConvertSecondaryConstructorToPrimary", "JoinDeclarationAndAssignment", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.graphs

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import com.coq.cocoblio.R
import com.coq.cocoblio.divers.printdebug
import com.coq.cocoblio.divers.printerror
import com.coq.cocoblio.divers.printwarning
import com.coq.cocoblio.divers.strip
import java.lang.ref.WeakReference

data class Tiling(val m: Int, val n: Int, val asLinear: Boolean = true)

enum class TextureType {
    Png,
    ConstantString,
    MutableString,
    LocalizedString,
}

/** Ensemble des info relatives à une texture.
 *  Contient l'ID de resource, l'ID openGL et les dimensions de la texture. */
class Texture {
    val m: Int
    val n: Int
    private val asLinear: Boolean
    var name: String = ""
        private set
    private val id: Int
    val type: TextureType

    var glID: Int = -1
        private set
    var width: Float = 1f
        private set
    var height: Float = 1f
        private set
    var ratio: Float = 1f
        private set

    fun updateAsMutableString(string: String) {
        if (type != TextureType.MutableString) {
            printerror("str: $string n'est pas une string mutable.")
            return
        }
        this.name = string
        drawAsString()
    }

    private constructor(resId: Int, name: String, type: TextureType) {
        this.type = type
        id = resId
        this.name = name
        if(type == TextureType.Png) {
            val tiling = pngResIdToTiling[resId]
            val ctx = context.get()
            if (tiling != null && ctx != null) {
                m = tiling.m; n = tiling.n; asLinear = tiling.asLinear
                drawAsPng(resId, ctx)
                return
            }
            printerror("Pas de tiling ou pas de context pour png $resId.")
            m = 1; n = 1; asLinear = true
            this.name = "Error"
        } else {
            m = 1; n = 1; asLinear = true
        }

        drawAsString()
    }
    protected fun finalize() {
        freeOpenGLTexture()
    }
    private fun freeOpenGLTexture() {
        if (glID < 0 ) return
        val tmp = IntArray(1)
        tmp[0] = glID
        GLES20.glDeleteTextures(1, tmp, 0)
        glID = -1
    }
    private fun drawAsString() {
        fun stringToBitmap(text: String) : Bitmap? {
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
            printerror("Ne peut générer de texture pour la string ${name}."); return
        }
        // 2. Création de la texture...
        val bitmap : Bitmap = stringToBitmap(if(name.isEmpty()) " " else name) ?: run {
            freeOpenGLTexture()
            printerror("Ne peut générer le bitmap pour la string ${name}.")
            return
        }
        // 3. Binding et parametres OpenGL...
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIDarrayTmp[0])
        val glFilter = if(asLinear) GLES20.GL_LINEAR else GLES20.GL_NEAREST
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
        // 4. Enregistrer l'ID de la texture OpenGL et ses info.
        glID = textureIDarrayTmp[0]
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        ratio = bitmap.width.toFloat() / bitmap.height.toFloat()

        bitmap.recycle()
    }
    private fun drawAsPng(resID: Int, context: Context) {
        // 0. Check si libre...
        if (glID >=0)
            freeOpenGLTexture()
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
        val bitmap = BitmapFactory.decodeResource(context.resources, resID, options)
        // 3. Binding et parametres OpenGL...
        val glFilter = if(asLinear) GLES20.GL_LINEAR else GLES20.GL_NEAREST
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
        // 4. Enregistrer l'ID de la texture OpenGL et ses info.
        glID = textureIDarrayTmp[0]
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        ratio = bitmap.width.toFloat() / bitmap.height.toFloat() *
                n.toFloat() / m.toFloat()
        bitmap.recycle()
    }

    companion object {
        /*-- Static fileds --*/
        var textSize = 48f
        var extraWidth = 0.15f
//        lateinit var defaultPng: Texture
//        lateinit var defaultString: Texture
//        lateinit var testFrame: Texture
        private var context: WeakReference<Context> = WeakReference(null)
        var loaded = false
        var isInit = false
        private val allStringTextures = mutableListOf<WeakReference<Texture> >()
        private val allConstantStringTextures = mutableMapOf<String, WeakReference<Texture> >()
        private val allLocalizedStringTextures = mutableMapOf<Int, WeakReference<Texture> >()
        private val allPngTextures = mutableMapOf<Int, WeakReference<Texture> >()
        private val defaultTiling = Tiling(1, 1)
        private var pngResIdToTiling = mutableMapOf(
                R.drawable.bar_in to defaultTiling,
                R.drawable.digits_black to Tiling(12, 2),
                R.drawable.disks to Tiling(4, 2),
                R.drawable.frame_mocha to defaultTiling,
                R.drawable.frame_white_back to defaultTiling,
                R.drawable.language_flags to Tiling(4, 4),
                R.drawable.scroll_bar_back to Tiling(1, 3),
                R.drawable.scroll_bar_front to Tiling(1, 3),
                R.drawable.some_animals to Tiling(4, 7),
                R.drawable.sparkle_stars to Tiling(3, 2),
                R.drawable.switch_back to defaultTiling,
                R.drawable.switch_front to defaultTiling,
                R.drawable.test_frame to Tiling(1, 1, false),
                R.drawable.the_cat to Tiling(1, 1, false),
        )

        /*-- Static methods --*/
        /** Préchargement des textures par défaut... */
        fun init(context: Context) {
            Texture.context = WeakReference(context)
            allPngTextures.clear()
            allConstantStringTextures.clear()
            allLocalizedStringTextures.clear()
            allStringTextures.clear()
            isInit = true
            loaded = true
        }
        fun suspend() {
            if(!loaded) {
                printwarning("Textures already unloaded.")
                return
            }
            printdebug("free all opengl textures")
            allStringTextures.strip()
            allStringTextures.forEach { weaktexture ->
                weaktexture.get()?.freeOpenGLTexture()
            }
            allPngTextures.strip()
            allPngTextures.forEach { (_, weaktexture) ->
                weaktexture.get()?.freeOpenGLTexture()
            }
            loaded = false
        }
        fun resume(context: Context) {
            if(!isInit) {
                printwarning("Texture pas init")
                return
            }
            if(loaded) {
                printwarning("Textures already loaded.")
                return
            }
            printdebug("restoring all opengl textures")
            allStringTextures.forEach { weaktexture ->
                weaktexture.get()?.drawAsString()
            }
            allPngTextures.forEach { (k, weaktexture) ->
                weaktexture.get()?.drawAsPng(k, context)
            }
            loaded = true
        }

        fun getConstantString(string: String) : Texture {
            allConstantStringTextures[string]?.get()?.let { texture ->
                return texture
            }
            val newCstStr = Texture(0, string, TextureType.ConstantString)
            allConstantStringTextures[string] = WeakReference(newCstStr)
            allStringTextures.add(WeakReference(newCstStr))
            return newCstStr
        }
        fun getNewMutableString(string: String = "") : Texture {
            val newMutStr = Texture(0, string, TextureType.MutableString)
            allStringTextures.add(WeakReference(newMutStr))
            return newMutStr
        }
        fun getLocalizedString(resId: Int) : Texture {
            allLocalizedStringTextures[resId]?.get()?.let { texture ->
                return texture
            }
            val ctx = context.get() ?: run {
                printerror("No context.")
                return getConstantString("\uD83E\uDD86")
            }
            val newLocStr = Texture(resId, ctx.getString(resId), TextureType.LocalizedString)
            allLocalizedStringTextures[resId] = WeakReference(newLocStr)
            allStringTextures.add(WeakReference(newLocStr))
            return newLocStr
        }
        // Pour changement de langue...
        fun updateAllLocalizedStrings() {
            val ctx = context.get() ?: run {
                printerror("No context."); return
            }
            allLocalizedStringTextures.forEach { (resId, wt) ->
                wt.get()?.let { tex ->
                    tex.name = ctx.getString(resId)
                    tex.drawAsString()
                } ?: run {
                    allLocalizedStringTextures.remove(resId)
                }
            }
        }
        fun getPng(resID: Int) : Texture {
            allPngTextures[resID]?.let { weak_tex ->
                weak_tex.get()?.let { texture ->
                    return texture
                } ?: run {
                    printdebug("Texture has been deallocated (redrawing...)")
                }
            }
            val newPng = Texture(resID, "png", TextureType.Png)
            allPngTextures[resID] = WeakReference(newPng)
            return newPng
        }
    }
}
