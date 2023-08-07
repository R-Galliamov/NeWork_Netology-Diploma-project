package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Event
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(private val eventRepository: EventRepository) :
    ViewModel() {
    val events: LiveData<List<Event>> = eventRepository.data.map { list ->
        list.map { event ->
            val speakers =
                event.users.filterKeys { it.toIntOrNull() in event.speakerIds }.values.toList()
            val participants =
                event.users.filterKeys { it.toIntOrNull() in event.participantsIds }.values.toList()
            event.copy(speakerUsers = speakers, participantUsers = participants)
        }

    }.asLiveData()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            eventRepository.getAll()
        }
    }
}