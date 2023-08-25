package ru.netology.nework.ui.posts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.ui.PlayerView
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
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
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment
import ru.netology.nework.viewModel.EditPostViewModel
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject


@AndroidEntryPoint
class PostFragment : Fragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding: FragmentPostBinding
        get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val editPostViewModel: EditPostViewModel by activityViewModels()

    private lateinit var onInteractionListener: OnPostInteractionListener

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(audioObserver)
        lifecycle.addObserver(videoObserver)

        val usersAdapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                usersViewModel.setCurrentUser(user)
                findNavController().navigate(R.id.action_postFragment_to_userProfileFragment)
                binding.usersContainer.visibility = View.GONE
            }
        })
        binding.recyclerViewUsers.adapter = usersAdapter
        onInteractionListener = object : OnPostInteractionListener {
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
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onContent(post: Post) {
            }

            override fun onImage() {

            }

            override fun onVideo(playerView: PlayerView, video: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(video.url)) {
                    videoObserver.videoPlayerDelegate(playerView, video, postId)
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onAudio(audio: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    audioObserver.mediaPlayerDelegate(audio, postId) {
                        updateAudioPlayerUI()
                        binding.progressBar.progress = 0
                    }

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        while (audioObserver.isPlaying) {
                            val currentPosition = audioObserver.getCurrentPosition()
                            delay(100)
                            withContext(Dispatchers.Main) {
                                val trackDuration = audioObserver.getTracDuration()
                                if (trackDuration != 0) {
                                    binding.progressBar.progress =
                                        (currentPosition * 100) / trackDuration
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onMenu(view: View, post: Post) {
                showMenu(view, post)
            }
        }

        val post = feedViewModel.currentPost.value
        post?.let {
            feedViewModel.updateCurrentPost(post.id)
        }

        feedViewModel.currentPost.observe(viewLifecycleOwner) { post ->
            setupUserData(post)
            setupContent(post)
            setupOnUser(post)
            setupLink(post)
            setupCoords(post)
            setupMentions(post)
            setupLikes(post)
            setupDatetime(post)
            setupPostMenu(post)
            setupAttachments(post)
            setupMap(post)
        }

        binding.overlay.setOnClickListener {
            binding.usersContainer.visibility = View.GONE
        }
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMap(post: Post) {
        if (post.coords != null) {
            binding.coordsContainer.visibility = View.VISIBLE
            binding.map.visibility = View.VISIBLE
            MapKitFactory.initialize(requireContext())
            val lat = post.coords.lat.toDouble()
            val long = post.coords.long.toDouble()
            val location = Point(lat, long)

            val pin = requireNotNull(
                AppCompatResources.getDrawable(
                    requireContext(), R.drawable.pin_icon_red
                )
            )

            val imageProvider = com.yandex.runtime.image.ImageProvider.fromBitmap(pin.toBitmap())

            binding.map.map.mapObjects.addPlacemark(location, imageProvider)

            binding.map.map.move(
                CameraPosition(
                    location, 17.0f, 0.0f, 0.0f
                ), Animation(Animation.Type.SMOOTH, 0f), null
            )
        } else {
            binding.map.visibility = View.INVISIBLE
            binding.coordsContainer.visibility = View.INVISIBLE
        }
    }

    private fun setupMentions(post: Post) {
        with(binding) {
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

            mention.movementMethod = LinkMovementMethod.getInstance()
            mention.text = spannableStringBuilder
            mentionedContainer.visibility =
                if (post.mentionIds.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupCoords(post: Post) {
        with(binding) {
            coordsContainer.visibility = if (post.coords == null) View.GONE else View.VISIBLE
            coords.text = post.coords.let { "${it?.lat} : ${it?.long}" }
        }
    }

    private fun setupLikes(post: Post) {
        val likeRes = if (post.likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
        binding.like.setImageResource(likeRes)

        with(binding) {
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
        }
    }

    private fun setupContent(post: Post) {
        binding.content.text = post.content
    }

    private fun setupUserData(post: Post) {
        with(binding) {
            authorName.text = post.authorId.toString()
            authorJob.text = post.authorJob ?: ""
            authorName.text = post.author
            authorAvatar.loadCircleCropAvatar(post.authorAvatar.toString())
        }
    }

    private fun setupOnUser(post: Post) {
        with(binding) {
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

    private fun setupLink(post: Post) {
        with(binding) {
            linkContainer.visibility = if (post.link.isNullOrBlank()) View.GONE else View.VISIBLE
            link.setOnClickListener {
                onInteractionListener.onLink(post.link.toString())
            }
            link.text = post.link.orEmpty()
        }
    }

    private fun setupPostMenu(post: Post) {
        if (post.ownedByMe) {
            binding.menu.visibility = View.VISIBLE
        } else {
            binding.menu.visibility = View.GONE
        }
        binding.menu.setOnClickListener {
            onInteractionListener.onMenu(it, post)
        }
    }

    private fun setupAttachments(post: Post) {
        with(binding) {
            when (post.attachment?.type) {
                Attachment.Type.IMAGE -> {
                    imageAttachment.visibility = View.VISIBLE
                    audioAttachment.visibility = View.GONE
                    videoAttachment.visibility = View.GONE
                    imageAttachment.loadImageAttachment(post.attachment.url)
                }

                Attachment.Type.VIDEO -> {
                    videoAttachment.visibility = View.VISIBLE
                    imageAttachment.visibility = View.GONE
                    audioAttachment.visibility = View.GONE
                    if (videoObserver.getSettledVideoId() != post.id) {
                        thumbnail.visibility = View.VISIBLE
                        videoPlayerView.visibility = View.GONE
                    }
                    thumbnail.loadImageAttachment(post.attachment.url)
                    videoAttachment.setOnClickListener {
                        thumbnail.visibility = View.GONE
                        videoPlayerView.visibility = View.VISIBLE
                        onInteractionListener.onVideo(
                            binding.videoPlayerView, post.attachment, post.id
                        )
                    }
                }

                Attachment.Type.AUDIO -> {
                    videoAttachment.visibility = View.GONE
                    audioAttachment.visibility = View.VISIBLE
                    imageAttachment.visibility = View.GONE
                    playButton.setOnClickListener {
                        onInteractionListener.onAudio(post.attachment, post.id)
                        updateAudioPlayerUI()
                    }
                }

                null -> {
                    imageAttachment.visibility = View.GONE
                    audioAttachment.visibility = View.GONE
                    videoAttachment.visibility = View.GONE
                }
            }
        }
    }

    private fun showMenu(view: View, post: Post) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.post_event_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    editPostViewModel.setPostData(post)
                    findNavController().navigate(R.id.action_postFragment_to_editPostFragment)
                    true
                }

                R.id.delete -> {
                    feedViewModel.deletePost(post)
                    findNavController().navigateUp()
                    true
                }

                else -> false
            }
        }
        popUpMenu.show()
    }

    private fun setupDatetime(post: Post) {
        with(binding) {
            date.text = DateTimeConverter.publishedToUiDate(post.published)
            time.text = DateTimeConverter.publishedToUiTime(post.published)
        }
    }

    private fun updateAudioPlayerUI() {
        val imageId = if (audioObserver.isPlaying) R.drawable.pause_icon else R.drawable.play_icon
        binding.playButton.setImageResource(imageId)
    }

    override fun onStop() {
        super.onStop()
        binding.map.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.map.onStart()
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}