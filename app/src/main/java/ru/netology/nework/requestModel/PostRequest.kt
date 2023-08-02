package ru.netology.nework.requestModel

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates


data class PostRequest(
    val id: Int,
    val content: String,
    val coords: Coordinates,
    val link: String,
    val attachment: Attachment,
    val mentionIds: List<Int>,
)