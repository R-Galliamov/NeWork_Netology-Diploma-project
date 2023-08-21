package ru.netology.nework.listeners

import com.google.android.exoplayer2.ui.PlayerView
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event

interface OnEventInteractionListener {
    fun onLike(event: Event)
    fun onLikeLongClick(usersIdsList: List<Int>)
    fun onUser(userId: Int)
    fun onContent(event: Event)
    fun onLink(url: String)
    fun onImage()
    fun onVideo(playerView: PlayerView, video: Attachment, eventId: Int)
    fun onAudio(audio: Attachment, eventId: Int)
}