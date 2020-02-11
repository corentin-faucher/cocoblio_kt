package com.coq.cocoblio


import android.app.Activity
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.*
import com.coq.cocoblio.nodes.KeyboardKey

/** Les events sont gérés par le renderer.... ? pas le choix ?
 * En effet, ils doivent être dans la thread d'OpenGL...
 */
abstract class CoqActivity(private val appThemeID: Int,
                           private val vertShaderID: Int?,
                           private val fragShaderID: Int?
) : Activity(), GestureDetector.OnGestureListener {

    protected lateinit var renderer: Renderer
    // Workaround... Solution ???
    abstract fun getGameEngine(): GameEngineBase

    private lateinit var view: GLSurfaceView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appThemeID)
        super.onCreate(savedInstanceState)

        gestureDetector = GestureDetector(this, this)
        gestureDetector.setIsLongpressEnabled(false)

        view = GLSurfaceView(this)
        view.setEGLContextClientVersion(2)
        view.preserveEGLContextOnPause = true
        renderer = Renderer(this, vertShaderID, fragShaderID)
        view.setRenderer(renderer)

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
                renderer.onTouchUp(velocityX, velocityY)
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
                renderer.onTouchDrag(eFirst.x, eFirst.y, eNow.x, eNow.y)
            }
        }
        return true
    }

    override fun onShowPress(event: MotionEvent) {
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        with(view) {
            queueEvent {
                renderer.onSingleTap(event.x, event.y)
            }
        }
        return true
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!gestureDetector.onTouchEvent(event) && (event?.action == MotionEvent.ACTION_UP)) {
            with(view) {
                queueEvent {
                    renderer.onTouchUp(null, null)
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

