package com.example.todo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Task
import com.example.todo.data.TaskStatus
import com.example.todo.notifications.GeofenceManager
import com.example.todo.notifications.ReminderScheduler
import com.example.todo.repo.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskViewModel(private val repo: TaskRepository, private val context: Context? = null) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    val visibleTasks: StateFlow<List<Task>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.observeAll() else repo.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tasks: StateFlow<List<Task>> = visibleTasks

    fun create(task: Task, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.insert(task)
            context?.let { ctx ->
                if (task.reminderTimeMillis != null) ReminderScheduler.scheduleReminder(ctx, task.copy(id = id))
                if (task.locationLat != null && task.locationLng != null) GeofenceManager.addGeofenceForTask(ctx, id,
                    task.locationLat!!,
                    task.locationLng!!
                )
            }
            onDone(id)
        }
    }

    fun update(task: Task) {
        viewModelScope.launch {
            repo.update(task)

            // reminders/geofence: cancel and (re)schedule
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
                if (task.reminderTimeMillis != null) ReminderScheduler.scheduleReminder(ctx, task)

                // remove previous geofence and add new if present
                GeofenceManager.removeGeofenceForTask(ctx, task.id)
                if (task.locationLat != null && task.locationLng != null) {
                    GeofenceManager.addGeofenceForTask(ctx, task.id, task.locationLat!!,
                        task.locationLng!!
                    )
                }
            }

            // jeśli oznaczono jako DONE i zadanie ma recurringRule -> utwórz kolejne wystąpienie
            if (task.status == TaskStatus.DONE && !task.recurringRule.isNullOrBlank()) {
                val nextDue = computeNextDue(task.dueAt, task.recurringRule!!)
                if (nextDue != null) {
                    val newTask = task.copy(
                        id = 0L,
                        createdAt = System.currentTimeMillis(),
                        dueAt = nextDue,
                        status = TaskStatus.PENDING
                    )
                    // wstaw nowe wystąpienie i zaplanuj przypomnienia/geofence dla niego
                    val newId = repo.insert(newTask)
                    context?.let { ctx2 ->
                        if (newTask.reminderTimeMillis != null) ReminderScheduler.scheduleReminder(ctx2, newTask.copy(id = newId))
                        if (newTask.locationLat != null && newTask.locationLng != null) GeofenceManager.addGeofenceForTask(ctx2, newId,
                            newTask.locationLat!!,
                            newTask.locationLng!!
                        )
                    }
                }
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repo.delete(task)
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
                GeofenceManager.removeGeofenceForTask(ctx, task.id)
            }
        }
    }

    fun getById(id: Long, callback: (Task?) -> Unit) {
        viewModelScope.launch {
            callback(repo.getById(id))
        }
    }

    private fun computeNextDue(dueAt: Long?, rule: String): Long? {
        if (dueAt == null) return null
        return try {
            val instant = Instant.ofEpochMilli(dueAt)
            val zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
            val next = when (rule.uppercase()) {
                "DAILY" -> zdt.plusDays(1)
                "WEEKLY" -> zdt.plusWeeks(1)
                "MONTHLY" -> zdt.plusMonths(1)
                else -> return null
            }
            next.toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    class Factory(private val repo: TaskRepository, private val context: Context? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repo, context) as T
        }
    }
}