package com.elwataniatv.app.service

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BackgroundPlayerManager {
    var activePlayer: ExoPlayer? = null
        set(value) {
            field = value
            _isPlaying.value = value?.isPlaying == true
        }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun updatePlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun pause() {
        activePlayer?.pause()
        _isPlaying.value = false
    }

    fun play() {
        activePlayer?.play()
        _isPlaying.value = true
    }

    fun stopAndRelease() {
        activePlayer?.stop()
        activePlayer?.release()
        activePlayer = null
        _isPlaying.value = false
    }
}
