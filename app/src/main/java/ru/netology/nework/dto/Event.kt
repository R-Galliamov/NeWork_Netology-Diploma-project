package ru.netology.nework.dto

data class Event(
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coordinates? = null,
    val type: Type,
    val likeOwnerIds: List<Int> = emptyList(),
    val likedByMe: Boolean = false,
    val speakerIds: List<Int> = emptyList(),
    val speakerUsers: List<UserPreview> = emptyList(),
    val participantsIds: List<Int> = emptyList(),
    val participantUsers: List<UserPreview> = emptyList(),
    val participatedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val link: String? = null,
    val ownedByMe: Boolean = false,
    val users: Map<String, UserPreview> = emptyMap(),

    ) {
    enum class Type {
        OFFLINE, ONLINE
    }
}


