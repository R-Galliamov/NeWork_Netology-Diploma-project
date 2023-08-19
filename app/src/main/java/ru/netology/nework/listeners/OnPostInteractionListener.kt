package ru.netology.nework.listeners

import android.widget.VideoView
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post

interface OnPostInteractionListener {
    fun onLike(post: Post)
    fun onLikeLongClick(usersIdsList: List<Int>)
    fun onUser(userId: Int)
    fun onContent(post: Post)
    fun onLink(url: String)
    fun onImage()
    fun onVideo(videoView: VideoView, video: Attachment)
    fun isVideoPlaying(): Boolean
    fun onAudio(audio: Attachment, postId: Int)
    fun isAudioPlaying(): Boolean

}