package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dto.Event
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.service.api.ApiService
import java.io.IOException
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val eventDao: EventDao
) :
    EventRepository {
    override val data: Flow<List<Event>> =
        eventDao.getAll().map(List<EventEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        val response = apiService.getAllEvents()
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        val body = response.body() ?: throw ApiError(response.code(), response.message())
        eventDao.upsertEvent(body.toEntity())
    }

    override suspend fun onLike(event: Event): Event {
        try {
            val response =
                if (!event.likedByMe) apiService.likeEventById(event.id) else apiService.dislikeEventById(
                    event.id
                )
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

    override suspend fun isDbEmpty() = eventDao.getRowCount() == 0
}