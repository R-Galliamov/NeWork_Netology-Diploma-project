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
import ru.netology.nework.dto.User
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.repository.JobRepository
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val usersRepository: UserRepository,
    private val jobRepository: JobRepository,
    appAuth: AppAuth
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
            jobRepository.getJobs(user.id)
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
}