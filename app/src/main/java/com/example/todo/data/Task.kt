package com.example.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.todo.data.Converters
import kotlinx.serialization.Serializable

/**
 * Status zadania w cyklu życia: do zrobienia, wykonane lub archiwalne.
 *
 * Wykorzystywany w filtrach listy oraz do logiki ponownego tworzenia zadań cyklicznych.
 */
enum class TaskStatus { PENDING, DONE, ARCHIVED }

/**
 * Priorytet zadania używany do sortowania i wizualnego wyróżniania na liście.
 */
enum class Priority { LOW, MEDIUM, HIGH }

/**
 * Lokalizacja powiązana z zadaniem.
 *
 * @property lat szerokość geograficzna
 * @property lng długość geograficzna
 * @property radiusMeters promień geofence (domyślnie 100 m)
 * @property label etykieta przyjazna użytkownikowi (opcjonalna)
 */
@Serializable
data class TaskLocation(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float = 100f,
    val label: String? = null
)

/**
 * Encja Room reprezentująca zadanie w bazie.
 *
 * Zawiera dane potrzebne do listy, szczegółów, powiadomień czasowych i lokalizacyjnych
 * oraz obsługi zadań cyklicznych i załączników.
 */
@Entity(tableName = "tasks")
@TypeConverters(Converters::class)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var title: String,
    var description: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var dueAt: Long? = null,
    var status: TaskStatus = TaskStatus.PENDING,
    var category: String? = null,
    var priority: Priority = Priority.MEDIUM,
    var locations: List<TaskLocation> = emptyList(),
    var recurringRule: String? = null,
    var attachments: List<Attachment> = emptyList(),
    var reminderTimeMillis: Long? = null
)