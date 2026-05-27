package com.example.todo.repo

import com.example.todo.data.Task
import com.example.todo.data.TaskDao
import kotlinx.coroutines.flow.Flow

/**
 * Repozytorium zadań – cienka warstwa nad DAO.
 *
 * Abstrahuje dostęp do bazy i upraszcza użycie w ViewModelu.
 */
class TaskRepository(private val dao: TaskDao) {

    /** Obserwuj wszystkie zadania w bazie. */
    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    /** Pobierz zadanie po ID. */
    suspend fun getById(id: Long): Task? = dao.getById(id)

    /** Wstaw nowe zadanie i zwróć wygenerowane ID. */
    suspend fun insert(task: Task): Long = dao.insert(task)

    /** Zaktualizuj istniejące zadanie. */
    suspend fun update(task: Task) = dao.update(task)

    /** Usuń zadanie. */
    suspend fun delete(task: Task) = dao.delete(task)

    /** Wyszukaj po tytule lub opisie (LIKE). */
    fun search(q: String) = dao.search("%$q%")

    /** Obserwuj zadania o danym statusie. */
    fun observeByStatus(status: com.example.todo.data.TaskStatus) = dao.observeByStatus(status)
}