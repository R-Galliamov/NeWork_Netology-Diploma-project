package ru.netology.nework.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.User
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.model.PhotoModel
import ru.netology.nework.model.requestModel.PostRequest
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.util.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(private val postRepository: PostRepository) :
    ViewModel() {
    private val emptyPost = PostRequest(content = "")
    private val _postRequest = MutableLiveData(emptyPost)
    val postRequest: LiveData<PostRequest>
        get() = _postRequest


    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated


    private val _dataState = MutableLiveData<LoadingStateModel>()
    val dataState: LiveData<LoadingStateModel>
        get() = _dataState


    fun setPhoto(uri: String) {
        val attachment = Attachment(uri, Attachment.Type.IMAGE)
        _postRequest.value = _postRequest.value?.copy(attachment = attachment)
    }

    fun setVideo(uri: String) {
        val attachment = Attachment(uri, Attachment.Type.VIDEO)
        _postRequest.value = _postRequest.value?.copy(attachment = attachment)
    }

    fun removeAttachment() {
        _postRequest.value = _postRequest.value?.copy(attachment = null)
    }

    fun setContent(content: String) {
        val text = content.trim()
        _postRequest.value = _postRequest.value?.copy(content = text)
    }

    fun setLink(link: String) {
        val link = link.trim()
        _postRequest.value = _postRequest.value?.copy(link = link)
    }

    fun addMentionUser(user: User) {
        val currentPostRequest = _postRequest.value ?: emptyPost
        if (currentPostRequest.mentionIds.contains(user.id)) {
            return
        }
        val newMentionIds = currentPostRequest.mentionIds.toMutableList().apply {
            add(user.id)
        }
        val newMentionUsers = currentPostRequest.mentionUsers.toMutableList().apply {
            add(user)
        }
        val newPostRequest = currentPostRequest.copy(
            mentionIds = newMentionIds, mentionUsers = newMentionUsers
        )
        _postRequest.value = newPostRequest
    }

    fun savePost() {
        _postRequest.value?.let {
            viewModelScope.launch {
                try {
                    postRepository.savePost(_postRequest.value!!)
                    _postCreated.value = Unit
                } catch (e: Exception) {
                    Log.d("Error", e.message.toString())
                    e.printStackTrace()
                }
            }
        }
        _postRequest.value = emptyPost
    }

    fun clear() {
        _postRequest.value = emptyPost
    }
}