package com.kavach.engine.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/** Background music player backed by [MediaPlayer]. */
class MusicManager(private val context: Context) {

    private var player: MediaPlayer? = null

    fun play(assetPath: String, loop: Boolean = true) {
        stop()
        try {
            player = MediaPlayer().apply {
                val fd = context.assets.openFd(assetPath)
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                isLooping = loop
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("MusicManager", "Could not play $assetPath: ${e.message}")
        }
    }

    fun pause()              = player?.pause()
    fun resume()             = player?.start()
    fun setVolume(v: Float)  = player?.setVolume(v, v)
    fun isPlaying()          = player?.isPlaying == true

    fun stop() {
        player?.apply { if (isPlaying) stop(); release() }
        player = null
    }
}
