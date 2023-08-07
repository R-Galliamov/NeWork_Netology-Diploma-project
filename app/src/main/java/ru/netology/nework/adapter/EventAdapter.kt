package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.EventItemBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.view.loadCircleCropAvatar

class EventAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    interface OnInteractionListener {
        fun onLike(evet: Event)
    }

    inner class EventViewHolder(private val binding: EventItemBinding) : ViewHolder(binding.root) {
        fun bind(event: Event) {
            binding.apply {
                authorName.text = event.authorId.toString()
                authorJob.text = event.authorJob ?: ""
                authorName.text = event.author
                content.text = event.content
                link.text = event.link.orEmpty()
                coords.text = event.coords.let { "${it?.lat} : ${it?.long}" }
                speakers.text = event.speakerUsers.joinToString(",") { it.name }
                participants.text = event.participantUsers.joinToString(", ") { it.name }
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
                    //TODO check if data updates when like clicked
                    //likedByMe = !likedByMe
                    //updateLikeUi(likedByMe)
                    onInteractionListener.onLike(event)
                }
                authorAvatar.loadCircleCropAvatar(event.authorAvatar.toString())
                date.text = DateTimeConverter.publishedToUIDate(event.published)
                time.text = DateTimeConverter.publishedToUiTime(event.published)
                datetime.text = DateTimeConverter.datetimeToUiDatetime(event.datetime)
            }
        }

        private fun updateLikeUi(likedByMe: Boolean) {
            val likeRes =
                if (likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
            binding.like.setImageResource(likeRes)
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