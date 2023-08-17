package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.JobsAdapter
import ru.netology.nework.adapter.UserWallAdapter
import ru.netology.nework.databinding.FragmentUserProfileBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.User
import ru.netology.nework.service.MediaLifecycleObserver
import ru.netology.nework.ui.events.UserEventsFragment
import ru.netology.nework.ui.posts.UserPostsFragment
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.AuthViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding: FragmentUserProfileBinding
        get() = _binding!!
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    @Inject
    lateinit var mediaObserver: MediaLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        val fragments = listOf(
            UserPostsFragment(),
            UserEventsFragment(),
        )
        val tabTitles = listOf(getString(R.string.posts), getString(R.string.events))
        val adapter = UserWallAdapter(requireActivity(), fragments)
        binding.userWallViewPager.adapter = adapter
        TabLayoutMediator(binding.tabMode, binding.userWallViewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(mediaObserver)
        val adapter = JobsAdapter(object : JobsAdapter.OnInteractionListener {
            override fun onItem(job: Job) {

            }
        })
        binding.recyclerViewJobs.adapter = adapter

        usersViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.apply {
                    if (user.avatar.isNullOrBlank()) {
                        avatar.setImageResource(R.drawable.user_icon)
                    } else {
                        avatar.loadCircleCropAvatar(user.avatar)
                    }
                    userName.text = user.name
                    login.text = "@${user.login}"

                    adapter.submitList(user.jobs)
                    if (usersViewModel.currentUser.value == authViewModel.authenticatedUser.value) {
                        signOutButton.visibility = View.VISIBLE
                        signOutButton.setOnClickListener {
                            authViewModel.signOutUser()
                            findNavController().navigateUp()
                        }
                    } else {
                        signOutButton.visibility = View.GONE
                    }

                    backButton.setOnClickListener {
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetUser()
        _binding = null
    }

    private fun resetUser() {
        usersViewModel.usersNavList.removeLast()
        val previousUser = usersViewModel.usersNavList.lastOrNull()
        if (previousUser != null) {
            usersViewModel.setCurrentUser(previousUser)
        }
    }
}