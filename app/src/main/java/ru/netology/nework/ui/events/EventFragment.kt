package ru.netology.nework.ui.events

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
import ru.netology.nework.service.MediaLifecycleObserver
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment
import ru.netology.nework.viewModel.EventsViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventFragment : Fragment() {

    private var _binding: FragmentEventBinding? = null
    private val binding: FragmentEventBinding
        get() = _binding!!
    private val eventsViewModel: EventsViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    lateinit var onInteractionListener: OnEventInteractionListener
    @Inject
    lateinit var mediaObserver: MediaLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(mediaObserver)

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
                        requireContext(),
                        getString(R.string.invalid_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onContent(event: Event) {
            }

            override fun onImage() {

            }

            override fun onVideo() {

            }

            override fun onAudio(audio: Attachment, eventId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    mediaObserver.mediaPlayerDelegate(audio, eventId) {
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

        eventsViewModel.currentEvent.observe(viewLifecycleOwner) { event ->
            if (event != null) {
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
                    coordsContainer.visibility =
                        if (event.coords == null) View.GONE else View.VISIBLE
                    speakersContainer.visibility =
                        if (event.speakerIds.isEmpty()) View.GONE else View.VISIBLE
                    participantsContainer.visibility =
                        if (event.participantsIds.isEmpty()) View.GONE else View.VISIBLE
                    val likedByMe = event.likedByMe
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
                                    onInteractionListener.onAudio(event.attachment, event.id)
                                    updatePlayerUI()
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

        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
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