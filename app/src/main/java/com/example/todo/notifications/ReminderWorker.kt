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

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val title = inputData.getString("title") ?: "ToDo"
            val body = inputData.getString("body") ?: ""
            val notifId = inputData.getInt("id", 0)

            NotificationHelper.createChannel(applicationContext)

            // zbuduj PendingIntent otwierający szczegóły zadania w aplikacji
            val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                putExtra("open_task_id", notifId.toLong())
                // FLAGy pomagają przywrócić istniejącą aktywność zamiast tworzyć nową
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPending = PendingIntent.getActivity(
                applicationContext,
                notifId, // requestCode - unikalne-ish
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationHelper.build(
                applicationContext,
                title,
                body,
                id = notifId,
                contentIntent = openPending
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun buildInput(task: Task): Data {
            return Data.Builder()
                .putString("title", "Reminder: ${task.title}")
                .putString("body", task.description ?: "")
                .putInt("id", (task.id % Int.MAX_VALUE).toInt())
                .build()
        }
    }
}