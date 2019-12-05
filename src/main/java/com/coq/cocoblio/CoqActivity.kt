package com.coq.cocoblio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.*

/** Les events (et donc le gameEngine) sont gérés par le renderer.
 * En effet, ils doivent être dans la thread d'OpenGL...
 */
abstract class CoqActivity(private val appThemeID: Int, private val vertShaderID: Int?, private val fragShaderID: Int?)
    : Activity(), GestureDetector.OnGestureListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appThemeID)
        super.onCreate(savedInstanceState)
        //val conf = resources.configuration

        gestureDetector = GestureDetector(this, this)
//        gestureDetector.setOnDoubleTapListener(this)
        gestureDetector.setIsLongpressEnabled(false)

        glView = CoqGLSurfaceView(this)
        glView.renderer = CoqRenderer(this, vertShaderID, fragShaderID)
        glView.setRenderer(glView.renderer)
        setContentView(glView)

        SoundManager.initWith(this)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        with(glView) {
            queueEvent {
                renderer.onConfigurationChanged()
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(event == null)
            return super.onKeyDown(keyCode, event)
        if (event.repeatCount > 0)
            return super.onKeyDown(keyCode, event)
        with(glView) {
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
        with(glView) {
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
        with(glView) {
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
        with(glView) {
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
        with(glView) {
            queueEvent {
                renderer.onSingleTap(CoqRenderer.getPositionFrom(event.x, event.y, true))
            }
        }
        return true
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!gestureDetector.onTouchEvent(event) && (event?.action == MotionEvent.ACTION_UP)) {
            with(glView) {
                queueEvent {
                    renderer.onTouchUp(null)
                }
            }

        }
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
        with(glView) {
            queueEvent {
                renderer.onPause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    abstract fun getGameEngine() : GameEngineBase?

    private lateinit var gestureDetector: GestureDetector
    private lateinit var glView: CoqGLSurfaceView
}

@SuppressLint("ViewConstructor")
class CoqGLSurfaceView(main: CoqActivity) : GLSurfaceView(main) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
    }


    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateContextMenu(menu: ContextMenu?) {
        super.onCreateContextMenu(menu)
    }

    lateinit var renderer: CoqRenderer
}
