package com.example.todo.repo

import com.example.todo.data.Task
import com.example.todo.data.TaskDao
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun observeAll(): Flow<List<Task>> = dao.observeAll()
    suspend fun getById(id: Long): Task? = dao.getById(id)
    suspend fun insert(task: Task): Long = dao.insert(task)
    suspend fun update(task: Task) = dao.update(task)
    suspend fun delete(task: Task) = dao.delete(task)
    fun search(q: String) = dao.search("%$q%")
    fun observeByStatus(status: com.example.todo.data.TaskStatus) = dao.observeByStatus(status)
}