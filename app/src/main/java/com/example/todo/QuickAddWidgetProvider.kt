package com.example.todo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "QuickAddWidget"

class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate: ids=${appWidgetIds.joinToString()}")
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "onReceive: ACTION_APPWIDGET_UPDATE")
            updateAllWidgets(context)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            Log.d(TAG, "updateAllWidgets: start")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DatabaseProvider.get(context.applicationContext)
                    val pendingCount = try {
                        db.taskDao().countByStatus(com.example.todo.data.TaskStatus.PENDING)
                    } catch (e: Exception) {
                        Log.w(TAG, "countByStatus failed, returning 0", e)
                        0
                    }

                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))

                    Log.d(TAG, "updateAllWidgets: widgetIds=${ids.joinToString()} pendingCount=$pendingCount")

                    ids.forEach { id ->
                        val rv = RemoteViews(context.packageName, R.layout.widget_quick_add)
                        rv.setTextViewText(R.id.widget_count, if (pendingCount == 0) context.getString(R.string.widget_count_zero) else "$pendingCount oczekujących")

                        // otwieramy MainActivity i wymuszamy nowy task + czyszczenie starego taska,
// aby zawsze wykonało się onCreate (startDestination będzie brany z intentu)
                        val createIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("open_create", true)
                            // wymuszamy pełne uruchomienie — spowoduje wyczyszczenie poprzedniego taska
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pi = PendingIntent.getActivity(
                            context,
                            1000,
                            createIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        rv.setOnClickPendingIntent(R.id.widget_add_button, pi)

                        // Kliknięcie tytułu -> otwórz aplikację główną
                        val mainPi = PendingIntent.getActivity(
                            context,
                            1001,
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        rv.setOnClickPendingIntent(R.id.widget_title, mainPi)

                        mgr.updateAppWidget(id, rv)
                        Log.d(TAG, "updateAllWidgets: updated widgetId=$id")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "updateAllWidgets: failed", e)
                }
            }
        }
    }
}