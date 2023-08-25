package ru.netology.nework.adapter

import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.EventItemBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.listeners.OnEventInteractionListener
import ru.netology.nework.player.AudioPlayer
import ru.netology.nework.player.VideoPlayer
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment

class EventAdapter(
    private val onInteractionListener: OnEventInteractionListener,
    private val audioPlayer: AudioPlayer,
    private val videoPlayer: VideoPlayer
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    private var currentMediaId = -1
    private var trackProgress = 0

    fun setProgress(trackProgress: Int) {
        this.trackProgress = trackProgress
    }

    fun resetCurrentMediaId() {
        currentMediaId = -1
    }

    fun getPositionByEventId(postId: Int): Int {
        for (i in 0 until currentList.size) {
            if (currentList[i].id == postId) {
                return i
            }
        }
        return RecyclerView.NO_POSITION
    }

    inner class EventViewHolder(private val binding: EventItemBinding) : ViewHolder(binding.root) {
        fun bind(event: Event) {
            setupUserData(event)
            setupContent(event)
            setupLikes(event)
            setupPlayButton(event)
            setupVideoPlayButton()
            setupParticipants(event)
            updateAudioProgress(event)
            setupOnUser(event)
            setupLink(event)
            setupSpeakers(event)
            setupCoords(event)
            setupAttachments(event)
            setupDatetime(event)
            setupEventMenu(event)
            setupParticipateButton(event)
            setupOnline(event)
        }

        private fun setupOnline(event: Event) {
            when (event.type) {
                Event.Type.OFFLINE -> binding.online.visibility = View.GONE
                Event.Type.ONLINE -> {
                    binding.online.visibility = View.VISIBLE
                    binding.online.text = itemView.context.getText(R.string.online)
                }
            }
        }

        private fun setupParticipateButton(event: Event) {
            val text =
                if (!event.participatedByMe) itemView.context.getText(R.string.participate) else itemView.context.getText(
                    R.string.leave
                )
            with(binding) {
                participateButton.text = text
                participateButton.setOnClickListener {
                    onInteractionListener.onParticipate(event)
                }
            }
        }

        private fun setupUserData(event: Event) {
            with(binding) {
                authorName.text = event.authorId.toString()
                authorJob.text = event.authorJob ?: ""
                authorName.text = event.author
                authorAvatar.loadCircleCropAvatar(event.authorAvatar.toString())
            }
        }

        private fun setupContent(event: Event) {
            binding.apply {
                content.text = event.content
                itemView.setOnClickListener {
                    onInteractionListener.onContent(event)
                }
            }
        }

        private fun setupOnUser(event: Event) {
            with(binding) {
                authorAvatar.setOnClickListener {
                    onInteractionListener.onUser(event.authorId)
                }
                authorName.setOnClickListener {
                    onInteractionListener.onUser(event.authorId)
                }
                authorJob.setOnClickListener {
                    onInteractionListener.onUser(event.authorId)
                }
            }
        }

        private fun setupLikes(event: Event) {
            val likeRes =
                if (event.likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
            binding.like.setImageResource(likeRes)

            with(binding) {
                like.setOnClickListener {
                    onInteractionListener.onLike(event)
                }
                like.setOnLongClickListener {
                    onInteractionListener.onLikeLongClick(event.likeOwnerIds)
                    true
                }

                if (event.likeOwnerIds.isEmpty()) {
                    likeCount.visibility = View.GONE
                } else {
                    likeCount.visibility = View.VISIBLE
                    likeCount.text = event.likeOwnerIds.size.toString()
                }
            }
        }

        private fun setupParticipants(event: Event) {
            with(binding) {
                participants.movementMethod = LinkMovementMethod.getInstance()
                participants.text = getSpannableBuilder(event.participantsIds, event)
                participantsContainer.visibility =
                    if (event.participantsIds.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        private fun setupSpeakers(event: Event) {
            with(binding) {
                speakers.movementMethod = LinkMovementMethod.getInstance()
                speakers.text = getSpannableBuilder(event.speakerIds, event)
                speakersContainer.visibility =
                    if (event.speakerIds.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        private fun setupLink(event: Event) {
            with(binding) {
                linkContainer.visibility =
                    if (event.link.isNullOrBlank()) View.GONE else View.VISIBLE
                link.setOnClickListener {
                    onInteractionListener.onLink(event.link.toString())
                }
                link.text = event.link.orEmpty()
            }
        }

        private fun setupCoords(event: Event) {
            with(binding) {
                coordsContainer.visibility = if (event.coords == null) View.GONE else View.VISIBLE
                coords.text = event.coords.let { "${it?.lat} : ${it?.long}" }
            }
        }

        private fun setupAttachments(event: Event) {
            with(binding) {
                val attachment = event.attachment
                when (attachment?.type) {
                    Attachment.Type.IMAGE -> {
                        imageAttachment.loadImageAttachment(event.attachment.url)
                        imageAttachment.visibility = View.VISIBLE
                        videoAttachment.visibility = View.GONE
                        audioAttachment.visibility = View.GONE
                    }

                    Attachment.Type.VIDEO -> {
                        videoAttachment.visibility = View.VISIBLE
                        imageAttachment.visibility = View.GONE
                        audioAttachment.visibility = View.GONE

                        if (videoPlayer.getSettledVideoId() != event.id) {
                            thumbnail.visibility = View.VISIBLE
                            videoPlayerView.visibility = View.GONE
                        } else {
                            thumbnail.visibility = View.GONE
                            videoPlayerView.visibility = View.VISIBLE
                            videoPlayer.attachView(videoPlayerView)
                            if (videoPlayer.isVideoPlaying()) {
                                videoPlayer.play()
                            }
                        }
                        thumbnail.loadImageAttachment(event.attachment.url)
                        videoAttachment.setOnClickListener {
                            thumbnail.visibility = View.GONE
                            videoPlayerView.visibility = View.VISIBLE
                            onInteractionListener.onVideo(
                                binding.videoPlayerView, event.attachment, event.id
                            )
                        }
                    }

                    Attachment.Type.AUDIO -> {
                        audioAttachment.visibility = View.VISIBLE
                        imageAttachment.visibility = View.GONE
                        videoAttachment.visibility = View.GONE
                        playButton.setOnClickListener {
                            currentMediaId = event.id
                            onInteractionListener.onAudio(attachment, event.id)
                            setupPlayButton(event)
                        }
                    }

                    null -> {
                        audioAttachment.visibility = View.GONE
                        imageAttachment.visibility = View.GONE
                        videoAttachment.visibility = View.GONE
                    }
                }
            }
        }

        private fun setupEventMenu(event: Event) {
            if (event.ownedByMe) {
                binding.menu.visibility = View.VISIBLE
            } else {
                binding.menu.visibility = View.GONE
            }
            binding.menu.setOnClickListener {
                onInteractionListener.onMenu(it, event)
            }
        }

        private fun setupPlayButton(event: Event) {
            if (currentMediaId != event.id) {
                binding.playButton.setImageResource(R.drawable.play_icon)
            } else {
                val imageId =
                    if (audioPlayer.isAudioPlaying()) R.drawable.pause_icon else R.drawable.play_icon
                binding.playButton.setImageResource(imageId)
            }
        }

        private fun setupVideoPlayButton() {
            if (!videoPlayer.isVideoPlaying()) R.drawable.play_icon
        }

        private fun updateAudioProgress(event: Event) {
            binding.progressBar.progress = if (currentMediaId == event.id) trackProgress else 0
        }

        private fun setupDatetime(event: Event) {
            with(binding) {
                date.text = DateTimeConverter.publishedToUiDate(event.published)
                time.text = DateTimeConverter.publishedToUiTime(event.published)
                datetime.text = DateTimeConverter.datetimeToUiDateTime(event.datetime)
            }
        }

        private fun getSpannableBuilder(ids: List<Int>, event: Event): SpannableStringBuilder {
            val spannableStringBuilder = SpannableStringBuilder()
            ids.forEachIndexed { index, userId ->
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(view: View) {
                        onInteractionListener.onUser(userId)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }

                val userPreview = event.users.filterKeys { it.toInt() == userId }.values.first()
                spannableStringBuilder.append(
                    userPreview.name, clickableSpan, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (index < ids.size - 1) {
                    spannableStringBuilder.append(", ")
                }
            }
            return spannableStringBuilder
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = EventItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}