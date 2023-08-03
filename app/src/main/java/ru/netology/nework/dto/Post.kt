package ru.netology.nework.dto

data class Post(
    val id: Int,
    val authorId: Int,
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val published: String,
    val coords: Coordinates? = null,
    val link: String? = null,
    val likeOwnerIds: List<Int>,
    val mentionIds: List<Int>,
    val mentionUsers: List<User> = emptyList(),
    val mentionedMe: Boolean = false,
    val likedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
    val users: Map<String, UserPreview> = emptyMap()
)
