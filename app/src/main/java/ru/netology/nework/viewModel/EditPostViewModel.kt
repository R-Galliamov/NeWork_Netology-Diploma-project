package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.model.requestModel.PostRequest
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.util.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) :
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

    fun setAudio(uri: String) {
        val attachment = Attachment(uri, Attachment.Type.AUDIO)
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

    fun setCoords(coords: String) {
        val coordsList = coords.split(", ")
        val coordinates = Coordinates(coordsList[0], coordsList[1])
        _postRequest.value = _postRequest.value?.copy(coords = coordinates)
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
                    _postRequest.value = emptyPost
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setPostData(post: Post) {
        var postRequest = emptyPost
        viewModelScope.launch(Dispatchers.IO) {
            val mentionUsers = mutableListOf<User>()
            post.mentionIds.forEach {
                val user = userRepository.getUserById(it)
                mentionUsers.add(user)
            }
            postRequest = PostRequest(
                id = post.id,
                content = post.content,
                coords = post.coords,
                link = post.link,
                attachment = post.attachment,
                mentionIds = post.mentionIds,
                mentionUsers = mentionUsers
            )
            withContext(Dispatchers.Main) {
                _postRequest.value = postRequest
            }

        }

    }

    fun clear() {
        _postRequest.value = emptyPost
    }
}