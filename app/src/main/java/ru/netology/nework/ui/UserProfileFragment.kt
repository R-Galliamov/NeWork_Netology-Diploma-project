package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentUserProfileBinding
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.UsersViewModel

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding: FragmentUserProfileBinding
        get() = _binding!!
    private val usersViewModel: UsersViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usersViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            binding.apply {
                if (user.avatar.isNullOrBlank()) {
                    avatar.setImageResource(R.drawable.user_black_icon)
                } else {
                    avatar.loadCircleCropAvatar(user.avatar)
                }
                userName.text = user.name
                login.text = "@${user.login}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}