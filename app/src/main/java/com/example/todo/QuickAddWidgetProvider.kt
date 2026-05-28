package com.example.todo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Dostawca widgetu "Quick Add" (Szybkie dodawanie).
 * Umożliwia użytkownikowi uruchomienie ekranu tworzenia zadania bezpośrednio z pulpitu telefonu.
 */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAllWidgets(context)
        }
    }

    companion object {
        /**
         * Odświeża wszystkie instancje widgetu "Quick Add" na ekranie.
         * Podpina [PendingIntent] pod elementy UI widgetu, który otwiera aplikację
         * z instrukcją bezpośredniego przejścia do ekranu tworzenia zadania.
         */
        fun updateAllWidgets(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))

            // Intencja otwierająca MainActivity z flagą otwarcia ekranu "create"
            val createIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_create", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pi = PendingIntent.getActivity(
                context,
                1000,
                createIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            ids.forEach { id ->
                val rv = RemoteViews(context.packageName, R.layout.widget_quick_add)
                // Podpięcie akcji kliknięcia pod widoki w layoucie XML
                rv.setOnClickPendingIntent(R.id.widget_root, pi)
                rv.setOnClickPendingIntent(R.id.widget_add_button, pi)
                mgr.updateAppWidget(id, rv)
            }
        }
    }
}