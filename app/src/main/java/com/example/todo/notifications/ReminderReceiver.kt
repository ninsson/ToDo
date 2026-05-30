package com.example.todo.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todo.MainActivity
import com.example.todo.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

/**
 * Receiver uruchamiany przez AlarmManager.
 * Wyświetla powiadomienie, jeśli użytkownik ma włączone przypomnienia.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationsOn = runBlocking { SettingsRepository(context).isNotificationsEnabled() }
        if (!notificationsOn) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "ToDo"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""

        NotificationHelper.createChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_task_id", taskId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            (taskId % Int.MAX_VALUE).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper.build(
            context,
            title,
            body,
            id = (taskId % Int.MAX_VALUE).toInt(),
            contentIntent = openPending
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}