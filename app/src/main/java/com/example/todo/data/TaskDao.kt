package com.example.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO dla encji Task.
 *
 * Udostępnia strumienie obserwacji listy, CRUD oraz proste wyszukiwanie i liczniki.
 */
@Dao
interface TaskDao {
    /** Obserwuj wszystkie zadania posortowane po priorytecie i terminie. */
    @Query("SELECT * FROM tasks ORDER BY priority DESC, dueAt ASC")
    fun observeAll(): Flow<List<Task>>

    /** Pobierz pojedyncze zadanie po ID. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    /** Wstaw zadanie i zwróć wygenerowane ID. */
    @Insert
    suspend fun insert(task: Task): Long

    /** Zaktualizuj istniejące zadanie. */
    @Update
    suspend fun update(task: Task)

    /** Usuń zadanie. */
    @Delete
    suspend fun delete(task: Task)

    /** Obserwuj zadania o danym statusie. */
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueAt ASC")
    fun observeByStatus(status: TaskStatus): Flow<List<Task>>

    /** Wyszukiwanie po tytule lub opisie. */
    @Query("SELECT * FROM tasks WHERE title LIKE :query OR description LIKE :query ORDER BY dueAt ASC")
    fun search(query: String): Flow<List<Task>>

    /** Zlicz zadania o danym statusie (np. do badge/estatystyk). */
    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    suspend fun countByStatus(status: TaskStatus): Int
}