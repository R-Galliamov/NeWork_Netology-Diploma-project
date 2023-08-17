package ru.netology.nework.ui.posts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentUserPostsBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.listeners.OnPostInteractionListener
import ru.netology.nework.service.MediaLifecycleObserver
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserPostsFragment : Fragment() {

    private var _binding: FragmentUserPostsBinding? = null
    private val binding: FragmentUserPostsBinding
        get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

    @Inject
    lateinit var mediaObserver: MediaLifecycleObserver

    private var adapter: PostAdapter? = null

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

        lifecycle.addObserver(mediaObserver)

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

        adapter = PostAdapter(object : OnPostInteractionListener {
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
                feedViewModel.setCurrentPost(post)
                findNavController().navigate(R.id.action_userProfileFragment_to_postFragment)
            }

            override fun onImage() {

            }

            override fun onVideo() {

            }

            override fun onAudio(audio: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    mediaObserver.mediaPlayerDelegate(audio, postId) {
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

            override fun isAudioPlaying(): Boolean {
                return mediaObserver.isPlaying
            }
        })

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter


        usersViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                feedViewModel.loadUserPosts(user.id)
            }
        }

        feedViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            adapter?.submitList(posts)
        }

        feedViewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.errorState) {
                if (state.errorObject.status == 401) {
                    Toast.makeText(
                        requireContext(),
                        state.errorObject.status.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.something_went_wrong),
                        Snackbar.LENGTH_LONG
                    ).setAction(getString(R.string.retry)) { feedViewModel.loadPosts() }
                        .show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}