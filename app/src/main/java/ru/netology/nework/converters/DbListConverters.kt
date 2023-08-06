package ru.netology.nework.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nework.dto.UserPreview

class DbListConverters {
    @TypeConverter
    fun fromString(value: String): List<Int> {
        return value.split(",").filter { it.isNotBlank() }.map { it.toInt() }
    }

    @TypeConverter
    fun fromList(list: List<Int>): String {
        return list.joinToString(",")
    }

    private val gson = Gson()

}

class DbMapConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): Map<String, UserPreview> {
        val type = object : TypeToken<Map<String, UserPreview>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMap(map: Map<String, UserPreview>): String {
        return gson.toJson(map)
    }

}

