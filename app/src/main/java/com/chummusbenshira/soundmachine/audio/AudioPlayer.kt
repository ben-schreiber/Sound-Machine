package com.chummusbenshira.soundmachine.audio

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

interface AudioPlayer {
    fun setSource(resId: Int, playWhenReady: Boolean)
    fun release()
}

@OptIn(UnstableApi::class)
class MediaControllerManager(
    private val context: Context
) : AudioPlayer {
    private var mediaController: MediaController? = null
    private var currentResId: Int = 0
    private var pendingPlayWhenReady: Boolean? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                mediaController = controller
                controller.repeatMode = Player.REPEAT_MODE_ONE
                
                // If setSource was called before controller was ready, apply it now
                if (currentResId != 0) {
                    val uri = "android.resource://${context.packageName}/$currentResId"
                    controller.setMediaItem(MediaItem.fromUri(uri))
                    controller.prepare()
                    pendingPlayWhenReady?.let {
                        if (it) controller.play() else controller.pause()
                    }
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun setSource(resId: Int, playWhenReady: Boolean) {
        val isNewSource = currentResId != resId
        currentResId = resId
        pendingPlayWhenReady = playWhenReady

        val controller = mediaController
        if (controller != null) {
            if (isNewSource) {
                val uri = "android.resource://${context.packageName}/$resId"
                val mediaItem = MediaItem.fromUri(uri)
                controller.setMediaItem(mediaItem)
                controller.prepare()
            }

            if (playWhenReady) {
                controller.play()
            } else {
                controller.pause()
            }
        }
    }

    override fun release() {
        mediaController?.release()
        mediaController = null
    }
}
