package ru.netology.nework.repository

import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Job
import ru.netology.nework.error.ApiError
import ru.netology.nework.service.api.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : JobRepository {
    override suspend fun getJobs(userId: Int): List<Job> {
        val response = apiService.getJobsByUserId(userId)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }

    override suspend fun saveJob(job: Job): Job {
        val response = apiService.saveJob(job)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }

    override suspend fun deleteJob(jobId: Int) {
        apiService.deleteJob(jobId)
    }

}