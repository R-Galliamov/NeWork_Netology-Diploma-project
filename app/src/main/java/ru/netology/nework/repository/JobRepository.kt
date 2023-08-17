package ru.netology.nework.repository

import ru.netology.nework.dto.Job

interface JobRepository {
    suspend fun getJobs(userId: Int) : List<Job>
    suspend fun saveJob(job: Job) : Job
    suspend fun deleteJob(jobId: Int)
}