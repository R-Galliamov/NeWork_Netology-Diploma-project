package ru.netology.nework.listeners

import android.widget.VideoView
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event

interface OnEventInteractionListener {
    fun onLike(event: Event)
    fun onLikeLongClick(usersIdsList: List<Int>)
    fun onUser(userId: Int)
    fun onContent(event: Event)
    fun onLink(url: String)
    fun onImage()
    fun onVideo(videoView: VideoView, video: Attachment)
    fun isVideoPlaying(): Boolean
    fun onAudio(audio: Attachment, eventId: Int)
    fun isAudioPlaying(): Boolean
}