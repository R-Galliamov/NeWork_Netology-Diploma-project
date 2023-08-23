package ru.netology.nework.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.dto.Attachment
import java.lang.NullPointerException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoLifecycleObserver @Inject constructor(private val context: Context) :
    LifecycleEventObserver, VideoPlayer {

    private var player: ExoPlayer? = null
    val isPlaying get() = player?.isPlaying ?: false

    private var mediaSource: ProgressiveMediaSource? = null
    private var mediaId = -1
    private var view: PlayerView? = null

    init {
        initPlayer()
    }

    override fun getSettledVideoId(): Int = mediaId

    override fun isVideoPlaying(): Boolean = isPlaying

    private fun initPlayer() {
        player = SimpleExoPlayer.Builder(context).build()
    }

    override fun attachView(view: PlayerView) {
        view.player = player
        this.view = view
    }

    private fun setSource(url: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_MP4)
            .build()

        mediaSource = ProgressiveMediaSource.Factory(
            DefaultDataSource.Factory(context)
        )
            .createMediaSource(mediaItem)
    }

    private fun setupPlayer(mediaId: Int) {
        player?.let { exoPlayer ->
            mediaSource?.let { source ->
                exoPlayer.setMediaSource(source)
                exoPlayer.playWhenReady = true
                exoPlayer.seekTo(0, 0L)
                exoPlayer.prepare()
                this.mediaId = mediaId
            } ?: throw NullPointerException("Media source must be initialized")
        } ?: throw NullPointerException("Player must be initialized")
    }

    fun videoPlayerDelegate(view: PlayerView, media: Attachment, mediaId: Int = -2) {
        if (this.mediaId != mediaId) {
            this.mediaId = mediaId
            this.view?.let { detachView(it) }
            player ?: initPlayer()
            setSource(media.url)
            attachView(view)
            setupPlayer(mediaId)
        } else {
            if (isPlaying) {
                player?.pause()
            } else {
                player?.play()
            }
        }
    }

    override fun pause() {
        player?.pause() ?: throw NullPointerException("Player must be initialized")
    }

    override fun play() {
        player?.play() ?: throw NullPointerException("Player must be initialized")
    }

    private fun detachView(view: PlayerView) {
        view.player = null
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> player?.pause()
            Lifecycle.Event.ON_STOP -> {
                mediaId = -1
                mediaSource = null
                player = null
                this.view?.let { detachView(it) }
            }

            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            else -> Unit
        }
    }
}