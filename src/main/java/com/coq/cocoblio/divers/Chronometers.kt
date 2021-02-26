@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.divers

object GlobalChrono {
    var elapsedMS: Long = 0L
        private set
    /** Active/désactive le chrono du temps écoulé.
     * Setter à true a pour effet de "toucher" le chrono (évite de s'endormir). */
    var isPaused: Boolean
        set(newValue) {
            if (!newValue) { // isPaused == false -> isActive == true
                isActive = true
                touchTime = startingTime + elapsedMS
                touchElapsed = 0
                if (!isActive) { justUnpause = true }
            } else {
                isActive = false
            }
        }
        get() = !isActive

    val shouldSleep: Boolean
        get() = touchElapsed > 10000L
    val elapsedSec: Float
        get() = elapsedMS.toFloat() / 1000.0f

    val elapsedMS16: Short
        get() = elapsedMS.toShort()

    val elapsedMS32: Int
        get() = elapsedMS.toInt()


    fun update() {
        if (startingTime == 0L) { // Pas init...
            startingTime = systemTime
            touchTime = startingTime
            isActive = true
        }
        if (justUnpause) {
            startingTime = systemTime - elapsedMS
            touchTime = systemTime - touchElapsed
            justUnpause = false
            return
        }
        if (isActive) {
            // Cas patologique où il n'y a pas eu de mise à jour depuis longtemps...
            if (systemTime - startingTime - elapsedMS > 500) {
                startingTime = systemTime - elapsedMS - 500 // Interval plafonne à 500
                touchTime = systemTime - touchElapsed - 500
            }
            elapsedMS = systemTime - startingTime
            touchElapsed = systemTime - touchTime
        }
    }

    // Membres privés
    private var isActive: Boolean = false
    private var startingTime: Long = 0
    private var touchTime: Long = 0
    private var touchElapsed: Long = 0
    private var justUnpause: Boolean = false
    private val systemTime: Long
        get() = System.currentTimeMillis()
}

class Chrono {
    /** Le chronomètre est activé. */
    var isActive: Boolean = false
        private set
    /** Le temps écoulé depuis "start()" en millisec. */
    val elapsedMS: Long
        get() = if(isActive) (GlobalChrono.elapsedMS - time) else time
    /** Le temps écoulé depuis "start()" en secondes. */
    val elapsedSec: Float
        get() = elapsedMS.toFloat() / 1000f
    /** Le temps global où le chrono a commencé (en millisec). */
    val startTimeMS: Long
        get() = if(isActive) time else GlobalChrono.elapsedMS - time

    fun start() {
        time = GlobalChrono.elapsedMS
        isActive = true
    }
    fun stop() {
        isActive = false
        time = 0
    }
    fun pause() {
        time = elapsedMS
        isActive = false
    }
    fun unpause() {
        time = startTimeMS
        isActive = true
    }
    fun addMS(millisec: Long) {
        if (isActive) {
            time -= millisec
        } else {
            time += millisec
        }
    }
    fun addSec(sec: Float) {
        if (sec > 0f)
            addMS((sec*1000f).toLong())
    }

    fun removeMS(millisec: Long) {
        time = if (isActive) { // time est le starting time.
            if (elapsedMS > millisec) time + millisec
            else GlobalChrono.elapsedMS
        } else { // time est le temps écoulé.
            if (time > millisec) time - millisec else 0
        }
    }
    fun removeSec(sec: Float) {
        if (sec > 0f)
            removeMS((sec*1000f).toLong())
    }

    // Membres privés
    private var time: Long = 0L
}

class CountDown(var ringTimeMS: Long) {
    var isActive: Boolean = false
        private set

    val isRinging: Boolean
        get() {
            return if (isActive) {
                ((GlobalChrono.elapsedMS - time) > ringTimeMS)
            } else {
                (time > ringTimeMS)
            }
        }

    var ringTimeSec: Float
        get() = ringTimeMS.toFloat() / 1000.0f
        set(newRingTimeSec) {
            ringTimeMS = (newRingTimeSec * 1000.0f).toLong()
        }

    val elapsedMS64: Long
        get() = if(isActive) (GlobalChrono.elapsedMS - time) else time

    val remainingMS: Long
        get() {
            val elapsed = elapsedMS64
            return if (elapsed > ringTimeMS) 0 else (ringTimeMS - elapsed)
        }

    val remainingSec: Float
        get() = remainingMS.toFloat() / 1000.0f


    constructor(ringSec: Float) : this(
        if(ringSec <0) 0L else (ringSec * 1000.0f).toLong())

    fun start() {
        time = GlobalChrono.elapsedMS
        isActive = true
    }
    fun stop() {
        isActive = false
        time = 0
    }

    // Membres privés
    private var time: Long = 0
}

class SmallChrono {
    val elapsedMS16: Short
        get() = (GlobalChrono.elapsedMS16 - startTime).toShort()

    val elapsedSec: Float
        get() =(GlobalChrono.elapsedMS16 - startTime).toFloat()/1000.0f

    fun start() {
        startTime = GlobalChrono.elapsedMS16
    }
    fun setElapsedTo(newTimeMS: Int) {
        startTime = (GlobalChrono.elapsedMS - newTimeMS).toShort()
    }

    // Membre privé
    private var startTime: Short = 0
}
