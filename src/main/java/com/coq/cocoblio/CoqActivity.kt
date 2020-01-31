package com.coq.cocoblio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.*
import com.coq.cocoblio.nodes.KeyboardKey
import com.coq.cocoblio.nodes.Node

/** Les events sont gérés par le renderer.... ? pas le choix ?
 * En effet, ils doivent être dans la thread d'OpenGL...
 */
abstract class CoqActivity(private val appThemeID: Int,
                           private val vertShaderID: Int?,
                           private val fragShaderID: Int?
) : Activity(), GestureDetector.OnGestureListener {

//    private lateinit var ge: GameEngineBase
//    abstract fun getGameEngine() : GameEngineBase
    abstract fun getEventstHandler() : EventsHandler
    abstract fun getStructureRoot() : Node?

    // La vue OpenGL avec (avec le renderer).
    private lateinit var view: CoqGLSurfaceView

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appThemeID)
        super.onCreate(savedInstanceState)

        gestureDetector = GestureDetector(this, this)
        gestureDetector.setIsLongpressEnabled(false)

        view = CoqGLSurfaceView(this)
        view.renderer = CoqRenderer(this, vertShaderID, fragShaderID)
        view.setRenderer(view.renderer)
        setContentView(view)

        SoundManager.initWith(this)
    }


    override fun onConfigurationChanged(newConfig: Configuration?) {
        with(view) {
            queueEvent {
                renderer.onConfigurationChanged()
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    /*-- Renvoie des events vers le renderer...??? --*/

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null)
            return super.onKeyDown(keyCode, event)
        if (event.repeatCount > 0)
            return super.onKeyDown(keyCode, event)
        with(view) {
            queueEvent {
                renderer.onKeyDown(object : KeyboardKey {
                    override val scancode = event.scanCode
                    override val keycode = keyCode
                    override val keymod = event.metaState
                    override val isVirtual = false
                })
            }
        }
        return if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null)
            return super.onKeyUp(keyCode, event)
        //println("keyUp $keyCode, sc: ${event.scanCode}, mod: ${event.metaState}, capslock: ${event.isCapsLockOn}")
        with(view) {
            queueEvent {
                renderer.onKeyUp(object : KeyboardKey {
                    override val scancode = event.scanCode
                    override val keycode = keyCode
                    override val keymod = event.metaState
                    override val isVirtual = false
                })
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onFling(event1: MotionEvent, event2: MotionEvent,
                         velocityX: Float, velocityY: Float): Boolean {
        with(view) {
            queueEvent {
                renderer.onTouchUp(CoqRenderer.getPositionFrom(velocityX, velocityY, true))
            }
        }
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onLongPress(event: MotionEvent) {
    }

    override fun onScroll(eFirst: MotionEvent, eNow: MotionEvent,
                          distanceX: Float, distanceY: Float): Boolean {
        with(view) {
            queueEvent {
                renderer.onTouchDrag(CoqRenderer.getPositionFrom(eFirst.x, eFirst.y, true),
                    CoqRenderer.getPositionFrom(eNow.x, eNow.y, true))
            }
        }
        return true
    }

    override fun onShowPress(event: MotionEvent) {
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        with(view) {
            queueEvent {
                renderer.onSingleTap(CoqRenderer.getPositionFrom(event.x, event.y, true))
            }
        }
        return true
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!gestureDetector.onTouchEvent(event) && (event?.action == MotionEvent.ACTION_UP)) {
            with(view) {
                queueEvent {
                    renderer.onTouchUp(null)
                }
            }

        }
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        view.onPause()
        with(view) {
            queueEvent {
                renderer.onPause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }
}

@SuppressLint("ViewConstructor")
class CoqGLSurfaceView(main: CoqActivity) : GLSurfaceView(main) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
    }

    lateinit var renderer: CoqRenderer
}
