package ru.netology.nework.service

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.lifecycle.MutableLiveData
import ru.netology.nework.dto.Attachment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoPlayer @Inject constructor() {

    var isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)

    private fun playVideo(videoView: VideoView, media: Attachment, onComplete: () -> Unit) {
        videoView.apply {
            setMediaController(MediaController(videoView.context))
            setVideoURI(Uri.parse(media.url))
            setOnPreparedListener {
                this@VideoPlayer.isPlaying.value = true
                start()
            }
            setOnCompletionListener {
                stop(videoView)
                onComplete()
            }
        }
    }

    private fun stop(videoView: VideoView) {
        videoView.stopPlayback()
        isPlaying.value = false
    }

    fun videoPlayerDelegate(videoView: VideoView, media: Attachment, onComplete: () -> Unit) {
        if (isPlaying.value == true) {
            stop(videoView)
        } else {
            playVideo(videoView, media, onComplete)
        }
    }
}