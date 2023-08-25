package ru.netology.nework.ui.posts

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
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.error.ErrorHandler
import ru.netology.nework.listeners.OnPostInteractionListener
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.viewModel.EditPostViewModel
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.NavStateViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding: FragmentFeedBinding
        get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val editPostViewModel: EditPostViewModel by activityViewModels()
    private val navStateViewModel: NavStateViewModel by activityViewModels()

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    private var postAdapter: PostAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
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

        postAdapter = PostAdapter(object : OnPostInteractionListener {
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
                    findNavController().navigate(R.id.action_holderFragment_to_userProfileFragment)
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
                feedViewModel.setCurrentPost(post)
                findNavController().navigate(R.id.action_holderFragment_to_postFragment)
            }

            override fun onImage() {

            }

            override fun onVideo(playerView: PlayerView, video: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(video.url)) {
                    videoObserver.videoPlayerDelegate(playerView, video, postId)
                    postAdapter?.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }
            }


            override fun onAudio(audio: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    audioObserver.mediaPlayerDelegate(audio, postId) {
                        postAdapter?.resetCurrentMediaId()
                        postAdapter?.notifyDataSetChanged()
                    }

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        while (audioObserver.isPlaying) {
                            val currentPosition = audioObserver.getCurrentPosition()
                            delay(100)
                            withContext(Dispatchers.Main) {
                                val trackDuration = audioObserver.getTracDuration()
                                if (trackDuration != 0) {
                                    postAdapter?.setProgress((currentPosition * 100) / trackDuration)
                                }
                                postAdapter?.let {
                                    val itemPosition = it.getPositionByPostId(postId)
                                    it.notifyItemChanged(itemPosition)
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

        }, audioObserver, videoObserver)

        val recyclerView = binding.recyclerView
        recyclerView.adapter = postAdapter

        binding.swiperefresh.setOnRefreshListener {
            feedViewModel.loadPosts()
        }

        feedViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressContainer.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.errorState) {
                val errorDescription = ErrorHandler.getApiErrorDescriptor(state.errorObject)
                Toast.makeText(
                    requireContext(), errorDescription, Toast.LENGTH_SHORT
                ).show()
            }
        }
        feedViewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter?.submitList(posts)
        }
    }

    private fun showMenu(view: View, post: Post) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.post_event_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    editPostViewModel.setPostData(post)
                    findNavController().navigate(R.id.action_holderFragment_to_editPostFragment)
                    true
                }

                R.id.delete -> {
                    feedViewModel.deletePost(post)
                    true
                }

                else -> false
            }
        }
        popUpMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navStateViewModel.navState.value = NavStateViewModel.NavState.FeedFragment
        feedViewModel.resetState()
        _binding = null
    }
}