package com.chummusbenshira.soundmachine.audio

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

interface AudioPlayer {
    fun setSource(resId: Int, playWhenReady: Boolean)
    fun release()
}

@OptIn(UnstableApi::class)
class ExoPlayerManager(private val context: Context) : AudioPlayer {
    private var exoPlayer: ExoPlayer? = ExoPlayer.Builder(context).build()
    private var currentResId: Int = 0
    private var loudnessEnhancer: LoudnessEnhancer? = null

    init {
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer?.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId == 0) return

                loudnessEnhancer?.release()

                try {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                        setTargetGain(1000)
                        enabled = true
                    }
                } catch (e: Exception) {
                    Log.e("ExoPlayerManager", "Failed to create LoudnessEnhancer", e)
                }
            }
        })
    }

    override fun setSource(resId: Int, playWhenReady: Boolean) {
        val isNewSource = currentResId != resId
        if (isNewSource) {
            currentResId = resId
            val uri = "android.resource://${context.packageName}/$resId"
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        }

        exoPlayer?.playWhenReady = playWhenReady
    }

    override fun release() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
