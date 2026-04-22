package com.chummusbenshira.soundmachine.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chummusbenshira.soundmachine.R
import com.chummusbenshira.soundmachine.audio.AudioPlayer
import com.chummusbenshira.soundmachine.audio.MediaControllerManager
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
    val baseColor: Color
)

data class SoundMachineUiState(
    val pages: List<NoiseInfo> = emptyList(),
    val isPlaying: Boolean = false,
    val showIndicator: Boolean = false,
    val initialPage: Int = 0,
    val currentPage: Int = 0
)

class SoundMachineViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("sound_machine_prefs", Context.MODE_PRIVATE)
    private val audioPlayer: AudioPlayer = MediaControllerManager(application)

    private val _uiState = MutableStateFlow(SoundMachineUiState())
    val uiState: StateFlow<SoundMachineUiState> = _uiState.asStateFlow()

    init {
        val savedPage = prefs.getInt("last_page", 0)
        val pages = loadPages()
        val initialPage = savedPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        
        _uiState.update { 
            it.copy(
                pages = pages,
                initialPage = initialPage,
                currentPage = initialPage
            ) 
        }

        // Set the initial source so the player is ready at the last used page
        audioPlayer.setSource(
            resId = pages[initialPage].resId,
            playWhenReady = false
        )
        
        audioPlayer.setOnIsPlayingChangedListener { isPlaying ->
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }
    }

    private fun loadPages(): List<NoiseInfo> {
        return listOf(
            NoiseInfo(R.raw.whitenoise, FloralWhite),
            NoiseInfo(R.raw.pinknoise, Pink),
            NoiseInfo(R.raw.brownnoise, Brown)
        )
    }

    fun onPageChanged(page: Int) {
        // We still want to allow the update if it's the first time or a real change.
        // The UI might call this with the initialPage on first composition.
        val oldPage = _uiState.value.currentPage
        
        _uiState.update { it.copy(currentPage = page) }
        prefs.edit { putInt("last_page", page) }
        
        if (oldPage != page || page == _uiState.value.initialPage) {
            audioPlayer.setSource(
                resId = _uiState.value.pages[page].resId,
                playWhenReady = _uiState.value.isPlaying
            )
        }
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
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
