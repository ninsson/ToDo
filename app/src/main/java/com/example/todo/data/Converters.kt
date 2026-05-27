package com.example.todo.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Konwertery Room do zapisu złożonych typów w bazie.
 *
 * Enumy są zapisywane jako nazwy tekstowe, a listy (załączniki/lokalizacje) jako JSON.
 */
class Converters {
    private val json = Json { encodeDefaults = true }

    /** Zamienia priorytet na String do zapisu w bazie. */
    @TypeConverter
    fun fromPriority(p: Priority?): String? = p?.name

    /** Zamienia String z bazy na priorytet. */
    @TypeConverter
    fun toPriority(s: String?): Priority? = s?.let { Priority.valueOf(it) }

    /** Zamienia status na String do zapisu w bazie. */
    @TypeConverter
    fun fromStatus(s: TaskStatus?): String? = s?.name

    /** Zamienia String z bazy na status. */
    @TypeConverter
    fun toStatus(s: String?): TaskStatus? = s?.let { TaskStatus.valueOf(it) }

    /** Serializacja listy załączników do JSON. */
    @TypeConverter
    fun fromAttachments(list: List<Attachment>?): String? =
        list?.let { json.encodeToString(it) }

    /** Deserializacja listy załączników z JSON. */
    @TypeConverter
    fun toAttachments(data: String?): List<Attachment> =
        if (data.isNullOrEmpty()) emptyList() else json.decodeFromString(data)

    /** Serializacja listy lokalizacji do JSON. */
    @TypeConverter
    fun fromLocations(list: List<TaskLocation>?): String? =
        list?.let { json.encodeToString(it) }

    /** Deserializacja listy lokalizacji z JSON. */
    @TypeConverter
    fun toLocations(data: String?): List<TaskLocation> =
        if (data.isNullOrEmpty()) emptyList() else json.decodeFromString(data)
}