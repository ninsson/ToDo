package com.example.todo.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import com.example.todo.data.AppDatabase
import com.example.todo.DatabaseProvider
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

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
                    // używamy singletona DB
                    val db = DatabaseProvider.get(context)
                    val task = db.taskDao().getById(id)
                    task?.let {
                        NotificationHelper.createChannel(context)

                        // jeśli lokalizacje dostępne, przygotuj PendingIntent do nawigacji do konkretnego punktu
                        val loc = it.locations.getOrNull(locIdx)
                        val navPending: PendingIntent? = loc?.let { location ->
                            val uri = Uri.parse("google.navigation:q=${location.lat},${location.lng}")
                            val navIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            PendingIntent.getActivity(
                                context,
                                // requestCode: taskId + locIdx to make it unique-ish
                                (id + locIdx).toInt(),
                                navIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }

                        NotificationHelper.build(
                            context,
                            "Zadanie w pobliżu: ${it.title}",
                            it.description ?: "",
                            id = (it.id % Int.MAX_VALUE).toInt(),
                            contentIntent = navPending
                        )
                    }
                }
            }
        }
    }
}