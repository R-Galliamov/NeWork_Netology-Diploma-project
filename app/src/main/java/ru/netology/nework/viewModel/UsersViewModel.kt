package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.User
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val usersRepository: UserRepository,
    appAuth: AppAuth
) :
    ViewModel() {
    val users: LiveData<List<User>> = usersRepository.data.asLiveData()
    private var _currentUser: MutableLiveData<User> = MutableLiveData(null)
    val currentUser: LiveData<User>
        get() = _currentUser

    init {
        loadUsers()
    }

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    fun loadUsers() {
        viewModelScope.launch {
            usersRepository.getAll()
        }
    }
}