package ru.netology.nework.converters

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeConverter {
    companion object {
        private fun publishedToDateTime(datetime: String): LocalDateTime {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            return LocalDateTime.parse(datetime, format)
        }

        private fun datetimeToDateTime(datetime: String): LocalDateTime {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            return LocalDateTime.parse(datetime, format)
        }

        fun publishedToUIDate(datetime: String): String {
            val format = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val dateTime = publishedToDateTime(datetime)
            return dateTime.format(format)
        }

        fun publishedToUiTime(datetime: String): String {
            val format = DateTimeFormatter.ofPattern("HH:mm")
            val dateTime = publishedToDateTime(datetime)
            return dateTime.format(format)
        }

        fun datetimeToUiDatetime(inputDate: String): String {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val date = inputDate.subSequence(0, 16)
            val dateTime = LocalDateTime.parse(date, format)
            val outputFormat = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            return dateTime.format(outputFormat)
        }
    }
}