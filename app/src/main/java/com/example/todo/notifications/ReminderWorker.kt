package com.example.todo.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.work.Data
import com.example.todo.MainActivity
import android.content.Context
import com.example.todo.settings.SettingsRepository

/**
 * Worker wykonujący pracę w tle, której zadaniem jest wyświetlenie powiadomienia przypominającego.
 * Wywoływany przez [ReminderScheduler] przy użyciu WorkManager.
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    /**
     * Główna logika zadania wykonywana w tle.
     * Pobiera parametry zadania z wejścia i wyświetla powiadomienie użytkownikowi.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Sprawdzenie, czy powiadomienia są włączone w ustawieniach aplikacji
            val notificationsOn = SettingsRepository(applicationContext).isNotificationsEnabled()
            if (!notificationsOn) {
                return@withContext Result.success()
            }

            val title = inputData.getString("title") ?: "ToDo"
            val body = inputData.getString("body") ?: ""
            val notifId = inputData.getInt("id", 0)

            NotificationHelper.createChannel(applicationContext)

            // Przygotowanie PendingIntent, który otworzy aplikację po kliknięciu w powiadomienie
            val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                putExtra("open_task_id", notifId.toLong())
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPending = PendingIntent.getActivity(
                applicationContext,
                notifId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Wyświetlenie powiadomienia przez NotificationHelper
            NotificationHelper.build(
                applicationContext,
                title,
                body,
                id = notifId,
                contentIntent = openPending
            )
            Result.success()
        } catch (e: Exception) {
            // W razie błędu oznaczamy pracę jako nieudaną
            Result.failure()
        }
    }

    /**
     * Pomocnik do budowania obiektu danych wejściowych dla WorkManagera.
     */
    companion object {
        /**
         * Tworzy obiekt [Data] z informacjami o zadaniu.
         *
         * @param task Zadanie, dla którego tworzymy przypomnienie.
         * @return Obiekt Data z tytułem, treścią i ID.
         */
        fun buildInput(task: Task): Data {
            return Data.Builder()
                .putString("title", "Reminder: ${task.title}")
                .putString("body", task.description ?: "")
                .putInt("id", (task.id % Int.MAX_VALUE).toInt())
                .build()
        }
    }
}