package ru.netology.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.dto.UserPreview

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val authorId: Int,
    val author: String,
    val authorAvatar: String?,
    val authorJob: String?,
    val content: String,
    val published: String,
    @Embedded
    var coords: CoordinatesEmbeddable?,
    val link: String?,
    val likeOwnerIds: List<Int>,
    val mentionIds: List<Int>,
    val mentionedMe: Boolean,
    val likedByMe: Boolean,
    @Embedded
    var attachment: AttachmentEmbeddable?,
    val users: Map<String, UserPreview>
) {
    fun toDto() = Post(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        authorJob = authorJob,
        content = content,
        published = published,
        coords = coords?.toDto(),
        link = link,
        likeOwnerIds = likeOwnerIds,
        mentionIds = mentionIds,
        mentionedMe = mentionedMe,
        likedByMe = likedByMe,
        attachment = attachment?.toDto(),
        users = users,
    )

    companion object {
        fun fromDto(post: Post) = PostEntity(
            id = post.id,
            authorId = post.authorId,
            author = post.author,
            authorAvatar = post.authorAvatar,
            authorJob = post.authorJob,
            content = post.content,
            published = post.published,
            coords = CoordinatesEmbeddable.fromDto(post.coords),
            link = post.link,
            likeOwnerIds = post.likeOwnerIds,
            mentionIds = post.mentionIds,
            mentionedMe = post.mentionedMe,
            likedByMe = post.likedByMe,
            attachment = AttachmentEmbeddable.fromDto(post.attachment),
            users = post.users
        )
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)

