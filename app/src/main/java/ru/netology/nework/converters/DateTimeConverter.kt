package ru.netology.nework.converters

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class DateTimeConverter {
    companion object {
        private fun publishedToDateTime(datetime: String): LocalDateTime {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            return LocalDateTime.parse(datetime, format)
        }

        private fun datetimeToLocalDateTime(datetime: String): LocalDateTime {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            return LocalDateTime.parse(datetime, format)
        }

        fun publishedToUiDate(inputDate: String): String {
            val format = DateTimeFormatter.ofPattern("dd MMM yyyy")
            val dateTime = publishedToDateTime(inputDate)
            return dateTime.format(format)
        }

        fun publishedToUiTime(datetime: String): String {
            val format = DateTimeFormatter.ofPattern("HH:mm")
            val dateTime = publishedToDateTime(datetime)
            return dateTime.format(format)
        }

        fun datetimeToUiDateTime(inputDate: String): String {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val date = inputDate.subSequence(0, 16)
            val dateTime = LocalDateTime.parse(date, format)
            val outputFormat = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            return dateTime.format(outputFormat)
        }

        fun datetimeToUiDate(inputDate: String): String {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val date = inputDate.subSequence(0, 16)
            val dateTime = LocalDateTime.parse(date, format)
            val outputFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")
            return dateTime.format(outputFormat)
        }

        fun uiDateToApiFormat(inputDate: String): String {
            val inputDateFormat = SimpleDateFormat("dd/MM/yyyy")
            val inputDate = inputDateFormat.parse(inputDate)
            val outputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
            outputDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return outputDateFormat.format(inputDate)
        }
    }
}