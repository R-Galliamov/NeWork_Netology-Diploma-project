package ru.netology.nework.dto


data class Attachment(
    val url: String,
    val type: Type,
) {
    enum class Type {
        IMAGE, VIDEO, AUDIO
    }
}
