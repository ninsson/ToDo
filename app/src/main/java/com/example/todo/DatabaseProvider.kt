package com.example.todo

import android.content.Context
import androidx.room.Room
import com.example.todo.data.AppDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val inst = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todo-db")
                // na etapie develop używamy fallbackToDestructiveMigration aby uniknąć exception przy zmianie schematu
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = inst
            inst
        }
    }
}