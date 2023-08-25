package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.model.requestModel.EventRequest

interface EventRepository {
    val data: Flow<List<Event>>
    suspend fun getAll()
    suspend fun onLike(event: Event): Event

    suspend fun isDbEmpty(): Boolean

    suspend fun saveEvent(eventRequest: EventRequest): Event
    suspend fun deleteEvent(eventId: Int)

    suspend fun upload(attachment: Attachment): Media

    suspend fun getEvent(id: Int): Event
    suspend fun getUserEvents(userId: Int): List<Event>

    suspend fun participate(eventId: Int): Event

    suspend fun leaveEvent(eventId: Int): Event

}