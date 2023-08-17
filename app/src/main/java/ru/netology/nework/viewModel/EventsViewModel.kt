package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ApiError
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.repository.EventRepository
import java.lang.Exception
import java.text.DecimalFormat
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
            event.copy(
                coords = event.coords?.let { getValidCoords(it) },
                speakerUsers = speakers,
                participantUsers = participants
            )
        }

    }.asLiveData()

    private val _dataState = MutableLiveData<LoadingStateModel>()
    val dataState: LiveData<LoadingStateModel>
        get() = _dataState

    private var _currentEvent: MutableLiveData<Event> = MutableLiveData(null)
    val currentEvent: LiveData<Event>
        get() = _currentEvent


    private var _userEvents: MutableLiveData<List<Event>> = MutableLiveData(null)
    val userEvents: LiveData<List<Event>>
        get() = _userEvents

    init {
        loadEvents()
    }

    fun setCurrentEvent(event: Event) {
        _currentEvent.value = event
    }

    fun loadUserEvents(userId: Int) {
        _userEvents.value = events.value?.filter { event -> event.authorId == userId }
        _dataState.value = LoadingStateModel()
    }

    fun loadEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            var newState = if (eventRepository.isDbEmpty()) {
                LoadingStateModel(loading = true)
            } else {
                LoadingStateModel(refreshing = true)
            }

            withContext(Dispatchers.Main) {
                _dataState.value = newState
            }
            newState = try {
                eventRepository.getAll()
                LoadingStateModel()
            } catch (e: Exception) {
                LoadingStateModel(errorState = true)
            }
            withContext(Dispatchers.Main) {
                _dataState.value = newState
            }
        }
    }

    fun onLike(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val event = eventRepository.onLike(event)
                withContext(Dispatchers.Main) {
                    setCurrentEvent(event)

                    val currentEvents = userEvents.value?.toMutableList()
                    currentEvents?.indexOfFirst { it.id == event.id }?.takeIf { it != -1 }
                        ?.let { index ->
                            currentEvents[index] = event
                            _userEvents.value = currentEvents!!
                        }
                }
            } catch (e: ApiError) {
                withContext(Dispatchers.Main) {
                    _dataState.value = LoadingStateModel(errorState = true, errorObject = e)
                }
            }
        }
    }

    fun resetState() {
        _dataState.value = LoadingStateModel()
    }

    private fun getValidCoords(coords: Coordinates): Coordinates {
        val decimalFormat = DecimalFormat("0.000000")
        val lat = decimalFormat.format(coords.lat.toDouble())
        val long = decimalFormat.format(coords.long.toDouble())
        return Coordinates(lat, long)
    }
}