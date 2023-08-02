package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.User

@Entity
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val login: String,
    val name: String,
    val avatar: String? = null,
) {
    fun toDto(): User = User(
        id = id,
        login = login,
        name = name,
        avatar = avatar,
    )

    companion object {
        fun fromDto(user: User): UserEntity = UserEntity(
            id = user.id,
            login = user.login,
            name = user.name,
            avatar = user.avatar,
        )
    }
}

fun List<UserEntity>.toDto(): List<User> = map(UserEntity::toDto)
fun List<User>.toEntity(): List<UserEntity> = map(UserEntity::fromDto)


