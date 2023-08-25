package ru.netology.nework.repository

import android.content.ContentResolver
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Media
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.requestModel.EventRequest
import ru.netology.nework.service.api.ApiService
import java.io.IOException
import java.text.DecimalFormat
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val eventDao: EventDao,
    private val contentResolver: ContentResolver,
) : EventRepository {
    override val data: Flow<List<Event>> =
        eventDao.getAll().map(List<EventEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = apiService.getAllEvents()
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            eventDao.upsertEvent(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveEvent(eventRequest: EventRequest): Event {
        try {
            var data = eventRequest
            if (eventRequest.attachment != null) {
                val media = upload(eventRequest.attachment)
                data = data.copy(attachment = data.attachment?.copy(url = media.url))
            }
            val response = apiService.saveEvent(data)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val event = response.body() ?: throw ApiError(response.code(), response.message())
            eventDao.upsertEvent(EventEntity.fromDto(event))
            return event
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun onLike(event: Event): Event {
        try {
            val response =
                if (!event.likedByMe) apiService.likeEventById(event.id) else apiService.dislikeEventById(
                    event.id
                )
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val event = response.body() ?: throw ApiError(response.code(), response.message())
            eventDao.upsertEvent(EventEntity.fromDto(event))
            return event
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun isDbEmpty() = eventDao.getRowCount() == 0
    override suspend fun deleteEvent(eventId: Int) {
        try {
            apiService.deleteEvent(eventId)
            eventDao.deleteEvent(eventId)
        } catch (e: java.lang.Exception) {
            throw e
        }
    }

    override suspend fun upload(attachment: Attachment): Media {
        try {
            val media = contentResolver.openInputStream(attachment.url.toUri())?.use {
                MultipartBody.Part.createFormData(
                    "file", "file", it.readBytes().toRequestBody()
                )
            }
            requireNotNull(media) {
                "Resource ${attachment.url} not found"
            }
            val response = apiService.upload(media)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getEvent(id: Int): Event {
        return eventDao.getEvent(id).toDto()
    }

    override suspend fun getUserEvents(userId: Int): List<Event> {
        try {
            return eventDao.getUserEvents(userId).toDto()
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun participate(eventId: Int): Event {
        try {
            val response = apiService.participateEvent(eventId)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val event = response.body() ?: throw ApiError(response.code(), response.message())
            eventDao.upsertEvent(EventEntity.fromDto(event))
            return event
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun leaveEvent(eventId: Int): Event {
        try {
            val response = apiService.leaveEvent(eventId)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val event = response.body() ?: throw ApiError(response.code(), response.message())
            eventDao.upsertEvent(EventEntity.fromDto(event))
            return event
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}