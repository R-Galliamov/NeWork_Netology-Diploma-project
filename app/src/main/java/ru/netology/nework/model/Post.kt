package ru.netology.nework.model

data class Post(
    val id: Int,
    val authorId: Int,
    val authorAvatar: String,
    val authorJob: String,
    val content: String,
    val published: String,
    val coords: Coordinates? = null,
    val link: String,
    val likeOwnersIds: List<Int>,
    val mentionIds: List<Int>,
    val mentionedMe: Boolean,
    val likedByMe: Boolean,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
    val users: List<UserPreview> = emptyList(), //TODO just list of users that liked post. Maybe delete this
)

data class PostRequest(
    val id: Int,
    val content: String,
    val coords: Coordinates,
    val link: String,
    val attachment: Attachment,
    val mentionIds: List<Int>,
)