package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.model.requestModel.EventRequest
import ru.netology.nework.repository.EventRepository
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.util.SingleLiveEvent
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class EditEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val emptyEvent = EventRequest(content = "", datetime = "", type = Event.Type.OFFLINE)
    private val _eventRequest = MutableLiveData(emptyEvent)
    val eventRequest: LiveData<EventRequest>
        get() = _eventRequest


    private val _eventCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _eventCreated

    private val _dataState = MutableLiveData<LoadingStateModel>()
    val dataState: LiveData<LoadingStateModel>
        get() = _dataState


    fun setPhoto(uri: String) {
        val attachment = Attachment(uri, Attachment.Type.IMAGE)
        _eventRequest.value = _eventRequest.value?.copy(attachment = attachment)
    }

    fun setVideo(uri: String) {
        val attachment = Attachment(uri, Attachment.Type.VIDEO)
        _eventRequest.value = _eventRequest.value?.copy(attachment = attachment)
    }

    fun setAudio(uri: String) {
        val attachment = Attachment(uri, Attachment.Type.AUDIO)
        _eventRequest.value = _eventRequest.value?.copy(attachment = attachment)
    }

    fun removeAttachment() {
        _eventRequest.value = _eventRequest.value?.copy(attachment = null)
    }

    fun setContent(content: String) {
        val text = content.trim()
        _eventRequest.value = _eventRequest.value?.copy(content = text)
    }

    fun setLink(link: String) {
        val link = link.trim()
        _eventRequest.value = _eventRequest.value?.copy(link = link)
    }

    fun setCoords(coords: String) {
        val coordsList = coords.split(", ")
        val coordinates = Coordinates(coordsList[0], coordsList[1])
        _eventRequest.value = _eventRequest.value?.copy(coords = coordinates)
    }

    fun switchEventType() {
        val type = eventRequest.value?.type ?: Event.Type.OFFLINE
        val newType = when (type) {
            Event.Type.OFFLINE -> Event.Type.ONLINE
            Event.Type.ONLINE -> Event.Type.OFFLINE
        }
        _eventRequest.value = _eventRequest.value?.copy(type = newType)
    }

    fun addSpeakerUser(user: User) {
        val currentEventRequest = _eventRequest.value ?: emptyEvent
        if (currentEventRequest.speakerIds.contains(user.id)) {
            return
        }
        val newSpeakerIds = currentEventRequest.speakerIds.toMutableList().apply {
            add(user.id)
        }
        val newSpeakerUsers = currentEventRequest.speakerUsers.toMutableList().apply {
            add(user)
        }
        val newEventRequest = currentEventRequest.copy(
            speakerIds = newSpeakerIds, speakerUsers = newSpeakerUsers
        )
        _eventRequest.value = newEventRequest
    }


    fun saveEvent() {
        _eventRequest.value?.let {
            viewModelScope.launch {
                try {
                    eventRepository.saveEvent(_eventRequest.value!!)
                    _eventCreated.value = Unit
                    _eventRequest.value = emptyEvent
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setLocalDateTime(date: LocalDateTime) {
        val event = _eventRequest.value
        val dateTime = DateTimeConverter.localDateTimeToApiEventFormat(date)
        _eventRequest.value = event?.copy(datetime = dateTime, localDateTime = date)
    }

    fun setEventData(event: Event) {
        var eventRequest = emptyEvent
        viewModelScope.launch(Dispatchers.IO) {
            val speakerUsers = mutableListOf<User>()
            event.speakerIds.forEach {
                val user = userRepository.getUserById(it)
                speakerUsers.add(user)
            }
            eventRequest = EventRequest(
                id = event.id,
                content = event.content,
                datetime = event.datetime,
                localDateTime = DateTimeConverter.apiEventFormatToLocalDateTime(event.datetime),
                coords = event.coords,
                type = event.type,
                link = event.link,
                attachment = event.attachment,
                speakerIds = event.speakerIds,
                speakerUsers = speakerUsers,
            )
            withContext(Dispatchers.Main) {
                _eventRequest.value = eventRequest
            }

        }
    }

    fun clear() {
        _eventRequest.value = emptyEvent
    }
}