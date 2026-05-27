package com.example.todo

import android.content.Context
import androidx.room.Room
import com.example.todo.data.AppDatabase

/**
 * Singleton dostarczający instancję bazy Room.
 *
 * Używa `fallbackToDestructiveMigration()` w trybie developmentu, aby uniknąć błędów migracji.
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    /** Zwraca instancję bazy, tworząc ją przy pierwszym użyciu. */
    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val inst = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todo-db")
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = inst
            inst
        }
    }
}