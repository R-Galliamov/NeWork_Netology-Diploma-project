package ru.netology.nework.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nework.model.requestModel.PostRequest
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor()   : ViewModel() {
    private val emptyPost = PostRequest(content = "")
    private val edited = MutableLiveData(emptyPost)

    fun changeContent(content: String) {
        val text = content.trim()
        edited.value = edited.value?.copy(content = text)
    }
}