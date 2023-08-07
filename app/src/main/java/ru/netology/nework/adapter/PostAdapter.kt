package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.USER_AVATARS
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.PostItemBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.view.loadCircleCropAvatar
import kotlin.random.Random

class PostAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    interface OnInteractionListener {
        fun onLike(post: Post)
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
                mention.text = post.mentionUsers.joinToString(", ") { it.name }
                linkContainer.visibility =
                    if (post.link.isNullOrBlank()) View.GONE else View.VISIBLE
                coordsContainer.visibility = if (post.coords == null) View.GONE else View.VISIBLE
                mentionedContainer.visibility =
                    if (post.mentionIds.isEmpty()) View.GONE else View.VISIBLE
                var likedByMe = post.likedByMe
                updateLikeUi(likedByMe)
                like.setOnClickListener {
                    //TODO check if data updates when like clicked
                    //likedByMe = !likedByMe
                    //updateLikeUi(likedByMe)
                    onInteractionListener.onLike(post)
                }
                if (post.authorAvatar.isNullOrBlank()) {
                    val index = Random.nextInt(USER_AVATARS.size)
                    authorAvatar.setImageResource(USER_AVATARS[index])
                } else {
                    authorAvatar.loadCircleCropAvatar(post.authorAvatar.toString())
                }
                date.text = DateTimeConverter.publishedToUIDate(post.published)
                time.text = DateTimeConverter.publishedToUiTime(post.published)
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