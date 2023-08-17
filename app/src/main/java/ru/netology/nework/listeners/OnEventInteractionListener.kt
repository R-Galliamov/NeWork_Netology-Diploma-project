package ru.netology.nework.listeners

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.service.MediaLifecycleObserver

interface OnEventInteractionListener {
    fun onLike(event: Event)
    fun onLikeLongClick(usersIdsList: List<Int>)
    fun onUser(userId: Int)
    fun onContent(event: Event)
    fun onLink(url: String)
    fun onImage()
    fun onVideo()
    fun onAudio(audio: Attachment, eventId: Int)
    fun isAudioPlaying(): Boolean
}