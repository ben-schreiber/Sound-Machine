package com.chummusbenshira.soundmachine.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chummusbenshira.soundmachine.R
import com.chummusbenshira.soundmachine.audio.AudioPlayer
import com.chummusbenshira.soundmachine.audio.ExoPlayerManager
import com.chummusbenshira.soundmachine.ui.theme.Brown
import com.chummusbenshira.soundmachine.ui.theme.FloralWhite
import com.chummusbenshira.soundmachine.ui.theme.Pink
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoiseInfo(
    val resId: Int,
    val baseColor: Color // Use Long to store ARGB color
)

data class SoundMachineUiState(
    val pages: List<NoiseInfo> = emptyList(),
    val isPlaying: Boolean = false,
    val showIndicator: Boolean = false,
    val currentPage: Int = 0
)

class SoundMachineViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer: AudioPlayer = ExoPlayerManager(application)

    private val _uiState = MutableStateFlow(SoundMachineUiState())
    val uiState: StateFlow<SoundMachineUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(pages = loadPages()) }
    }

    private fun loadPages(): List<NoiseInfo> {
        return listOf(
                NoiseInfo(R.raw.whitenoise, FloralWhite),
            NoiseInfo(R.raw.pinknoise, Pink),
            NoiseInfo(R.raw.brownnoise, Brown)
        )
    }

    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
        audioPlayer.setSource(
            resId = _uiState.value.pages[page].resId,
            playWhenReady = _uiState.value.isPlaying
        )
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
        audioPlayer.setSource(
            resId = _uiState.value.pages[_uiState.value.currentPage].resId,
            playWhenReady = isPlaying
        )
    }

    fun onIsScrolling(isScrolling: Boolean) {
        viewModelScope.launch {
            if (isScrolling) {
                _uiState.update { it.copy(showIndicator = true) }
            } else {
                delay(1000)
                _uiState.update { it.copy(showIndicator = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
