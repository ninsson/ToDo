package com.example.todo.notifications

import android.content.Context
import androidx.work.*
import com.example.todo.data.Task
import java.util.concurrent.TimeUnit

/**
 * Singleton odpowiedzialny za planowanie czasowych przypomnień o zadaniach.
 * Korzysta z WorkManager, aby zapewnić, że przypomnienia zostaną wyzwolone nawet po zamknięciu aplikacji.
 */
object ReminderScheduler {
    private const val TAG_REMINDER = "reminder"

    /**
     * Planuje jednorazowe zadanie przypomnienia o określonej godzinie.
     * Jeśli dla danego zadania istnieje już przypomnienie, zostanie ono zastąpione nowym.
     *
     * @param context Kontekst aplikacji.
     * @param task Zadanie, o którym należy przypomnieć.
     */
    fun scheduleReminder(context: Context, task: Task) {
        val time = task.reminderTimeMillis ?: return
        val delay = time - System.currentTimeMillis()

        // Jeśli czas przypomnienia już minął, nie planujemy zadania
        if (delay <= 0) return

        val data = ReminderWorker.buildInput(task)
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG_REMINDER)
            .build()

        // Używamy UniqueWork, aby uniknąć duplikowania przypomnień dla tego samego zadania
        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_task_${task.id}",
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    /**
     * Anuluje konkretne przypomnienie dla wybranego zadania.
     *
     * @param context Kontekst aplikacji.
     * @param taskId ID zadania, którego przypomnienie ma zostać anulowane.
     */
    fun cancelReminder(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_task_$taskId")
    }

    /**
     * Usuwa wszystkie zaplanowane przypomnienia w aplikacji.
     *
     * @param context Kontekst aplikacji.
     */
    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER)
    }
}