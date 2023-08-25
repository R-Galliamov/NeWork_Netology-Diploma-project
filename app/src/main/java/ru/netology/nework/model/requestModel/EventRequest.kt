package ru.netology.nework.model.requestModel

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import java.time.LocalDateTime

data class EventRequest(
    val id: Int = 0,
    val content: String,
    val datetime: String,
    val localDateTime: LocalDateTime? = null,
    val coords: Coordinates? = null,
    val type: Event.Type,
    val attachment: Attachment? = null,
    val link: String? = null,
    val speakerIds: List<Int> = emptyList(),
    val speakerUsers: List<User> = emptyList(),
)