package ru.netology.nework.entity

import ru.netology.nework.dto.Attachment

data class AttachmentEmbeddable(
    val url: String,
    val type: Attachment.Type,
) {
    fun toDto() = Attachment(url, type)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.type)
        }
    }
}
