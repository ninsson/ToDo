package com.example.todo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Attachment
import com.example.todo.data.Priority
import com.example.todo.data.Task
import com.example.todo.data.TaskLocation
import com.example.todo.data.TaskStatus
import com.example.todo.notifications.GeofenceManager
import com.example.todo.notifications.ReminderScheduler
import com.example.todo.repo.TaskRepository
import com.example.todo.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class SortOption { PRIORITY, DUE_DATE, CREATED_AT }
enum class FilterOption { ALL, TODAY, OVERDUE, HIGH_PRIORITY, PENDING, DONE }

class TaskViewModel(private val repo: TaskRepository, private val context: Context? = null) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.PRIORITY)
    private val _filterOption = MutableStateFlow(FilterOption.ALL)

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSortOption(s: SortOption) { _sortOption.value = s }
    fun setFilterOption(f: FilterOption) { _filterOption.value = f }

    // combine repo.observeAll() with search/filter/sort
    val visibleTasks: StateFlow<List<Task>> = combine(
        repo.observeAll(),
        _searchQuery.debounce(250),
        _sortOption,
        _filterOption
    ) { list, q, sortOpt, filterOpt ->
        var result = list

        // search
        val query = q.trim().lowercase()
        if (query.isNotEmpty()) {
            result = result.filter { t ->
                t.title.lowercase().contains(query) || (t.description?.lowercase()?.contains(query) ?: false)
            }
        }

        // filter
        val now = System.currentTimeMillis()
        fun Long.toLocalDate(): LocalDate =
            Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

        result = when (filterOpt) {
            FilterOption.ALL -> result
            FilterOption.TODAY -> {
                val today = LocalDate.now()
                result.filter { it.dueAt != null && it.dueAt!!.toLocalDate() == today }
            }
            FilterOption.OVERDUE -> result.filter { it.dueAt != null && it.dueAt!! < now && it.status != TaskStatus.DONE }
            FilterOption.HIGH_PRIORITY -> result.filter { it.priority == Priority.HIGH }
            FilterOption.PENDING -> result.filter { it.status == TaskStatus.PENDING }
            FilterOption.DONE -> result.filter { it.status == TaskStatus.DONE }
        }

        // sort
        val sorted = when (sortOpt) {
            SortOption.PRIORITY -> {
                result.sortedWith(Comparator { a, b ->
                    val pCmp = b.priority.ordinal.compareTo(a.priority.ordinal)
                    if (pCmp != 0) return@Comparator pCmp
                    val da = a.dueAt ?: Long.MAX_VALUE
                    val db = b.dueAt ?: Long.MAX_VALUE
                    return@Comparator da.compareTo(db)
                })
            }
            SortOption.DUE_DATE -> {
                result.sortedWith(Comparator { a, b ->
                    val da = a.dueAt ?: Long.MAX_VALUE
                    val db = b.dueAt ?: Long.MAX_VALUE
                    val dCmp = da.compareTo(db)
                    if (dCmp != 0) return@Comparator dCmp
                    return@Comparator b.priority.ordinal.compareTo(a.priority.ordinal)
                })
            }
            SortOption.CREATED_AT -> {
                result.sortedByDescending { it.createdAt }
            }
        }

        val (notDone, done) = sorted.partition { it.status != TaskStatus.DONE }
        notDone + done
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tasks: StateFlow<List<Task>> = visibleTasks

    fun create(task: Task, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val taskToInsert = if (!task.recurringRule.isNullOrBlank() && task.dueAt == null) {
                task.copy(dueAt = task.createdAt)
            } else {
                task
            }

            val id = repo.insert(taskToInsert)
            context?.let { ctx ->
                val settings = SettingsRepository(ctx)
                val notificationsOn = settings.isNotificationsEnabled()
                val locationOn = settings.isLocationEnabled()

                if (notificationsOn && taskToInsert.reminderTimeMillis != null) {
                    ReminderScheduler.scheduleReminder(ctx, taskToInsert.copy(id = id))
                }
                if (locationOn && taskToInsert.locations.isNotEmpty()) {
                    GeofenceManager.addGeofencesForTask(ctx, id, taskToInsert.locations)
                }
            }
            onDone(id)
        }
    }

    fun update(task: Task) {
        viewModelScope.launch {
            repo.update(task)
            context?.let { ctx ->
                val settings = SettingsRepository(ctx)
                val notificationsOn = settings.isNotificationsEnabled()
                val locationOn = settings.isLocationEnabled()

                ReminderScheduler.cancelReminder(ctx, task.id)
                if (notificationsOn && task.reminderTimeMillis != null) {
                    ReminderScheduler.scheduleReminder(ctx, task)
                }

                // geofences
                GeofenceManager.removeGeofencesForTask(ctx, task.id)
                if (locationOn && task.locations.isNotEmpty()) {
                    GeofenceManager.addGeofencesForTask(ctx, task.id, task.locations)
                }
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repo.delete(task)
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
                GeofenceManager.removeGeofencesForTask(ctx, task.id)
            }
            deleteAttachmentFiles(task.attachments)
        }
    }

    fun getById(id: Long, callback: (Task?) -> Unit) {
        viewModelScope.launch {
            callback(repo.getById(id))
        }
    }

    private fun deleteAttachmentFiles(attachments: List<Attachment>) {
        attachments.forEach { att ->
            if (!att.localPath.startsWith("content://")) {
                runCatching { File(att.localPath).delete() }
            }
        }
    }

    private fun computeNextMillis(currentMillis: Long?, rule: String?): Long? {
        if (currentMillis == null || rule.isNullOrBlank()) return null
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(currentMillis).atZone(zone)

        return when (rule) {
            "codziennie" -> zdt.plusDays(1).toInstant().toEpochMilli()
            "co tydzień" -> zdt.plusWeeks(1).toInstant().toEpochMilli()
            "co miesiąc" -> zdt.plusMonths(1).toInstant().toEpochMilli()
            else -> {
                if (rule.startsWith("every:")) {
                    val parts = rule.split(":")
                    if (parts.size >= 3) {
                        val count = parts[1].toLongOrNull() ?: return null
                        when (parts[2]) {
                            "days" -> zdt.plusDays(count).toInstant().toEpochMilli()
                            "weeks" -> zdt.plusWeeks(count).toInstant().toEpochMilli()
                            "months" -> zdt.plusMonths(count).toInstant().toEpochMilli()
                            else -> null
                        }
                    } else null
                } else {
                    null
                }
            }
        }
    }

    fun completeTask(task: Task) {
        if (task.status == TaskStatus.DONE) return

        viewModelScope.launch {
            val done = task.copy(status = TaskStatus.DONE)
            repo.update(done)

            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, done.id)
                GeofenceManager.removeGeofencesForTask(ctx, done.id)
            }

            val rule = task.recurringRule
            if (!rule.isNullOrBlank()) {
                val baseMillis = task.dueAt ?: task.reminderTimeMillis ?: task.createdAt
                val nextBase = computeNextMillis(baseMillis, rule)
                if (nextBase != null) {
                    val reminderOffset: Long? = task.reminderTimeMillis?.let { it - baseMillis }
                    val nextReminder = if (reminderOffset != null) nextBase + reminderOffset else null

                    val newTask = task.copy(
                        id = 0L,
                        status = TaskStatus.PENDING,
                        createdAt = System.currentTimeMillis(),
                        dueAt = nextBase,
                        reminderTimeMillis = nextReminder,
                        // zachowujemy lokalizacje, żeby nowe wystąpienie miało te same geofence'y
                        locations = task.locations
                    )

                    create(newTask) { /* id async */ }
                }
            }
        }
    }

    fun reopenTask(task: Task) {
        if (task.status == TaskStatus.PENDING) return
        viewModelScope.launch {
            val reopened = task.copy(status = TaskStatus.PENDING)
            update(reopened)
        }
    }

    fun cyclePriority(task: Task) {
        val next = when (task.priority) {
            Priority.LOW -> Priority.MEDIUM
            Priority.MEDIUM -> Priority.HIGH
            Priority.HIGH -> Priority.LOW
        }
        update(task.copy(priority = next))
    }

    fun setPriority(task: Task, p: Priority) {
        update(task.copy(priority = p))
    }

    class Factory(private val repo: TaskRepository, private val context: Context? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repo, context) as T
        }
    }
}