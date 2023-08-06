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
import ru.netology.nework.service.api.ApiService
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
}