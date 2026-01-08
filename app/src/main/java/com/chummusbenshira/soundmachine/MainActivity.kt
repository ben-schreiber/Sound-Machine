package com.chummusbenshira.soundmachine

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
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

data class NoisePage(
    val resId: Int,
    val color: Color
)

@Composable
fun SoundMachineApp() {
    val pages = remember {
        listOf(
            NoisePage(R.raw.whitenoise2, Color.White),
            NoisePage(R.raw.pinknoise, Color(0xFFFFC0CB)), // Pink
            NoisePage(R.raw.brownnoise, Color(0xFF795548))  // Brown
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    var isPlaying by remember { mutableStateOf(false) }
    var showIndicator by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    // Handle visibility of indicator
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            showIndicator = true
        } else {
            delay(1000)
            showIndicator = false
        }
    }

    // Handle track switching
    LaunchedEffect(pagerState.currentPage) {
        val resId = pages[pagerState.currentPage].resId
        try {
            mediaPlayer.reset()
            val afd = context.resources.openRawResourceFd(resId)
            if (afd != null) {
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer.prepare()
                mediaPlayer.isLooping = true
                if (isPlaying) {
                    mediaPlayer.start()
                }
            }
        } catch (e: IOException) {
            Log.e("SoundMachine", "Error switching track", e)
        } catch (e: Exception) {
            Log.e("SoundMachine", "Error initializing track", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Placeholder, covered by Pager
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val page = pages[pageIndex]
                NoiseScreen(
                    backgroundColor = page.color,
                    isPlaying = isPlaying,
                    onToggle = {
                        isPlaying = !isPlaying
                        if (isPlaying) {
                            if (!mediaPlayer.isPlaying) {
                                try {
                                    mediaPlayer.start()
                                } catch (e: Exception) {
                                    Log.e("SoundMachine", "Error starting playback", e)
                                }
                            }
                        } else {
                            if (mediaPlayer.isPlaying) {
                                try {
                                    mediaPlayer.pause()
                                } catch (e: Exception) {
                                    Log.e("SoundMachine", "Error pausing playback", e)
                                }
                            }
                        }
                    }
                )
            }

            // Indicator
            AnimatedVisibility(
                visible = showIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pages.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) 
                                MaterialTheme.colorScheme.onSecondaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoiseScreen(
    backgroundColor: Color,
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    // When ON, button should be a shade lighter.
    val buttonColor = if (isPlaying) {
        Color.White.copy(alpha = 0.3f).compositeOver(backgroundColor)
    } else {
        backgroundColor
    }
    
    // For border, if background is very light, use dark border. If dark, use light.
    // Or stick to previous logic: MaterialTheme.colorScheme.onPrimaryContainer
    // Since background is custom now (White, Pink, Brown), we need a contrasting border.
    // Let's calculate contrast or just use Black/White based on background brightness.
    // But previous logic used `MaterialTheme.colorScheme.onPrimaryContainer`.
    // Let's try to maintain good visibility.
    // Simple heuristic:
    val borderColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val buttonSize = screenWidth * 0.8f

        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(buttonColor)
                .border(width = 4.dp, color = borderColor, shape = CircleShape)
                .clickable { onToggle() }
        )
    }
}

// Helper to check brightness
fun Color.luminance(): Float {
    return (0.299 * red + 0.587 * green + 0.114 * blue).toFloat()
}
