package ru.netology.nework.model

data class EventResponse(
    val id: Int,
    val authorId: Int,
    val authorAvatar: String,
    val authorJob: String,
    val content: String,
    val dateTime: String,
    val published: String,
    val coords: Coordinates,
    val type: Type,
    val likeOwnersId: List<Int>,
    val likedByMe: Boolean,
    val speakersId: List<Int>,
    val participantsIds: List<Int>,
    val participatedByMe: Boolean,
    val attachment: Attachment,
    val link: String,
    val ownedByMe: Boolean,
    val users: List<UserPreview>,

    ) {
    enum class Type {
        OFFLINE, ONLINE
    }
}

data class EventCreateRequest(
    val id: Int,
    val content: String,
    val dateTime: String,
    val coords: Coordinates,
    val type: EventResponse.Type,
    val attachment: Attachment,
    val link: String,
    val speakersId: List<Int>,
)
