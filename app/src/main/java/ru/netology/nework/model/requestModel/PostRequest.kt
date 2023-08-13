package ru.netology.nework.model.requestModel

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates


data class PostRequest(
    val id: Int = 0,
    val content: String,
    val coords: Coordinates? = null,
    val link: String? = null,
    val attachment: Attachment? = null,
    val mentionIds: List<Int> = emptyList(),
)