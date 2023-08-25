package ru.netology.nework.ui.events

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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.FragmentEventBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.listeners.OnEventInteractionListener
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment
import ru.netology.nework.viewModel.EditEventViewModel
import ru.netology.nework.viewModel.EventsViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventFragment : Fragment() {

    private var _binding: FragmentEventBinding? = null
    private val binding: FragmentEventBinding
        get() = _binding!!
    private val eventsViewModel: EventsViewModel by activityViewModels()
    private val editEventViewModel: EditEventViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    lateinit var onInteractionListener: OnEventInteractionListener

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(audioObserver)
        lifecycle.addObserver(videoObserver)

        val usersAdapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                usersViewModel.setCurrentUser(user)
                findNavController().navigate(R.id.action_eventFragment_to_userProfileFragment)
                binding.usersContainer.visibility = View.GONE
            }
        })
        binding.recyclerViewUsers.adapter = usersAdapter
        onInteractionListener = object : OnEventInteractionListener {
            override fun onLike(event: Event) {
                eventsViewModel.onLike(event)
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
                    findNavController().navigate(R.id.action_eventFragment_to_userProfileFragment)
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

            override fun onContent(event: Event) {
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

            override fun onAudio(audio: Attachment, eventId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    audioObserver.mediaPlayerDelegate(audio, eventId) {
                        updatePlayerUI()
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

            override fun onMenu(view: View, event: Event) {
                showMenu(view, event)
            }

            override fun onParticipate(event: Event) {
                eventsViewModel.participate(event)
            }
        }

        val event = eventsViewModel.currentEvent.value
        event?.let {
            eventsViewModel.updateCurrentEvent(event.id)
        }

        eventsViewModel.currentEvent.observe(viewLifecycleOwner) { event ->
            setupUserData(event)
            setupContent(event)
            setupLikes(event)
            setupOnUser(event)
            setupLink(event)
            setupSpeakers(event)
            setupParticipants(event)
            setupCoords(event)
            setupDatetime(event)
            setupAttachments(event)
            setupEventMenu(event)
            setupParticipateButton(event)
            setupOnline(event)
            setupMap(event)

            binding.backButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupMap(event: Event) {
        if (event.coords != null) {
            binding.coordsContainer.visibility = View.VISIBLE
            binding.map.visibility = View.VISIBLE
            MapKitFactory.initialize(requireContext())
            val lat = event.coords.lat.toDouble()
            val long = event.coords.long.toDouble()
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

    private fun showMenu(view: View, event: Event) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.post_event_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    editEventViewModel.setEventData(event)
                    findNavController().navigate(R.id.action_eventFragment_to_editEventFragment)
                    true
                }

                R.id.delete -> {
                    eventsViewModel.deleteEvent(event)
                    findNavController().navigateUp()
                    true
                }

                else -> false
            }
        }
        popUpMenu.show()
    }

    private fun setupParticipateButton(event: Event) {
        val text = if (!event.participatedByMe) requireContext().getString(R.string.join)
        else requireContext().getText(R.string.leave)
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
        binding.content.text = event.content
    }

    private fun setupLikes(event: Event) {
        val likeRes = if (event.likedByMe) R.drawable.like_checked else R.drawable.like_unchecked
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

    private fun setupAttachments(event: Event) {
        with(binding) {
            when (event.attachment?.type) {
                Attachment.Type.IMAGE -> {
                    imageAttachment.visibility = View.VISIBLE
                    audioAttachment.visibility = View.GONE
                    videoAttachment.visibility = View.GONE
                    imageAttachment.loadImageAttachment(event.attachment.url)
                }

                Attachment.Type.VIDEO -> {
                    videoAttachment.visibility = View.VISIBLE
                    imageAttachment.visibility = View.GONE
                    audioAttachment.visibility = View.GONE

                    if (videoObserver.getSettledVideoId() != event.id) {
                        thumbnail.visibility = View.VISIBLE
                        videoPlayerView.visibility = View.GONE
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
                    videoAttachment.visibility = View.GONE
                    audioAttachment.visibility = View.VISIBLE
                    imageAttachment.visibility = View.GONE
                    playButton.setOnClickListener {
                        onInteractionListener.onAudio(event.attachment, event.id)
                        updatePlayerUI()
                    }
                }

                null -> {
                    videoAttachment.visibility = View.GONE
                    imageAttachment.visibility = View.GONE
                    audioAttachment.visibility = View.GONE
                }
            }
        }
    }

    private fun setupOnline(event: Event) {
        when (event.type) {
            Event.Type.OFFLINE -> binding.online.visibility = View.GONE
            Event.Type.ONLINE -> {
                binding.online.visibility = View.VISIBLE
                binding.online.text = requireContext().getText(R.string.online)
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

    private fun setupLink(event: Event) {
        with(binding) {
            linkContainer.visibility = if (event.link.isNullOrBlank()) View.GONE else View.VISIBLE
            link.setOnClickListener {
                onInteractionListener.onLink(event.link.toString())
            }
            link.text = event.link.orEmpty()
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

    private fun setupCoords(event: Event) {
        with(binding) {
            coordsContainer.visibility = if (event.coords == null) View.GONE else View.VISIBLE
            coords.text = event.coords.let { "${it?.lat} : ${it?.long}" }
        }
    }

    private fun updatePlayerUI() {
        val imageId = if (audioObserver.isPlaying) R.drawable.pause_icon else R.drawable.play_icon
        binding.playButton.setImageResource(imageId)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}