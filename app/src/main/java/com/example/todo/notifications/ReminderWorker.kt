package com.example.todo.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.work.Data

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val title = inputData.getString("title") ?: "ToDo"
            val body = inputData.getString("body") ?: ""
            NotificationHelper.createChannel(applicationContext)
            NotificationHelper.build(applicationContext, title, body, id = inputData.getInt("id", 0))
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