package com.example.todo.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Pomocnik służący do tworzenia i wyświetlania powiadomień w systemie.
 * Zawiera metody do inicjalizacji kanałów powiadomień oraz budowania ich treści.
 */
object NotificationHelper {
    /** Identyfikator kanału używanego przez aplikację. */
    const val CHANNEL_ID = "todo_channel"

    /**
     * Rejestruje kanał powiadomień w systemie Android.
     * Należy wywołać tę metodę przed pierwszym wyświetleniem powiadomienia.
     *
     * @param context Kontekst aplikacji.
     */
    fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ToDo notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)
    }

    /**
     * Tworzy i wyświetla powiadomienie dla użytkownika.
     *
     * @param context Kontekst aplikacji.
     * @param title Tytuł wyświetlany w powiadomieniu.
     * @param body Treść (opis) powiadomienia.
     * @param id Unikalny identyfikator powiadomienia (pozwala na aktualizację lub usunięcie konkretnego dymku).
     * @param contentIntent Opcjonalna akcja wyzwalana po kliknięciu w treść powiadomienia.
     * @param actionTitle Opcjonalna nazwa przycisku akcji (np. "Nawiguj").
     * @param actionIntent Opcjonalna akcja wykonywana po kliknięciu w przycisk akcji.
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
            // Dodanie przycisku akcji do powiadomienia
            builder.addAction(0, actionTitle, actionIntent)
        }

        nm.notify(id, builder.build())
    }
}