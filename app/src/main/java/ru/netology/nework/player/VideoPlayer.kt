package ru.netology.nework.player

import com.google.android.exoplayer2.ui.PlayerView

interface VideoPlayer {
    fun play()
    fun pause()
    fun attachView(playerView: PlayerView)
    fun isVideoPlaying(): Boolean
    fun getSettledVideoId(): Int
}