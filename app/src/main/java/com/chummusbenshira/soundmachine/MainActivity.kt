package com.chummusbenshira.soundmachine

import android.content.Context
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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
            NoisePage(R.raw.whitenoise, Color(0xFFFFFAF0)), // Floral White
            NoisePage(R.raw.pinknoise, Color(0xFFFFC0CB)), // Pink
            NoisePage(R.raw.brownnoise, Color(0xFF795548))  // Brown
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    var isPlaying by remember { mutableStateOf(false) }
    var showIndicator by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
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
    LaunchedEffect(pagerState.currentPage, isPlaying) {
        val resId = pages[pagerState.currentPage].resId
        exoPlayerManager.setSource(resId, playWhenReady = isPlaying)
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
            // Layer 1: Background Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pages[pageIndex].color)
                )
            }

            // Layer 2: Static Button Overlay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp
                val screenHeight = configuration.screenHeightDp.dp
                val buttonSize = min(screenWidth, screenHeight) * 0.8f

                val currentBackgroundColor = pages[pagerState.currentPage].color
                val borderColor = if (currentBackgroundColor.luminance() > 0.5f) Color.Black else Color.White

                val buttonFillColor = if (isPlaying) {
                    Color.White.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(buttonFillColor)
                        .border(width = 4.dp, color = borderColor, shape = CircleShape)
                        .clickable { isPlaying = !isPlaying }
                )
            }

            // Layer 3: Indicator
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
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
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

@UnstableApi 
class ExoPlayerManager(private val context: Context) {
    private var exoPlayer: ExoPlayer? = ExoPlayer.Builder(context).build()
    private var currentResId: Int = 0

    init {
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
    }

    fun setSource(resId: Int, playWhenReady: Boolean) {
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

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}

fun Color.luminance(): Float {
    return (0.299 * red + 0.587 * green + 0.114 * blue).toFloat()
}
