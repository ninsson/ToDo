package com.example.todo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Baza danych Room dla aplikacji ToDo.
 *
 * Zawiera jedną tabelę `tasks` i używa konwerterów JSON dla list załączników i lokalizacji.
 */
@Database(entities = [Task::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** Dostęp do DAO zadań. */
    abstract fun taskDao(): TaskDao
}