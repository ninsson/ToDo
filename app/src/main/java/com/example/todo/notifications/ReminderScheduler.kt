package com.example.todo.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.todo.data.Task

/**
 * Singleton odpowiedzialny za planowanie czasowych przypomnień o zadaniach.
 * Korzysta z AlarmManager, aby wyzwalać powiadomienia o zadanej godzinie.
 */
object ReminderScheduler {
    private const val PREFS_NAME = "reminder_prefs"
    private const val KEY_IDS = "scheduled_ids"

    /**
     * Planuje przypomnienie o określonej godzinie.
     * Jeśli istnieje poprzednie przypomnienie dla tego zadania, zostanie nadpisane.
     */
    fun scheduleReminder(context: Context, task: Task) {
        val time = task.reminderTimeMillis ?: return
        if (time <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, "Reminder: ${task.title}")
            putExtra(ReminderReceiver.EXTRA_BODY, task.description ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dokładne wywołanie (działa też w Doze)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)

        // zapisz ID do późniejszego cancelAll
        saveScheduledId(context, task.id)
    }

    /**
     * Anuluje konkretne przypomnienie dla wybranego zadania.
     */
    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        removeScheduledId(context, taskId)
    }

    /**
     * Usuwa wszystkie zaplanowane przypomnienia w aplikacji.
     */
    fun cancelAllReminders(context: Context) {
        val ids = getScheduledIds(context)
        ids.forEach { id -> cancelReminder(context, id) }
        clearScheduledIds(context)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getScheduledIds(context: Context): Set<Long> {
        val set = prefs(context).getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun saveScheduledId(context: Context, id: Long) {
        val set = prefs(context).getStringSet(KEY_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(id.toString())
        prefs(context).edit().putStringSet(KEY_IDS, set).apply()
    }

    private fun removeScheduledId(context: Context, id: Long) {
        val set = prefs(context).getStringSet(KEY_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (set.remove(id.toString())) {
            prefs(context).edit().putStringSet(KEY_IDS, set).apply()
        }
    }

    private fun clearScheduledIds(context: Context) {
        prefs(context).edit().remove(KEY_IDS).apply()
    }
}