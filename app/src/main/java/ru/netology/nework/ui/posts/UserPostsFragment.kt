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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentUserPostsBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.error.ErrorHandler
import ru.netology.nework.listeners.OnPostInteractionListener
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.viewModel.EditPostViewModel
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserPostsFragment : Fragment() {

    private var _binding: FragmentUserPostsBinding? = null
    private val binding: FragmentUserPostsBinding
        get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels()
    private val editPostViewModel: EditPostViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

    @Inject
    lateinit var errorHandler: ErrorHandler

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    private var postAdapter: PostAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserPostsBinding.inflate(inflater, container, false)
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
                    if (user != usersViewModel.currentUser.value) {
                        usersViewModel.setCurrentUser(user)
                        findNavController().navigate(R.id.action_userProfileFragment_self)
                    }
                }
            }

            override fun onContent(post: Post) {
                feedViewModel.setCurrentPost(post)
                findNavController().navigate(R.id.action_userProfileFragment_to_postFragment)
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
                        postAdapter?.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onMenu(view: View, post: Post) {
                showMenu(view, post)
            }
        }, audioObserver, videoObserver)

        val recyclerView = binding.recyclerView
        recyclerView.adapter = postAdapter

        usersViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                feedViewModel.updateUserPosts(user.id)
            }
        }

        feedViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter?.submitList(posts)
        }

        feedViewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.errorState) {
                val errorDescription = errorHandler.getErrorDescriptor(state.errorStatus)
                Toast.makeText(
                    requireContext(),
                    errorDescription,
                    Toast.LENGTH_SHORT
                ).show()
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
                    findNavController().navigate(R.id.action_userProfileFragment_to_editPostFragment)
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
        postAdapter = null
        _binding = null
    }
}