package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.User
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.repository.JobRepository
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val usersRepository: UserRepository,
    private val jobRepository: JobRepository,
) :
    ViewModel() {
    val users: LiveData<List<User>> = usersRepository.data.asLiveData()
    private var _currentUser: MutableLiveData<User> = MutableLiveData(null)
    val currentUser: LiveData<User>
        get() = _currentUser
    private val _dataState = MutableLiveData<LoadingStateModel>()
    val dataState: LiveData<LoadingStateModel>
        get() = _dataState

    val usersNavList = mutableListOf<User>()

    init {
        loadUsers()
    }

    fun setCurrentUser(user: User) {
        if (usersNavList.isEmpty() || usersNavList.last() != user) {
            usersNavList.add(user)
        }
        _currentUser.value = user

        viewModelScope.launch(Dispatchers.IO) {
            val userJobs = jobRepository.getJobs(user.id)
            withContext(Dispatchers.Main) {
                _currentUser.value = user.copy(jobs = userJobs)
            }
        }
    }

    suspend fun getUsersById(userIds: List<Int>): List<User> {
        return userIds.map { userId ->
            getUserById(userId)
        }
    }

    suspend fun getUserById(userId: Int): User {
        val usersList = users.value
        val jobs = jobRepository.getJobs(userId)
        return usersList?.firstOrNull { user -> userId == user.id }
            ?: usersRepository.getUserById(userId).copy(jobs = jobs)
    }

    fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            var newState = if (usersRepository.isDbEmpty()) {
                LoadingStateModel(loading = true)
            } else {
                LoadingStateModel(refreshing = true)
            }

            withContext(Dispatchers.Main) {
                _dataState.value = newState
            }
            newState = try {
                usersRepository.getAll()
                LoadingStateModel()
            } catch (e: Exception) {
                LoadingStateModel(errorState = true)
            }
            withContext(Dispatchers.Main) {
                _dataState.value = newState
            }
        }
    }

    fun saveJob(job: Job) {
        viewModelScope.launch(Dispatchers.IO) {
            val job = jobRepository.saveJob(job)
            val user = currentUser.value
            val jobsList = user?.jobs as MutableList
            jobsList.add(job)
            withContext(Dispatchers.Main) {
                setCurrentUser(user.copy(jobs = jobsList))
            }
        }
    }

    fun deleteJob(job: Job) {
        viewModelScope.launch(Dispatchers.IO) {
            jobRepository.deleteJob(job.id)

            val user = currentUser.value
            val jobsList = user?.jobs as MutableList
            jobsList.remove(job)
            withContext(Dispatchers.Main) {
                setCurrentUser(user.copy(jobs = jobsList))
            }
        }
    }
}