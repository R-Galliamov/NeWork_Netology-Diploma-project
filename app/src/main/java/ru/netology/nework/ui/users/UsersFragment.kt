package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.User
import ru.netology.nework.error.ErrorHandler
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.viewModel.NavStateViewModel
import ru.netology.nework.viewModel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding: FragmentEventsBinding
        get() = _binding!!

    private val usersViewModel: UsersViewModel by activityViewModels()
    private val navStateViewModel: NavStateViewModel by activityViewModels()

    @Inject
    lateinit var mediaObserver: AudioLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(mediaObserver)

        val adapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                usersViewModel.setCurrentUser(user)
                findNavController().navigate(R.id.action_holderFragment_to_userProfileFragment)
            }
        })
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.swiperefresh.setOnRefreshListener {
            usersViewModel.loadUsers()
        }
        usersViewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }
        usersViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressContainer.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.errorState) {
                val errorDescription = ErrorHandler.getApiErrorDescriptor(state.errorObject)
                Toast.makeText(
                    requireContext(),
                    errorDescription,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navStateViewModel.navState.value = NavStateViewModel.NavState.UsersFragment
        _binding = null
    }
}