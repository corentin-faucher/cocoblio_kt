@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.coq.cocoblio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log
import com.coq.cocoblio.maths.printerror
import java.lang.Exception
import kotlin.math.*

object SoundManager {
    var isMute = false

    internal fun initWith(ctx: Context) {
        audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // 1. Init du "SoundPool"
        soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                .build()

        soundPool?.setOnLoadCompleteListener {
                _, sampleID, status ->
            if(status != 0) {
                Log.e("LOG", "Ne peut charger le son $sampleID")
            }
        }

        soundResIDToPoolID.clear()

        // 2. Chargement des sons de base
        initSound(R.raw.clap_clap, ctx)
        initSound(R.raw.duck_error, ctx)
        initSound(R.raw.go_start, ctx)
        initSound(R.raw.note_piano, ctx)
        initSound(R.raw.pouing_a, ctx)
        initSound(R.raw.pouing_b, ctx)
        initSound(R.raw.ready_set, ctx)
        initSound(R.raw.ship_horn, ctx)
        initSound(R.raw.tac_tac, ctx)
        initSound(R.raw.type_writter, ctx)
    }

    fun initSound(soundResID: Int, ctx: Context) {
        if(soundResIDToPoolID.containsKey(soundResID)) {
            printerror("$soundResID deja init"); return }
        soundResIDToPoolID[soundResID] = soundPool?.load(ctx, soundResID,1) ?:
                run{
                    printerror("Ne peut charger le son $soundResID"); -1}
    }
    fun getSoundPoolID(soundResID: Int) : Int {
        return soundResIDToPoolID[soundResID] ?: throw Exception("Son non chargé.")
    }

    private val soundResIDToPoolID = mutableMapOf<Int, Int>()

    fun play(soundPoolID: Int, pitch: Short = 0, volume: Float = 1f) {
        if (isMute) return
        if ( soundPoolID <0) {
            printerror("Son pas loadé."); return}

        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val currVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val usedVolume = max(1f, min(0f, volume)) * currVolume.toFloat() / maxVolume.toFloat()

        soundPool?.play(soundPoolID, usedVolume, usedVolume, 1, 0, 2f.pow(pitch.toFloat()/12f))
    }

    fun playWithResID(resID: Int, pitch: Short = 0, volume: Float = 1f) {
        soundResIDToPoolID[resID]?.let {soundPoolID ->
            play(soundPoolID, pitch, volume)
        } ?: run{ printerror("Son $resID non chargé.") }
    }

    private var soundPool: SoundPool? = null
    private var audioManager: AudioManager? = null
}