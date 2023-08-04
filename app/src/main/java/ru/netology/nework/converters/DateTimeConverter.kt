package ru.netology.nework.converters

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeConverter {
    companion object {
        private fun toDateTime(datetime: String): LocalDateTime {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            return LocalDateTime.parse(datetime, format)
        }

        fun toUIDate(string: String): String {
            val userFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val dateTime = toDateTime(string)
            return dateTime.format(userFormat)
        }

        fun toUiTime(string: String) : String {
            val userFormat = DateTimeFormatter.ofPattern("HH:mm")
            val dateTime = toDateTime(string)
            return dateTime.format(userFormat)
        }

    }
}