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
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.EventItemBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.listeners.OnEventInteractionListener
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment

class EventAdapter(private val onInteractionListener: OnEventInteractionListener) :
    ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    var currentEventPosition = -1

    inner class EventViewHolder(private val binding: EventItemBinding) : ViewHolder(binding.root) {
        fun bind(event: Event) {
            binding.apply {
                authorName.text = event.authorId.toString()
                authorJob.text = event.authorJob ?: ""
                authorName.text = event.author
                content.text = event.content
                link.text = event.link.orEmpty()
                coords.text = event.coords.let { "${it?.lat} : ${it?.long}" }

                speakers.movementMethod = LinkMovementMethod.getInstance()
                speakers.text = getSpannableBuilder(event.speakerIds, event)

                participants.movementMethod = LinkMovementMethod.getInstance()
                participants.text = getSpannableBuilder(event.participantsIds, event)

                linkContainer.visibility =
                    if (event.link.isNullOrBlank()) View.GONE else View.VISIBLE
                coordsContainer.visibility = if (event.coords == null) View.GONE else View.VISIBLE
                speakersContainer.visibility =
                    if (event.speakerIds.isEmpty()) View.GONE else View.VISIBLE
                participantsContainer.visibility =
                    if (event.participantsIds.isEmpty()) View.GONE else View.VISIBLE
                var likedByMe = event.likedByMe
                updateLikeUi(likedByMe)
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
                itemView.setOnClickListener {
                    onInteractionListener.onContent(event)
                }

                authorAvatar.loadCircleCropAvatar(event.authorAvatar.toString())
                authorAvatar.setOnClickListener {
                    onInteractionListener.onUser(event.authorId)
                }
                authorName.setOnClickListener {
                    onInteractionListener.onUser(event.authorId)
                }
                authorJob.setOnClickListener {
                    onInteractionListener.onUser(event.authorId)
                }
                link.setOnClickListener {
                    onInteractionListener.onLink(event.link.toString())
                }
                date.text = DateTimeConverter.publishedToUIDate(event.published)
                time.text = DateTimeConverter.publishedToUiTime(event.published)
                datetime.text = DateTimeConverter.datetimeToUiDatetime(event.datetime)

                if (currentEventPosition != adapterPosition) {
                    playButton.setImageResource(R.drawable.play_icon)
                } else {
                    val imageId =
                        if (onInteractionListener.isAudioPlaying()) R.drawable.pause_icon else
                            R.drawable.play_icon
                    playButton.setImageResource(imageId)
                }

                if (event.attachment != null) {
                    when (event.attachment.type) {
                        Attachment.Type.IMAGE -> {
                            imageAttachment.loadImageAttachment(event.attachment.url)
                            imageAttachment.visibility = View.VISIBLE
                            playerAttachment.visibility = View.GONE
                        }

                        Attachment.Type.VIDEO -> {
                            onInteractionListener.onVideo()
                            imageAttachment.visibility = View.GONE
                            playerAttachment.visibility = View.GONE
                        }

                        Attachment.Type.AUDIO -> {
                            playerAttachment.visibility = View.VISIBLE
                            imageAttachment.visibility = View.GONE
                            playButton.setOnClickListener {
                                currentEventPosition = adapterPosition
                                onInteractionListener.onAudio(event.attachment, event.id)
                                notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    imageAttachment.visibility = View.GONE
                    playerAttachment.visibility = View.GONE
                }
                if (event.ownedByMe) {
                    menu.visibility = View.VISIBLE
                } else {
                    menu.visibility = View.GONE
                }
            }
        }

        private fun updateLikeUi(likedByMe: Boolean) {
            val likeRes =
                if (likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
            binding.like.setImageResource(likeRes)
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

            val userPreview =
                event.users.filterKeys { it.toInt() == userId }.values.first()
            spannableStringBuilder.append(
                userPreview.name,
                clickableSpan,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (index < ids.size - 1) {
                spannableStringBuilder.append(", ")
            }
        }
        return spannableStringBuilder
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