package ru.netology.nework.ui.posts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.FragmentPostBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.listeners.OnPostInteractionListener
import ru.netology.nework.service.MediaLifecycleObserver
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel

class PostFragment : Fragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding: FragmentPostBinding
        get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val mediaObserver = MediaLifecycleObserver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(mediaObserver)

        val usersAdapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                usersViewModel.setCurrentUser(user)
                findNavController().navigate(R.id.action_postFragment_to_userProfileFragment)
                binding.usersContainer.visibility = View.GONE
            }
        })
        binding.recyclerViewUsers.adapter = usersAdapter
        val onInteractionListener = object : OnPostInteractionListener {
            override fun onLike(post: Post) {
                feedViewModel.onLike(post)
            }

            override fun onLikeLongClick(usersIdsList: List<Int>) {
                if (usersIdsList.isNotEmpty()) {
                    binding.usersContainer.visibility = View.VISIBLE
                    viewLifecycleOwner.lifecycleScope.launch {
                        val users = usersViewModel.getUsersById(usersIdsList)
                        usersAdapter.submitList(users)
                    }
                }
            }

            override fun onUser(userId: Int) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val user = usersViewModel.getUserById(userId)
                    usersViewModel.setCurrentUser(user)
                    findNavController().navigate(R.id.action_postFragment_to_userProfileFragment)
                }
            }

            override fun onLink(url: String) {
                if (URLUtil.isValidUrl(url)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onContent(post: Post) {
            }

            override fun onImage() {
                TODO("Not yet implemented")
            }

            override fun onVideo() {

            }

            override fun onAudio(audio: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    mediaObserver.mediaPlayerDelegate(audio, postId) {
                        updatePlayerUI()
                        binding.progressBar.progress = 0
                    }

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        while (mediaObserver.isPlaying) {
                            val currentPosition = mediaObserver.getCurrentPosition()
                            delay(100)
                            withContext(Dispatchers.Main) {
                                val trackDuration = mediaObserver.getTracDuration()
                                if (trackDuration != 0) {
                                    binding.progressBar.progress =
                                        (currentPosition * 100) / trackDuration
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun isAudioPlaying(): Boolean {
                return mediaObserver.isPlaying
            }
        }

        feedViewModel.currentPost.observe(viewLifecycleOwner) { post ->
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

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
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
                mention.movementMethod = LinkMovementMethod.getInstance()
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
                like.setOnLongClickListener {
                    onInteractionListener.onLikeLongClick(post.likeOwnerIds)
                    true
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
                            playButton.setOnClickListener {
                                onInteractionListener.onAudio(post.attachment, post.id)
                                updatePlayerUI()
                            }
                        }
                    }
                } else {
                    imageAttachment.visibility = View.GONE
                    playerAttachment.visibility = View.GONE
                }
                if (post.ownedByMe) {
                    menu.visibility = View.VISIBLE
                } else {
                    menu.visibility = View.GONE
                }

                overlay.setOnClickListener {
                    usersContainer.visibility = View.GONE
                }
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun updateLikeUi(likedByMe: Boolean) {
        val likeRes =
            if (likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
        binding.like.setImageResource(likeRes)
    }


    private fun updatePlayerUI() {
        val imageId =
            if (mediaObserver.isPlaying) R.drawable.pause_icon else
                R.drawable.play_icon
        binding.playButton.setImageResource(imageId)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}