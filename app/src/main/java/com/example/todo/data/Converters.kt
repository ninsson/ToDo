package com.example.todo.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException

class Converters {
    private val json = Json { encodeDefaults = true }

    @TypeConverter
    fun fromPriority(p: Priority?): String? = p?.name

    @TypeConverter
    fun toPriority(s: String?): Priority? = s?.let { Priority.valueOf(it) }

    @TypeConverter
    fun fromStatus(s: TaskStatus?): String? = s?.name

    @TypeConverter
    fun toStatus(s: String?): TaskStatus? = s?.let { TaskStatus.valueOf(it) }

    @TypeConverter
    fun fromAttachments(list: List<Attachment>?): String? =
        list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toAttachments(data: String?): List<Attachment> =
        if (data.isNullOrEmpty()) emptyList() else json.decodeFromString(data)
}