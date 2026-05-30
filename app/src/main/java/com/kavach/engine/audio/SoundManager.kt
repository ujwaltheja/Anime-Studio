package com.kavach.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * Short sound-effect player backed by [SoundPool].
 * Load sounds on the main thread before play calls.
 */
class SoundManager(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = mutableMapOf<String, Int>()

    /** Load a sound from assets. Returns the asset path as key. */
    fun load(context: Context, assetPath: String): String {
        return try {
            val fd = context.assets.openFd(assetPath)
            val id = pool.load(fd, 1)
            fd.close()
            ids[assetPath] = id
            assetPath
        } catch (e: Exception) {
            Log.e("SoundManager", "Could not load $assetPath: ${e.message}")
            assetPath
        }
    }

    /** Play a previously loaded sound. */
    fun play(key: String, leftVol: Float = 1f, rightVol: Float = 1f, pitch: Float = 1f) {
        ids[key]?.let { pool.play(it, leftVol, rightVol, 1, 0, pitch) }
    }

    fun release() {
        pool.release()
        ids.clear()
    }
}
