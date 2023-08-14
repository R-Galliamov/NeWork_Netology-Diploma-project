package ru.netology.nework.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding: FragmentFeedBinding
        get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

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
        val adapter = PostAdapter(object : PostAdapter.OnInteractionListener {
            override fun onLike(post: Post) {
                feedViewModel.onLike(post)
            }

            override fun onUser(userId: Int) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val user = usersViewModel.getUserById(userId)
                    if (user != null) {
                        usersViewModel.setCurrentUser(user)
                        findNavController().navigate(R.id.action_holderFragment_to_userProfileFragment)
                    }
                }
            }

            override fun onLink(url: String) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }

            override fun onImage() {
                TODO("Not yet implemented")
            }

            override fun onVideo() {

            }

            override fun onAudio() {

            }


        })
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        feedViewModel.posts.observe(viewLifecycleOwner)
        { posts ->
            adapter.submitList(posts)
        }

        feedViewModel.errorLiveData.observe(viewLifecycleOwner)
        { error ->
            Toast.makeText(requireContext(), error.status.toString(), Toast.LENGTH_SHORT).show()
            //TODO create error handler
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}