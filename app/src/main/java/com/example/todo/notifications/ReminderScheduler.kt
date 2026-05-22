package com.example.todo.notifications

import android.content.Context
import androidx.work.*
import com.example.todo.data.Task
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun scheduleReminder(context: Context, task: Task) {
        val time = task.reminderTimeMillis ?: return
        val delay = time - System.currentTimeMillis()
        if (delay <= 0) return

        val data = ReminderWorker.buildInput(task)
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_task_${task.id}",
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun cancelReminder(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_task_$taskId")
    }
}