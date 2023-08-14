package ru.netology.nework.adapter

import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.PostItemBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment

class PostAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    interface OnInteractionListener {
        fun onLike(post: Post)
        fun onUser(userId: Int)
        fun onLink(url: String)
        fun onImage()
        fun onVideo()
        fun onAudio()
    }

    inner class PostViewHolder(private val binding: PostItemBinding) : ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.apply {
                authorName.text = post.authorId.toString()
                authorJob.text = post.authorJob ?: ""
                authorName.text = post.author
                content.text = post.content
                link.text = post.link.orEmpty()
                coords.text = post.coords.let { "${it?.lat} : ${it?.long}" }

                val spannableStringBuilder = SpannableStringBuilder()
                post.mentionIds.forEachIndexed { index, userId ->
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(view: View) {
                            onInteractionListener.onUser(userId)
                        }
                    }

                    val userPreview =
                        post.users.filterKeys { it.toInt() == userId }.values.first()
                    spannableStringBuilder.append(
                        userPreview.name,
                        clickableSpan,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    if (index < post.mentionIds.size - 1) {
                        spannableStringBuilder.append(", ")
                    }
                }
                mention.text = spannableStringBuilder

                linkContainer.visibility =
                    if (post.link.isNullOrBlank()) View.GONE else View.VISIBLE
                coordsContainer.visibility = if (post.coords == null) View.GONE else View.VISIBLE
                mentionedContainer.visibility =
                    if (post.mentionIds.isEmpty()) View.GONE else View.VISIBLE
                var likedByMe = post.likedByMe
                updateLikeUi(likedByMe)
                like.setOnClickListener {
                    onInteractionListener.onLike(post)
                }
                if (post.likeOwnerIds.isEmpty()) {
                    likeCount.visibility = View.GONE
                } else {
                    likeCount.visibility = View.VISIBLE
                    likeCount.text = post.likeOwnerIds.size.toString()
                }
                authorAvatar.loadCircleCropAvatar(post.authorAvatar.toString())
                authorAvatar.setOnClickListener {
                    onInteractionListener.onUser(post.authorId)
                }
                authorName.setOnClickListener {
                    onInteractionListener.onUser(post.authorId)
                }
                authorJob.setOnClickListener {
                    onInteractionListener.onUser(post.authorId)
                }
                link.setOnClickListener {
                    onInteractionListener.onLink(post.link.toString())
                }
                date.text = DateTimeConverter.publishedToUIDate(post.published)
                time.text = DateTimeConverter.publishedToUiTime(post.published)
                if (post.attachment != null) {
                    when (post.attachment.type) {
                        Attachment.Type.IMAGE -> {
                            imageAttachment.loadImageAttachment(post.attachment.url)
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
                            onInteractionListener.onAudio()
                        }
                    }
                } else {
                    imageAttachment.visibility = View.GONE
                    playerAttachment.visibility = View.GONE
                }
                if (post.ownedByMe) {
                    TODO("show pop up menu")
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}