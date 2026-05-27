package com.example.todo.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.todo.data.TaskLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlin.math.max

private const val TAG = "GeofenceManager"
private const val PREFS_NAME = "geofence_prefs"
private const val PREF_KEY_PREFIX = "task_geofences_"

object GeofenceManager {
    private fun geofencingClient(context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private fun makePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Dodaj geofence'y dla danego zadania. Przechowujemy requestId list w SharedPreferences,
     * żeby móc je później usunąć.
     *
     * requestId format: "task_{taskId}_{index}"
     */
    fun addGeofencesForTask(context: Context, taskId: Long, locations: List<TaskLocation>) {
        if (locations.isEmpty()) return

        val client = geofencingClient(context)
        val pending = makePendingIntent(context)
        val newIds = locations.mapIndexed { idx, _ -> "task_${taskId}_$idx" }

        // remove previous ones (best-effort)
        val oldIds = prefs(context).getStringSet(PREF_KEY_PREFIX + taskId, emptySet())?.toList() ?: emptyList()
        if (oldIds.isNotEmpty()) {
            client.removeGeofences(oldIds).addOnSuccessListener {
                Log.d(TAG, "Removed old geofences for task $taskId")
            }.addOnFailureListener { e ->
                Log.w(TAG, "Failed to remove old geofences for task $taskId: ${e.message}", e)
            }
        }

        // build request with all geofences
        val builder = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        locations.forEachIndexed { idx, loc ->
            val id = "task_${taskId}_$idx"
            val gf = Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(loc.lat, loc.lng, max(50f, loc.radiusMeters))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            builder.addGeofence(gf)
        }
        val request = builder.build()

        client.addGeofences(request, pending)
            .addOnSuccessListener {
                Log.d(TAG, "Geofences added for task $taskId (count=${locations.size})")
                // store ids
                prefs(context).edit().putStringSet(PREF_KEY_PREFIX + taskId, newIds.toSet()).apply()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to add geofences for task $taskId: ${e.message}", e)
            }
    }

    /**
     * Usuń wszystkie geofence'y powiązane z zadaniem (korzysta z listy requestId w prefs).
     */
    fun removeGeofencesForTask(context: Context, taskId: Long) {
        val client = geofencingClient(context)
        val key = PREF_KEY_PREFIX + taskId
        val ids = prefs(context).getStringSet(key, emptySet())?.toList() ?: emptyList()
        if (ids.isNotEmpty()) {
            client.removeGeofences(ids)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofences removed for task $taskId")
                    prefs(context).edit().remove(key).apply()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to remove geofences for task $taskId: ${e.message}", e)
                    // still remove key to avoid stale ids (optional)
                    prefs(context).edit().remove(key).apply()
                }
        }
    }

    /**
     * Usuń wszystkie geofence'y aplikacji (przy wyłączeniu lokalizacji).
     */
    fun removeAllGeofences(context: Context) {
        val client = geofencingClient(context)
        val pending = makePendingIntent(context)
        client.removeGeofences(pending)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
                prefs(context).edit().clear().apply()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to remove all geofences: ${e.message}", e)
            }
    }
}