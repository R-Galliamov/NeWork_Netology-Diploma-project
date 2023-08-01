package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.model.Token
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val appAuth: AppAuth): ViewModel() {
    val data: LiveData<Token> = appAuth.tokenStateFlow.asLiveData(Dispatchers.Default)
    val authenticated: Boolean
        get() = appAuth.tokenStateFlow.value.id != 0
}