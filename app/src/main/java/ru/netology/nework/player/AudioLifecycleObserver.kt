package ru.netology.nework.player

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Attachment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioLifecycleObserver @Inject constructor() : LifecycleEventObserver, AudioPlayer {
    private var player: MediaPlayer? = null
    var isPlaying = false
    var isSet = false
    var mediaId = -1

    private val DEFAULT_POST_ID = -2

    private var trackDuration: Int = 0
    private var currentPosition: Int = 0
    private var progressJob: Job? = null

    override fun getCurrentPosition(): Int {
        return currentPosition
    }

    override fun getTracDuration(): Int {
        return trackDuration
    }

    private fun initPlayer() {
        if (player == null) {
            player = MediaPlayer()
        }
    }

    override fun isAudioPlaying(): Boolean = isPlaying
    override fun play() {
        isPlaying = true
        player?.prepareAsync()
        player?.setOnPreparedListener {
            isSet = true
            trackDuration = it.duration
            it.start()

            progressJob = GlobalScope.launch(Dispatchers.IO) {
                while (isPlaying) {
                    currentPosition = player?.currentPosition ?: 0
                    delay(1000)
                }
            }
        }
    }

    override fun pause() {
        isPlaying = false
        player?.pause()
        progressJob?.cancel()
    }

    override fun resume() {
        isPlaying = true
        player?.start()
        startProgressJob()
    }

    override fun stopAndReset() {
        isPlaying = false
        player?.stop()
        player?.reset()
        isSet = false
        mediaId = -1
        progressJob?.cancel()
    }

    private fun startProgressJob() {
        progressJob = GlobalScope.launch(Dispatchers.IO) {
            while (isPlaying) {
                currentPosition = player?.currentPosition ?: 0
                delay(100)
            }
        }
    }

    fun mediaPlayerDelegate(
        audio: Attachment,
        mediaId: Int = DEFAULT_POST_ID,
        onComplete: () -> Unit,
    ) {
        if (this.mediaId != mediaId) {
            this.mediaId = mediaId
            if (isSet) {
                stopAndReset()
            }
            initPlayer()
            player?.setDataSource(audio.url)
            player?.setOnCompletionListener {
                this.mediaId = -1
                stopAndReset()
                onComplete()
            }
            isSet = true
            play()
        } else {
            if (isPlaying) {
                pause()
            } else {
                resume()
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_STOP -> {
                stopAndReset()
                player = null
            }

            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            else -> Unit
        }
    }
}