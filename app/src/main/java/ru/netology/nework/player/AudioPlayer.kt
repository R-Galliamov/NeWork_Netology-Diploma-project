package ru.netology.nework.player

import ru.netology.nework.dto.Attachment

interface AudioPlayer {
    fun isAudioPlaying(): Boolean
    fun getCurrentPosition(): Int
    fun getTracDuration(): Int
    fun play()
    fun pause()
    fun resume()
    fun stopAndReset()
    fun mediaPlayerDelegate(
        audio: Attachment,
        mediaId: Int,
        onComplete: () -> Unit
    )
}