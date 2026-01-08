package com.chummusbenshira.soundmachine

import android.content.Context
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.chummusbenshira.soundmachine.ui.theme.SoundMachineTheme
import kotlinx.coroutines.delay

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
@androidx.annotation.OptIn(UnstableApi::class)
fun SoundMachineApp() {
    val pages = remember {
        listOf(
            NoisePage(R.raw.whitenoise, Color.White),
            NoisePage(R.raw.pinknoise, Color(0xFFFFC0CB)), // Pink
            NoisePage(R.raw.brownnoise, Color(0xFF795548))  // Brown
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    var isPlaying by remember { mutableStateOf(false) }
    var showIndicator by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // ExoPlayer is much better at gapless looping than MediaPlayer
    val exoPlayerManager = remember {
        ExoPlayerManager(context)
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
        exoPlayerManager.setSource(resId)
        if (isPlaying) {
            exoPlayerManager.play()
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            exoPlayerManager.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
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
                            exoPlayerManager.play()
                        } else {
                            exoPlayerManager.pause()
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

// Wrapper for ExoPlayer to handle looping and source switching
@UnstableApi 
class ExoPlayerManager(private val context: Context) {
    private var exoPlayer: ExoPlayer? = ExoPlayer.Builder(context).build()
    private var currentResId: Int = 0

    init {
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
    }

    fun setSource(resId: Int) {
        if (currentResId == resId) return
        currentResId = resId
        
        // Construct the raw resource URI
        val uri = "android.resource://${context.packageName}/$resId"
        val mediaItem = MediaItem.fromUri(uri)
        
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
    }

    fun play() {
        if (currentResId == 0) return
        if (exoPlayer?.playbackState == Player.STATE_IDLE) {
            exoPlayer?.prepare()
        }
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
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
