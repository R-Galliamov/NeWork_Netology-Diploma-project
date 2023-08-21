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
import ru.netology.nework.databinding.PostItemBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.listeners.OnPostInteractionListener
import ru.netology.nework.player.AudioPlayer
import ru.netology.nework.player.VideoPlayer
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment

class PostAdapter(
    private val onInteractionListener: OnPostInteractionListener,
    private val audioPlayer: AudioPlayer,
    private val videoPlayer: VideoPlayer
) :
    ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    private var currentMediaId = -1
    private var trackProgress = 0

    fun setProgress(trackProgress: Int) {
        this.trackProgress = trackProgress
    }

    fun resetCurrentMediaId() {
        currentMediaId = -1
    }

    fun getPositionByPostId(postId: Int): Int {
        for (i in 0 until currentList.size) {
            if (currentList[i].id == postId) {
                return i
            }
        }
        return RecyclerView.NO_POSITION
    }

    inner class PostViewHolder(private val binding: PostItemBinding) : ViewHolder(binding.root) {
        fun bind(post: Post) {
            setupUserData(post)
            setupLink(post)
            setupCoords(post)
            setupPlayButton(post)
            setupVideoPlayButton()
            setupOnUser(post)
            setupMentions(post)
            updateAudioProgress(post)
            setupLikes(post)
            setupDateTime(post)
            setupAttachments(post)
            setupMenu(post)
            setupContent(post)
        }

        private fun setupContent(post: Post) {
            binding.apply {
                content.text = post.content
                itemView.setOnClickListener {
                    onInteractionListener.onContent(post)
                }
            }
        }

        private fun setupUserData(post: Post) {
            binding.apply {
                authorName.text = post.authorId.toString()
                authorJob.text = post.authorJob ?: ""
                authorName.text = post.author
                authorAvatar.loadCircleCropAvatar(post.authorAvatar.toString())
            }
        }

        private fun setupLink(post: Post) {
            binding.apply {
                link.setOnClickListener {
                    onInteractionListener.onLink(post.link.toString())
                }
                linkContainer.visibility =
                    if (post.link.isNullOrBlank()) View.GONE else View.VISIBLE
                link.text = post.link.orEmpty()
            }
        }

        private fun setupCoords(post: Post) {
            binding.apply {
                coordsContainer.visibility = if (post.coords == null) View.GONE else View.VISIBLE
                coords.text = post.coords.let { "${it?.lat} : ${it?.long}" }
            }
        }

        private fun setupOnUser(post: Post) {
            binding.apply {
                authorAvatar.setOnClickListener {
                    onInteractionListener.onUser(post.authorId)
                }
                authorName.setOnClickListener {
                    onInteractionListener.onUser(post.authorId)
                }
                authorJob.setOnClickListener {
                    onInteractionListener.onUser(post.authorId)
                }
            }
        }

        private fun setupDateTime(post: Post) {
            binding.date.text = DateTimeConverter.publishedToUIDate(post.published)
            binding.time.text = DateTimeConverter.publishedToUiTime(post.published)
        }

        private fun setupMenu(post: Post) {
            if (post.ownedByMe) {
                binding.menu.visibility = View.VISIBLE
            } else {
                binding.menu.visibility = View.GONE
            }
        }

        private fun setupVideoPlayButton() {
            if (!videoPlayer.isVideoPlaying()) R.drawable.play_icon
        }

        private fun setupPlayButton(post: Post) {
            if (currentMediaId != post.id) {
                binding.playButton.setImageResource(R.drawable.play_icon)
            } else {
                val imageId =
                    if (audioPlayer.isAudioPlaying()) R.drawable.pause_icon else R.drawable.play_icon
                binding.playButton.setImageResource(imageId)
            }
        }

        private fun setupMentions(post: Post) {
            binding.mentionedContainer.visibility =
                if (post.mentionIds.isEmpty()) View.GONE else View.VISIBLE
            val spannableStringBuilder = SpannableStringBuilder()
            post.mentionIds.forEachIndexed { index, userId ->
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(view: View) {
                        onInteractionListener.onUser(userId)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }

                val userPreview = post.users.filterKeys { it.toInt() == userId }.values.first()
                spannableStringBuilder.append(
                    userPreview.name, clickableSpan, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (index < post.mentionIds.size - 1) {
                    spannableStringBuilder.append(", ")
                }
            }
            binding.mention.movementMethod = LinkMovementMethod.getInstance()
            binding.mention.text = spannableStringBuilder
        }

        private fun setupLikes(post: Post) {
            val likeRes = if (post.likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
            with(binding) {
                like.setImageResource(likeRes)
                like.setOnClickListener {
                    onInteractionListener.onLike(getItem(adapterPosition))
                }
                like.setOnLongClickListener {
                    onInteractionListener.onLikeLongClick(getItem(adapterPosition).likeOwnerIds)
                    true
                }
                likeCount.text = post.likeOwnerIds.size.toString()
                likeCount.visibility =
                    if (post.likedByMe || (binding.likeCount.text.toString().toIntOrNull()
                            ?: 0) > 0
                    ) View.VISIBLE else View.GONE
            }
        }

        private fun setupAttachments(post: Post) {
            binding.apply {
                val attachment = post.attachment
                when (attachment?.type) {
                    Attachment.Type.IMAGE -> {
                        imageAttachment.visibility = View.VISIBLE
                        videoAttachment.visibility = View.GONE
                        audioAttachment.visibility = View.GONE
                        imageAttachment.loadImageAttachment(attachment.url)
                    }

                    Attachment.Type.AUDIO -> {
                        audioAttachment.visibility = View.VISIBLE
                        imageAttachment.visibility = View.GONE
                        videoAttachment.visibility = View.GONE
                        playButton.setOnClickListener {
                            currentMediaId = post.id
                            onInteractionListener.onAudio(attachment, post.id)
                            setupPlayButton(post)
                        }
                    }

                    Attachment.Type.VIDEO -> {
                        videoAttachment.visibility = View.VISIBLE
                        imageAttachment.visibility = View.GONE
                        audioAttachment.visibility = View.GONE

                        if (videoPlayer.getSettledVideoId() != post.id) {
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

                        thumbnail.loadImageAttachment(post.attachment.url)
                        videoAttachment.setOnClickListener {
                            thumbnail.visibility = View.GONE
                            videoPlayerView.visibility = View.VISIBLE
                            onInteractionListener.onVideo(
                                binding.videoPlayerView,
                                post.attachment,
                                post.id
                            )
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

        private fun updateAudioProgress(post: Post) {
            binding.progressBar.progress = if (currentMediaId == post.id) trackProgress else 0
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