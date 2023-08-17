package ru.netology.nework.listeners

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.service.MediaLifecycleObserver

interface OnPostInteractionListener {
    fun onLike(post: Post)
    fun onLikeLongClick(usersIdsList: List<Int>)
    fun onUser(userId: Int)
    fun onContent(post: Post)
    fun onLink(url: String)
    fun onImage()
    fun onVideo()
    fun onAudio(audio: Attachment, postId: Int)
    fun isAudioPlaying(): Boolean
}