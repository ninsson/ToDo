package com.example.todo

import android.content.Context
import androidx.room.Room
import com.example.todo.data.AppDatabase

/**
 * Singleton dostarczający instancję bazy danych Room.
 * * Implementuje wzorzec Singleton, aby zapewnić współdzielenie jednego połączenia
 * do bazy danych przez całą aplikację.
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    /**
     * Zwraca instancję bazy danych, tworząc ją przy pierwszym wywołaniu.
     * Wykorzystuje blok [synchronized] do zapewnienia bezpiecznego dostępu w środowisku wielowątkowym.
     *
     * @param context Kontekst aplikacji.
     * @return Instancja [AppDatabase].
     */
    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val inst =
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todo-db")
                    .fallbackToDestructiveMigration(false)
                .build()
            INSTANCE = inst
            inst
        }
    }
}