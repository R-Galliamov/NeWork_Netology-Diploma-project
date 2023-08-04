package ru.netology.nework.converters

import androidx.room.TypeConverter

class GsonConverters {
    @TypeConverter
    fun fromString(value: String): List<Int> {
        return value.split(",").filter { it.isNotBlank() }.map { it.toInt() }
    }

    @TypeConverter
    fun fromList(list: List<Int>): String {
        return list.joinToString(",")
    }
}