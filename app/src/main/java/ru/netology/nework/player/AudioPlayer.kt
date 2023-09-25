package ru.netology.nework.player

interface AudioPlayer {
    fun isAudioPlaying(): Boolean
    fun getCurrentPosition(): Int
    fun getTracDuration(): Int
    fun play()
    fun pause()
    fun resume()
    fun stopAndReset()
}