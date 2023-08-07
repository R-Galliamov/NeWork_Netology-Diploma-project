package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.viewModel.UsersViewModel

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding: FragmentEventsBinding
        get() = _binding!!

    private val viewModel: UsersViewModel by activityViewModels()

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
        val adapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onLike(user: User) {

            }
        })
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        //viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
        //    Toast.makeText(requireContext(), error.status.toString(), Toast.LENGTH_SHORT).show()
        //    //TODO create error handler
        //}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}