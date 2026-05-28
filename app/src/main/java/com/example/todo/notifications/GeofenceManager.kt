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

/**
 * Singleton zarządzający cyklem życia geofence'ów.
 * Odpowiada za rejestrację stref w systemie Google Location Services oraz ich usuwanie.
 */
object GeofenceManager {

    /**
     * Zwraca instancję klienta geofencingu.
     */
    private fun geofencingClient(context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    /**
     * Tworzy PendingIntent, który uruchamia [GeofenceBroadcastReceiver] przy wystąpieniu zdarzenia.
     */
    private fun makePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Zwraca SharedPreferences do przechowywania ID zarejestrowanych geofence'ów.
     */
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Rejestruje geofence'y dla podanego zadania.
     * Automatycznie usuwa poprzednio zarejestrowane strefy dla tego samego ID zadania,
     * aby uniknąć duplikatów.
     *
     * @param context Kontekst aplikacji.
     * @param taskId Unikalny identyfikator zadania.
     * @param locations Lista lokalizacji powiązanych z zadaniem.
     */
    fun addGeofencesForTask(context: Context, taskId: Long, locations: List<TaskLocation>) {
        if (locations.isEmpty()) return

        val client = geofencingClient(context)
        val pending = makePendingIntent(context)
        val newIds = locations.mapIndexed { idx, _ -> "task_${taskId}_$idx" }

        // Usunięcie starych geofence'ów przed dodaniem nowych (best-effort)
        val oldIds = prefs(context).getStringSet(PREF_KEY_PREFIX + taskId, emptySet())?.toList() ?: emptyList()
        if (oldIds.isNotEmpty()) {
            client.removeGeofences(oldIds).addOnSuccessListener {
                Log.d(TAG, "Removed old geofences for task $taskId")
            }.addOnFailureListener { e ->
                Log.w(TAG, "Failed to remove old geofences for task $taskId: ${e.message}", e)
            }
        }

        // Budowa żądania geofencingu
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

        // Rejestracja w API Google
        client.addGeofences(request, pending)
            .addOnSuccessListener {
                Log.d(TAG, "Geofences added for task $taskId (count=${locations.size})")
                // Zapisanie ID w preferencjach do późniejszego usunięcia
                prefs(context).edit().putStringSet(PREF_KEY_PREFIX + taskId, newIds.toSet()).apply()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to add geofences for task $taskId: ${e.message}", e)
            }
    }

    /**
     * Usuwa wszystkie geofence'y powiązane z konkretnym zadaniem.
     *
     * @param context Kontekst aplikacji.
     * @param taskId ID zadania, którego strefy mają zostać usunięte.
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
                    prefs(context).edit().remove(key).apply()
                }
        }
    }

    /**
     * Czyści wszystkie zarejestrowane geofence'y dla całej aplikacji.
     * Używane np. przy całkowitym wyłączeniu funkcji lokalizacji w aplikacji.
     *
     * @param context Kontekst aplikacji.
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