package com.example.todo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Task
import com.example.todo.notifications.ReminderScheduler
import com.example.todo.repo.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

class TaskViewModel(private val repo: TaskRepository, private val context: Context? = null) : ViewModel() {

    // search query exposed via setter
    private val _searchQuery = MutableStateFlow("")
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // visibleTasks: debounced search -> either repo.search or repo.observeAll
    val visibleTasks: StateFlow<List<Task>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.observeAll() else repo.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Backwards-compat alias (some screens used tasks)
    val tasks: StateFlow<List<Task>> = visibleTasks

    fun create(task: Task, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.insert(task)
            // schedule reminder (if any) using ReminderScheduler and known id
            if (context != null && task.reminderTimeMillis != null) {
                ReminderScheduler.scheduleReminder(context, task.copy(id = id))
            }
            onDone(id)
        }
    }

    fun update(task: Task) {
        viewModelScope.launch {
            repo.update(task)
            // reschedule reminder: cancel previous and schedule new if present
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
                if (task.reminderTimeMillis != null) {
                    ReminderScheduler.scheduleReminder(ctx, task)
                }
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repo.delete(task)
            // cancel reminder if any
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
            }
        }
    }

    fun getById(id: Long, callback: (Task?) -> Unit) {
        viewModelScope.launch {
            callback(repo.getById(id))
        }
    }

    class Factory(private val repo: TaskRepository, private val context: Context? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repo, context) as T
        }
    }
}