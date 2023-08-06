package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Event
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(private val eventRepository: EventRepository) :
    ViewModel() {
    val events: LiveData<List<Event>> = eventRepository.data.asLiveData()

    init{
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            eventRepository.getAll()
        }
    }
}