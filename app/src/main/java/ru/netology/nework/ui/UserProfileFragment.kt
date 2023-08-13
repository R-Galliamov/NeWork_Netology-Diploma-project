package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import ru.netology.nework.R
import ru.netology.nework.adapter.UserWallAdapter
import ru.netology.nework.databinding.FragmentUserProfileBinding
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.AuthViewModel
import ru.netology.nework.viewModel.UsersViewModel

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding: FragmentUserProfileBinding
        get() = _binding!!
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

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

                    backButton.setOnClickListener {
                        findNavController().navigateUp()
                    }

                    if (usersViewModel.currentUser.value == authViewModel.authenticatedUser.value) {
                        signOutButton.visibility = View.VISIBLE
                        signOutButton.setOnClickListener {
                            authViewModel.signOutUser()
                            findNavController().navigateUp()
                        }
                    } else {
                        signOutButton.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}