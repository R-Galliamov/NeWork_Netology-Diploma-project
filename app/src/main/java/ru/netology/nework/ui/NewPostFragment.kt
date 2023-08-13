package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.AuthViewModel

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding: FragmentNewPostBinding
        get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            val user = authViewModel.authenticatedUser.value
            avatar.loadCircleCropAvatar(user?.avatar.toString())
            userName.text = user?.name

            addLinkButton.setOnClickListener {

            }
            addLocationButton.setOnClickListener {

            }
            mentionUserButton.setOnClickListener {

            }
            cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }
            applyButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}