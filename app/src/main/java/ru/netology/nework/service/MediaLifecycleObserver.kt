package ru.netology.nework.service

import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Attachment

class MediaLifecycleObserver : LifecycleEventObserver {
    private var player: MediaPlayer? = MediaPlayer()
   var isPlaying = false
   var isSet = false
   var trackPostOrEventId = -1

    private var trackDuration: Int = 0
    private var currentPosition: Int = 0
    private var progressJob: Job? = null

    fun getCurrentPosition(): Int {
        return currentPosition
    }

    fun getTracDuration(): Int {
        return trackDuration
    }

    private fun play() {
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

    private fun pause() {
        isPlaying = false
        player?.pause()
        progressJob?.cancel()
    }

    private fun resume() {
        isPlaying = true
        player?.start()
        startProgressJob()
    }

    private fun stopAndReset() {
        isPlaying = false
        player?.stop()
        player?.reset()
        isSet = false
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
        postOrEventId: Int,
        onComplete: () -> Unit
    ) {
        if (trackPostOrEventId != postOrEventId) {
            trackPostOrEventId = postOrEventId
            if (isSet) {
                stopAndReset()
            }
            player?.setDataSource(audio.url)
            player?.setOnCompletionListener {
                trackPostOrEventId = -1
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
                player?.release()
                player = null
            }

            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            else -> Unit
        }
    }
}