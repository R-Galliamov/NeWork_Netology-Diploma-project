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
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.error.AppError
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.repository.EventRepository
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository, private val appAuth: AppAuth
) : ViewModel() {
    val events: LiveData<List<Event>> = eventRepository.data.map { list ->
        list.map { event ->
            val likeOwnerUsers =
                event.users.filterKeys { it.toIntOrNull() in event.likeOwnerIds }.values.toList()
            val speakers =
                event.users.filterKeys { it.toIntOrNull() in event.speakerIds }.values.toList()
            val participants =
                event.users.filterKeys { it.toIntOrNull() in event.participantsIds }.values.toList()
            event.copy(
                ownedByMe = appAuth.authStateFlow.value.id == event.authorId,
                participatedByMe = event.participantsIds.contains(appAuth.authStateFlow.value.id),
                coords = event.coords?.let { getValidCoords(it) },
                speakerUsers = speakers,
                participantUsers = participants,
                likeOwnerUsers = likeOwnerUsers
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
        _currentEvent.value = event.copy(coords = event.coords?.let { getValidCoords(it) })
    }

    fun updateCurrentEvent(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val event = eventRepository.getEvent(id).let { dbEvent ->
                    dbEvent.copy(
                        participatedByMe = dbEvent.participantsIds.contains(appAuth.authStateFlow.value.id),
                        ownedByMe = appAuth.authStateFlow.value.id == dbEvent.authorId,
                        coords = dbEvent.coords?.let { getValidCoords(it) },
                    )
                }
                withContext(Dispatchers.Main) {
                    setCurrentEvent(event)
                }
            } catch (e: Exception) {
                LoadingStateModel(errorState = true)
            }
        }
    }

    fun updateUserEvents(userId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val userEvents = eventRepository.getUserEvents(userId).map { event ->
                    event.copy(
                        participatedByMe = event.participantsIds.contains(appAuth.authStateFlow.value.id),
                        ownedByMe = appAuth.authStateFlow.value.id == event.authorId,
                        coords = event.coords?.let { getValidCoords(it) },
                    )
                }
                withContext(Dispatchers.Main) {
                    _userEvents.value = userEvents
                }
            } catch (e: Exception) {
                LoadingStateModel(errorState = true)
            }
        }
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
                    updateUserEvents(event)
                }
            } catch (e: AppError) {
                withContext(Dispatchers.Main) {
                    _dataState.value = LoadingStateModel(errorState = true, errorStatus = e.status)
                }
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                eventRepository.deleteEvent(event.id)

                withContext(Dispatchers.Main) {
                    val currentEvents = userEvents.value?.toMutableList()
                    currentEvents?.removeIf { it.id == event.id }
                    _userEvents.value = currentEvents!!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetState() {
        _dataState.value = LoadingStateModel()
    }

    fun participate(event: Event) {
        viewModelScope.launch {
            try {
                val event = if (event.participatedByMe) eventRepository.leaveEvent(event.id)
                else eventRepository.participate(event.id)
                withContext(Dispatchers.Main) {
                    setCurrentEvent(event)
                    updateUserEvents(event)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUserEvents(event: Event) {
        val currentEvents = userEvents.value?.toMutableList()
        currentEvents?.indexOfFirst { it.id == event.id }?.takeIf { it != -1 }?.let { index ->
            currentEvents[index] = event.copy(coords = event.coords?.let { getValidCoords(it) })
            _userEvents.value = currentEvents!!
        }
    }

    private fun getValidCoords(coords: Coordinates): Coordinates {
        val decimalFormat = DecimalFormat("0.000000")
        val lat = decimalFormat.format(coords.lat.toDouble())
        val long = decimalFormat.format(coords.long.toDouble())
        return Coordinates(lat, long)
    }
}