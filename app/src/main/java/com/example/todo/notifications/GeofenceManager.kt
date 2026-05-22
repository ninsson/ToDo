package com.example.todo.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlin.math.max

private const val TAG = "GeofenceManager"

object GeofenceManager {
    private fun geofencingClient(context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private fun makePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun addGeofenceForTask(context: Context, taskId: Long, lat: Double, lng: Double, radiusMeters: Float = 100f) {
        val geofence = Geofence.Builder()
            .setRequestId("task_$taskId")
            .setCircularRegion(lat, lng, max(50f, radiusMeters))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val client = geofencingClient(context)
        // first remove existing geofence with same id (best-effort), then add
        client.removeGeofences(listOf("task_$taskId"))
            .addOnSuccessListener {
                client.addGeofences(request, makePendingIntent(context))
                    .addOnSuccessListener {
                        Log.d(TAG, "Geofence added for task $taskId")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to add geofence for task $taskId: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to remove existing geofence for task $taskId: ${e.message}", e)
                // try to add anyway
                client.addGeofences(request, makePendingIntent(context))
                    .addOnSuccessListener { Log.d(TAG, "Geofence added for task $taskId (after remove failure)") }
                    .addOnFailureListener { ex ->
                        Log.w(TAG, "Failed to add geofence for task $taskId: ${ex.message}", ex)
                    }
            }
    }

    fun removeGeofenceForTask(context: Context, taskId: Long) {
        val client = geofencingClient(context)
        client.removeGeofences(listOf("task_$taskId"))
            .addOnSuccessListener { Log.d(TAG, "Geofence removed for task $taskId") }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to remove geofence for task $taskId: ${e.message}", e) }
    }
}