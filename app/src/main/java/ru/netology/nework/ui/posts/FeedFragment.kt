package ru.netology.nework.ui.posts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
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
import ru.netology.nework.listeners.OnPostInteractionListener
import ru.netology.nework.service.MediaLifecycleObserver
import ru.netology.nework.viewModel.AuthViewModel
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
    private val navStateViewModel: NavStateViewModel by activityViewModels()

    @Inject
    lateinit var mediaObserver: MediaLifecycleObserver

    private var postAdapter: PostAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(mediaObserver)
        feedViewModel.loadPosts()

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
                        requireContext(),
                        getString(R.string.invalid_link),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onContent(post: Post) {
                feedViewModel.setCurrentPost(post)
                findNavController().navigate(R.id.action_holderFragment_to_postFragment)
            }

            override fun onImage() {

            }

            override fun onVideo() {

            }

            override fun onAudio(audio: Attachment, postId: Int) {
                if (URLUtil.isValidUrl(audio.url)) {
                    mediaObserver.mediaPlayerDelegate(audio, postId) {
                        postAdapter?.resetCurrentMediaId()
                        postAdapter?.notifyDataSetChanged()
                    }

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        while (mediaObserver.isPlaying) {
                            val currentPosition = mediaObserver.getCurrentPosition()
                            delay(100)
                            withContext(Dispatchers.Main) {
                                val trackDuration = mediaObserver.getTracDuration()
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
        recyclerView.adapter = postAdapter

        binding.swiperefresh.setOnRefreshListener {
            feedViewModel.loadPosts()
        }

        feedViewModel.dataState.observe(viewLifecycleOwner)
        { state ->
            binding.progressContainer.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
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
        feedViewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter?.submitList(posts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navStateViewModel.navState.value = NavStateViewModel.NavState.FeedFragment
        feedViewModel.resetState()
        _binding = null
    }
}