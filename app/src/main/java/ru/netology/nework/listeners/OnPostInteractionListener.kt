package ru.netology.nework.listeners

import com.google.android.exoplayer2.ui.PlayerView
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post

interface OnPostInteractionListener {
    fun onLike(post: Post)
    fun onLikeLongClick(usersIdsList: List<Int>)
    fun onUser(userId: Int)
    fun onContent(post: Post)
    fun onLink(url: String)
    fun onImage()
    fun onVideo(playerView: PlayerView, video: Attachment, postId: Int)
    fun onAudio(audio: Attachment, postId: Int)
}