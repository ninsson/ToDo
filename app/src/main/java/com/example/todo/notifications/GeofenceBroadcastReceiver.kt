package com.example.todo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.Geofence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.Room
import com.example.todo.data.AppDatabase

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggering = geofencingEvent.triggeringGeofences
            triggering?.forEach { gf ->
                // oczekujemy requestId w formacie "task_{id}"
                val id = gf.requestId.removePrefix("task_").toLongOrNull() ?: return@forEach

                // pobierz task z DB i wyświetl powiadomienie
                CoroutineScope(Dispatchers.IO).launch {
                    // PROTIP: lepiej mieć singleton AppDatabase; tu tworzymy db na szybko
                    val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todo-db")
                        .fallbackToDestructiveMigration()
                        .build()
                    val task = db.taskDao().getById(id)
                    task?.let {
                        NotificationHelper.createChannel(context)
                        NotificationHelper.build(
                            context,
                            "Lokalizacja: ${it.title}",
                            it.description ?: "",
                            id = (it.id % Int.MAX_VALUE).toInt()
                        )
                    }
                }
            }
        }
    }
}