package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.dto.User
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(private val usersRepository: UserRepository) :
    ViewModel() {
    val users: LiveData<List<User>> = usersRepository.data.asLiveData()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            usersRepository.getAll()
        }
    }
}