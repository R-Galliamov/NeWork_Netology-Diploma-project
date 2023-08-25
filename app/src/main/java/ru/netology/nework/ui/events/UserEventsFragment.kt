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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentUserEventsBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.error.ErrorHandler
import ru.netology.nework.listeners.OnEventInteractionListener
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.viewModel.EditEventViewModel
import ru.netology.nework.viewModel.EventsViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserEventsFragment : Fragment() {

    private var _binding: FragmentUserEventsBinding? = null
    private val binding: FragmentUserEventsBinding
        get() = _binding!!

    private val eventsViewModel: EventsViewModel by activityViewModels()
    private val editEventViewModel: EditEventViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    private var adapter: EventAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserEventsBinding.inflate(inflater, container, false)
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
                binding.usersContainer.visibility = View.GONE
                findNavController().navigate(R.id.action_userProfileFragment_self)
            }
        })
        usersRecyclerView.adapter = usersAdapter
        binding.overlay.setOnClickListener {
            binding.usersContainer.visibility = View.GONE
        }

        adapter = EventAdapter(object : OnEventInteractionListener {
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
                    if (user != usersViewModel.currentUser.value) {
                        usersViewModel.setCurrentUser(user)
                        findNavController().navigate(R.id.action_userProfileFragment_self)
                    }
                }
            }

            override fun onContent(event: Event) {
                eventsViewModel.setCurrentEvent(event)
                findNavController().navigate(R.id.action_userProfileFragment_to_eventFragment)
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

            override fun onImage() {

            }

            override fun onVideo(playerView: PlayerView, video: Attachment, eventId: Int) {
                if (URLUtil.isValidUrl(video.url)) {
                    videoObserver.videoPlayerDelegate(playerView, video, eventId)
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onAudio(audio: Attachment, eventId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    audioObserver.mediaPlayerDelegate(audio, eventId) {
                        adapter?.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onMenu(view: View, event: Event) {
                showMenu(view, event)
            }

            override fun onParticipate(event: Event) {
                eventsViewModel.participate(event)
            }
        }, audioObserver, videoObserver)

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter

        usersViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                eventsViewModel.updateUserEvents(user.id)
            }
        }

        eventsViewModel.userEvents.observe(viewLifecycleOwner) { events ->
            adapter?.submitList(events)
        }

        eventsViewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.errorState) {
                val errorDescription = ErrorHandler.getApiErrorDescriptor(state.errorObject)
                Toast.makeText(
                    requireContext(),
                    errorDescription,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showMenu(view: View, event: Event) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.post_event_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    editEventViewModel.setEventData(event)
                    findNavController().navigate(R.id.action_userProfileFragment_to_editEventFragment)
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
        eventsViewModel.resetState()
        _binding = null
    }
}