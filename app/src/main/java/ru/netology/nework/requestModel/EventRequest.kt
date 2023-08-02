package ru.netology.nework.requestModel

import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event

data class EventRequest(
    val id: Int,
    val content: String,
    val dateTime: String,
    val coords: Coordinates,
    val type: Event.Type,
    val attachment: Attachment,
    val link: String,
    val speakersId: List<Int>,
)