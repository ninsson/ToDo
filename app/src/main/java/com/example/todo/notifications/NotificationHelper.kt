package com.example.todo.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "todo_channel"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "ToDo notifications", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(channel)
    }

    /**
     * contentIntent: opcjonalny PendingIntent uruchamiany po tapnięciu powiadomienia (np. otwarcie szczegółów zadania)
     * actionTitle/actionIntent: opcjonalna akcja przycisku (np. "Nawiguj" -> otwórz mapy)
     */
    fun build(
        context: Context,
        title: String,
        body: String,
        id: Int = 0,
        contentIntent: PendingIntent? = null,
        actionTitle: String? = null,
        actionIntent: PendingIntent? = null
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)

        contentIntent?.let { builder.setContentIntent(it) }
        if (actionIntent != null && !actionTitle.isNullOrBlank()) {
            // icon = 0 (brak), actionTitle, actionIntent
            builder.addAction(0, actionTitle, actionIntent)
        }

        nm.notify(id, builder.build())
    }
}