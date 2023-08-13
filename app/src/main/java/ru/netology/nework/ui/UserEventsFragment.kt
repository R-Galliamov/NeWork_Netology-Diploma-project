package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.viewModel.EventsViewModel
import ru.netology.nework.viewModel.UsersViewModel

@AndroidEntryPoint
class UserEventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding: FragmentEventsBinding
        get() = _binding!!

    private val eventsViewModel: EventsViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

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
        val adapter = EventAdapter(object : EventAdapter.OnInteractionListener {
            override fun onLike(event: Event) {

            }

        })
        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter

        eventsViewModel.events.observe(viewLifecycleOwner) {
            val user = usersViewModel.currentUser.value
            if (user != null) {
                val events = eventsViewModel.getUserEvents(user.id)
                adapter.submitList(events)
            }


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