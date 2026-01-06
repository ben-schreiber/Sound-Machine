package com.chummusbenshira.soundmachine

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chummusbenshira.soundmachine.ui.theme.SoundMachineTheme
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundMachineTheme {
                SoundMachineApp()
            }
        }
    }
}

@Composable
fun SoundMachineApp() {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                val afd = context.resources.openRawResourceFd(R.raw.whitenoise)
                if (afd == null) {
                    Log.e("SoundMachine", "Resource not found")
                } else {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    isLooping = true
                    Log.d("SoundMachine", "MediaPlayer initialized successfully")
                }
            } catch (e: IOException) {
                Log.e("SoundMachine", "Error setting data source", e)
            } catch (e: Exception) {
                Log.e("SoundMachine", "Error initializing MediaPlayer", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    // Colors
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer
    
    // When ON, button should be a shade lighter.
    val buttonColor = if (isPlaying) {
        Color.White.copy(alpha = 0.3f).compositeOver(backgroundColor)
    } else {
        backgroundColor
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val buttonSize = screenWidth * 0.8f

            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .border(width = 4.dp, color = MaterialTheme.colorScheme.onPrimaryContainer, shape = CircleShape)
                    .clickable {
                        isPlaying = !isPlaying
                        if (isPlaying) {
                            try {
                                if (!mediaPlayer.isPlaying) {
                                    mediaPlayer.start()
                                    Log.d("SoundMachine", "Started playback")
                                }
                            } catch (e: IllegalStateException) {
                                Log.e("SoundMachine", "Failed to start playback", e)
                            }
                        } else {
                            try {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                    Log.d("SoundMachine", "Paused playback")
                                }
                            } catch (e: IllegalStateException) {
                                Log.e("SoundMachine", "Failed to pause playback", e)
                            }
                        }
                    }
            )
        }
    }
}
