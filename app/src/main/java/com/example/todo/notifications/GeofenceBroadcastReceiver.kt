package com.example.todo.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.todo.DatabaseProvider
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.todo.MainActivity
import com.example.todo.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

/**
 * BroadcastReceiver obsługujący zdarzenia wejścia w zdefiniowane strefy geofencing.
 * Po wykryciu wejścia w obszar zadania, powiadamia użytkownika i oferuje opcję nawigacji.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    /**
     * Wywoływane przez system Android, gdy nastąpi zdarzenie geofencing.
     *
     * @param context Kontekst aplikacji.
     * @param intent Intencja zawierająca szczegóły zdarzenia geofencing.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        // Sprawdzenie ustawień użytkownika przed wyświetleniem powiadomienia
        val notificationsOn = runBlocking { SettingsRepository(context).isNotificationsEnabled() }
        val locationOn = runBlocking { SettingsRepository(context).isLocationEnabled() }
        if (!notificationsOn || !locationOn) return

        val transition = geofencingEvent.geofenceTransition

        // Obsługa zdarzenia wejścia w obszar geofence
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggering = geofencingEvent.triggeringGeofences
            triggering?.forEach { gf ->
                // Rozbicie ID geofence na ID zadania i indeks lokalizacji (format: "gf_ID_INDEX")
                val parts = gf.requestId.split("_")
                val id = parts.getOrNull(1)?.toLongOrNull() ?: return@forEach
                val locIdx = parts.getOrNull(2)?.toIntOrNull() ?: 0

                // Pobranie danych z bazy w tle
                CoroutineScope(Dispatchers.IO).launch {
                    val db = DatabaseProvider.get(context)
                    val task = db.taskDao().getById(id)

                    task?.let {
                        NotificationHelper.createChannel(context)

                        // Intent otwierający aplikację po kliknięciu w powiadomienie
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_task_id", it.id)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        val openPending = PendingIntent.getActivity(
                            context,
                            (it.id % Int.MAX_VALUE).toInt(),
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Opcjonalny Intent nawigacji do Map Google
                        val loc = it.locations.getOrNull(locIdx)
                        val navPending: PendingIntent? = loc?.let { location ->
                            val uri = Uri.parse("google.navigation:q=${location.lat},${location.lng}")
                            val navIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            PendingIntent.getActivity(
                                context,
                                ((it.id + locIdx) % Int.MAX_VALUE).toInt(),
                                navIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }

                        // Wyświetlenie gotowego powiadomienia
                        NotificationHelper.build(
                            context,
                            "Zadanie w pobliżu: ${it.title}",
                            it.description ?: "",
                            id = (it.id % Int.MAX_VALUE).toInt(),
                            contentIntent = openPending,
                            actionTitle = if (navPending != null) "Nawiguj" else null,
                            actionIntent = navPending
                        )
                    }
                }
            }
        }
    }
}