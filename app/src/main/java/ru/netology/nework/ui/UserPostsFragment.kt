package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel

@AndroidEntryPoint
class UserPostsFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding: FragmentFeedBinding
        get() = _binding!!

    private val postsViewModel: FeedViewModel by activityViewModels()
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
                postsViewModel.onLike(post)
            }

            override fun onUser(userId: Int) {
                TODO("Not yet implemented")
            }

            override fun onLink(url: String) {
                TODO("Not yet implemented")
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
        postsViewModel.posts.observe(viewLifecycleOwner) {
            val user = usersViewModel.currentUser.value
            if (user != null) {
                val posts = postsViewModel.getUserPosts(user.id)
                adapter.submitList(posts)
            }
        }

        postsViewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error.status.toString(), Toast.LENGTH_SHORT).show()
            //TODO create error handler
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}