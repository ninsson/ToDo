package com.example.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.todo.data.Converters
import kotlinx.serialization.Serializable

enum class TaskStatus { PENDING, DONE, ARCHIVED }
enum class Priority { LOW, MEDIUM, HIGH }

@Serializable
data class TaskLocation(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float = 100f,
    val label: String? = null
)

@Entity(tableName = "tasks")
@TypeConverters(Converters::class)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var title: String,
    var description: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var dueAt: Long? = null, // timestamp millis
    var status: TaskStatus = TaskStatus.PENDING,
    var category: String? = null,
    var priority: Priority = Priority.MEDIUM,
    // replaced single location with list of locations
    var locations: List<TaskLocation> = emptyList(),
    var recurringRule: String? = null, // RFC5545 or custom serial
    var attachments: List<Attachment> = emptyList(),
    var reminderTimeMillis: Long? = null // time-based reminder
)