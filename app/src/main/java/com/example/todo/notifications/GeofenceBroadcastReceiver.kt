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

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val notificationsOn = runBlocking { SettingsRepository(context).isNotificationsEnabled() }
        val locationOn = runBlocking { SettingsRepository(context).isLocationEnabled() }
        if (!notificationsOn || !locationOn) return

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggering = geofencingEvent.triggeringGeofences
            triggering?.forEach { gf ->
                // oczekujemy requestId w formacie "task_{id}_{idx}"
                val parts = gf.requestId.split("_")
                val id = parts.getOrNull(1)?.toLongOrNull() ?: return@forEach
                val locIdx = parts.getOrNull(2)?.toIntOrNull() ?: 0

                // pobierz task z DB i wyświetl powiadomienie
                CoroutineScope(Dispatchers.IO).launch {
                    val db = DatabaseProvider.get(context)
                    val task = db.taskDao().getById(id)
                    task?.let {
                        NotificationHelper.createChannel(context)

                        // contentIntent -> otwórz szczegóły zadania w aplikacji
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

                        // jeśli lokalizacja dostępna, przygotuj action "Nawiguj" otwierający Google Maps
                        val loc = it.locations.getOrNull(locIdx)
                        val navPending: PendingIntent? = loc?.let { location ->
                            val uri = Uri.parse("google.navigation:q=${location.lat},${location.lng}")
                            val navIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            PendingIntent.getActivity(
                                context,
                                // requestCode: unikalny-ish
                                ((it.id + locIdx) % Int.MAX_VALUE).toInt(),
                                navIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }

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