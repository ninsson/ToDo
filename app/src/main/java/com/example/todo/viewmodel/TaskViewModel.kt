package com.example.todo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Task
import com.example.todo.repo.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repo: TaskRepository) : ViewModel() {
    val tasks: StateFlow<List<Task>> = repo.observeAll()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun create(task: Task, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.insert(task)
            onDone(id)
        }
    }

    fun update(task: Task) {
        viewModelScope.launch { repo.update(task) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repo.delete(task) }
    }

    fun getById(id: Long, callback: (Task?) -> Unit) {
        viewModelScope.launch {
            callback(repo.getById(id))
        }
    }

    class Factory(private val repo: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repo) as T
        }
    }
}