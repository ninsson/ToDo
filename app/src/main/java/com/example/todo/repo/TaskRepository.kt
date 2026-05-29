package com.example.todo.repo

import com.example.todo.data.Task
import com.example.todo.data.TaskDao
import com.example.todo.data.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repozytorium zadań – cienka warstwa abstrakcji nad [TaskDao].
 *
 * Odpowiada za dostarczanie danych do ViewModelu i izoluje warstwę prezentacji
 * od szczegółów implementacji bazy danych.
 *
 * @param dao Interfejs dostępu do danych (Room DAO).
 */
class TaskRepository(private val dao: TaskDao) {

    /** * Zwraca strumień wszystkich zadań w bazie.
     * Użyj w ViewModelu, aby reagować na każdą zmianę danych w czasie rzeczywistym.
     */
    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    /** * Pobiera jednorazowy stan zadania o podanym identyfikatorze.
     * @param id Unikalne ID zadania.
     */
    suspend fun getById(id: Long): Task? = dao.getById(id)

    /** * Wstawia nowe zadanie do bazy danych.
     * @return Wygenerowane przez bazę ID wstawionego wiersza.
     */
    suspend fun insert(task: Task): Long = dao.insert(task)

    /** * Aktualizuje parametry istniejącego zadania.
     */
    suspend fun update(task: Task) = dao.update(task)

    /** * Usuwa zadanie z bazy danych.
     */
    suspend fun delete(task: Task) = dao.delete(task)

    /** * Wyszukuje zadania zawierające frazę w tytule lub opisie.
     * @param q Fraza wyszukiwania.
     */
    fun search(q: String) = dao.search("%$q%")

    /** * Obserwuje zadania przefiltrowane według statusu.
     * @param status Status zadania (np. PENDING, COMPLETED).
     */
    fun observeByStatus(status: TaskStatus) = dao.observeByStatus(status)
}