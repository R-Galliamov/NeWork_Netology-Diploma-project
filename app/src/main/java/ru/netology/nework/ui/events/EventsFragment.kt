package ru.netology.nework.ui.events

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.error.ErrorHandler
import ru.netology.nework.listeners.OnEventInteractionListener
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.viewModel.EditEventViewModel
import ru.netology.nework.viewModel.EventsViewModel
import ru.netology.nework.viewModel.NavStateViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding: FragmentEventsBinding
        get() = _binding!!

    private val eventsViewModel: EventsViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val editEventViewModel: EditEventViewModel by activityViewModels()
    private val navStateViewModel: NavStateViewModel by activityViewModels()

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    private var eventAdapter: EventAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(audioObserver)
        lifecycle.addObserver(videoObserver)

        binding.usersContainer.visibility = View.GONE
        val usersRecyclerView = binding.recyclerViewUsers
        val usersAdapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                usersViewModel.setCurrentUser(user)
                findNavController().navigate(R.id.action_holderFragment_to_userProfileFragment)
                binding.usersContainer.visibility = View.GONE
            }
        })
        usersRecyclerView.adapter = usersAdapter
        binding.overlay.setOnClickListener {
            binding.usersContainer.visibility = View.GONE
        }

        eventAdapter = EventAdapter(object : OnEventInteractionListener {
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
                    findNavController().navigate(R.id.action_holderFragment_to_userProfileFragment)
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
                eventsViewModel.setCurrentEvent(event)
                findNavController().navigate(R.id.action_holderFragment_to_eventFragment)
            }

            override fun onImage() {

            }

            override fun onVideo(playerView: PlayerView, video: Attachment, eventId: Int) {
                if (URLUtil.isValidUrl(video.url)) {
                    videoObserver.videoPlayerDelegate(playerView, video, eventId)
                    eventAdapter?.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onMenu(view: View, event: Event) {
                showMenu(view, event)
            }

            override fun onAudio(audio: Attachment, eventId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    audioObserver.mediaPlayerDelegate(audio, eventId) {
                        eventAdapter?.resetCurrentMediaId()
                        eventAdapter?.notifyDataSetChanged()
                    }

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        while (audioObserver.isPlaying) {
                            val currentPosition = audioObserver.getCurrentPosition()
                            delay(100)
                            withContext(Dispatchers.Main) {
                                val trackDuration = audioObserver.getTracDuration()
                                if (trackDuration != 0) {
                                    eventAdapter?.setProgress((currentPosition * 100) / trackDuration)
                                }
                                eventAdapter?.let {
                                    val itemPosition = it.getPositionByEventId(eventId)
                                    it.notifyItemChanged(itemPosition)
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

            override fun onParticipate(event: Event) {
                eventsViewModel.participate(event)
            }
        }, audioObserver, videoObserver)

        val recyclerView = binding.recyclerView
        recyclerView.adapter = eventAdapter

        binding.swiperefresh.setOnRefreshListener {
            eventsViewModel.loadEvents()
        }

        eventsViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressContainer.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.errorState) {
                val errorDescription = ErrorHandler.getApiErrorDescriptor(state.errorObject)
                Toast.makeText(
                    requireContext(),
                    errorDescription,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        eventsViewModel.events.observe(viewLifecycleOwner) { events ->
            eventAdapter?.submitList(events)
        }
    }

    private fun showMenu(view: View, event: Event) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.post_event_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    editEventViewModel.setEventData(event)
                    findNavController().navigate(R.id.action_holderFragment_to_editEventFragment)
                    true
                }

                R.id.delete -> {
                    eventsViewModel.deleteEvent(event)
                    true
                }

                else -> false
            }
        }
        popUpMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navStateViewModel.navState.value = NavStateViewModel.NavState.EventsFragment
        _binding = null
    }
}