package ru.netology.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.UserPreview

@Entity
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorAvatar: String? = null,
    val authorJob: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    @Embedded
    val coords: CoordinatesEmbeddable? = null,
    val eventType: String,
    val likeOwnerIds: List<Int> = emptyList(),
    val likedByMe: Boolean = false,
    val speakerIds: List<Int> = emptyList(),
    val participantsIds: List<Int> = emptyList(),
    val participatedByMe: Boolean = false,
    @Embedded
    val attachment: AttachmentEmbeddable? = null,
    val link: String? = null,
    val ownedByMe: Boolean = false,
    val users: Map<String, UserPreview> = emptyMap()
) {
    companion object {
        fun fromDto(event: Event) = EventEntity(
            id = event.id,
            authorId = event.authorId,
            author = event.author,
            authorAvatar = event.authorAvatar,
            authorJob = event.authorJob,
            content = event.content,
            datetime = event.datetime,
            published = event.published,
            coords = CoordinatesEmbeddable.fromDto(event.coords),
            eventType = event.type.name,
            likeOwnerIds = event.likeOwnerIds,
            likedByMe = event.likedByMe,
            speakerIds = event.speakerIds,
            participantsIds = event.participantsIds,
            participatedByMe = event.participatedByMe,
            attachment = AttachmentEmbeddable.fromDto(event.attachment),
            link = event.link,
            ownedByMe = event.ownedByMe,
            users = event.users
        )
    }

    fun toDto() = Event(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        authorJob = authorJob,
        content = content,
        datetime = datetime,
        published = published,
        coords = coords?.toDto(),
        type = Event.Type.valueOf(eventType),
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        speakerIds = speakerIds,
        participantsIds = participantsIds,
        participatedByMe = participatedByMe,
        attachment = attachment?.toDto(),
        link = link,
        ownedByMe = ownedByMe,
        users = users
    )
}

fun List<EventEntity>.toDto(): List<Event> = map(EventEntity::toDto)
fun List<Event>.toEntity(): List<EventEntity> = map(EventEntity::fromDto)