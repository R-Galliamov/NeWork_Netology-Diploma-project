package ru.netology.nework.repository

import android.util.Log
import ru.netology.nework.dto.Job
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.service.api.ApiService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : JobRepository {
    override suspend fun getJobs(userId: Int): List<Job> {
        try {
            val response = apiService.getJobsByUserId(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }

    }

    override suspend fun saveJob(job: Job): Job {
        val response = apiService.saveJob(job)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        Log.d("App log", response.body().toString())
        return response.body() ?: throw ApiError(response.code(), response.message())

    }

    override suspend fun deleteJob(jobId: Int) {
        apiService.deleteJob(jobId)
    }

}