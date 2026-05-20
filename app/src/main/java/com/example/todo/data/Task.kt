package com.example.todo.data

import androidx.room.*
import java.util.*

enum class TaskStatus { PENDING, DONE, ARCHIVED }
enum class Priority { LOW, MEDIUM, HIGH }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var title: String,
    var description: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var dueAt: Long? = null, // timestamp millis
    var status: TaskStatus = TaskStatus.PENDING,
    var category: String? = null,
    var priority: Priority = Priority.MEDIUM,
    var locationLat: Double? = null,
    var locationLng: Double? = null,
    var recurringRule: String? = null, // RFC5545 or custom serial
    var attachments: List<Attachment> = emptyList(),
    var reminderTimeMillis: Long? = null // time-based reminder
)