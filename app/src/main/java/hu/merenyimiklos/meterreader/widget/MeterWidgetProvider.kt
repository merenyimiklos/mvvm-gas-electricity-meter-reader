package hu.merenyimiklos.meterreader.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import hu.merenyimiklos.meterreader.MainActivity
import hu.merenyimiklos.meterreader.R
import hu.merenyimiklos.meterreader.data.MeterRepository
import hu.merenyimiklos.meterreader.data.db.MeterDatabase
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MeterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()

        CoroutineScope(
            Dispatchers.IO
        ).launch {
            try {
                updateWidgets(
                    context,
                    appWidgetManager,
                    appWidgetIds
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAll(
            context: Context
        ) {
            CoroutineScope(
                Dispatchers.IO
            ).launch {
                val manager =
                    AppWidgetManager
                        .getInstance(
                            context
                        )
                val ids =
                    manager
                        .getAppWidgetIds(
                            ComponentName(
                                context,
                                MeterWidgetProvider::class.java
                            )
                        )

                updateWidgets(
                    context,
                    manager,
                    ids
                )
            }
        }

        private suspend fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            if (ids.isEmpty()) return

            val readings =
                MeterRepository(
                    MeterDatabase
                        .getInstance(
                            context
                        )
                ).getAll()

            val normal =
                latest(
                    readings,
                    MeterType.ELECTRICITY
                )
            val night =
                latest(
                    readings,
                    MeterType
                        .ELECTRICITY_NIGHT
                )
            val gas =
                latest(
                    readings,
                    MeterType.GAS
                )

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                ).apply {
                    action =
                        MainActivity
                            .ACTION_OPEN_ADD
                }

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    91,
                    intent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )

            ids.forEach {
                id ->
                val views =
                    RemoteViews(
                        context.packageName,
                        R.layout
                            .meter_widget
                    )

                views.setTextViewText(
                    R.id.widget_normal,
                    readingText(
                        "Normál",
                        normal
                    )
                )
                views.setTextViewText(
                    R.id.widget_night,
                    readingText(
                        "Éjszakai",
                        night
                    )
                )
                views.setTextViewText(
                    R.id.widget_gas,
                    readingText(
                        "Gáz",
                        gas
                    )
                )
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    pendingIntent
                )

                manager.updateAppWidget(
                    id,
                    views
                )
            }
        }

        private fun latest(
            readings: List<MeterReading>,
            type: MeterType
        ): MeterReading? =
            readings
                .filter {
                    it.type == type
                }
                .maxByOrNull {
                    it.dateEpochDay
                }

        private fun readingText(
            label: String,
            reading: MeterReading?
        ): String {
            if (reading == null) {
                return label +
                    ": –"
            }

            val formatter =
                NumberFormat
                    .getNumberInstance(
                        Locale(
                            "hu",
                            "HU"
                        )
                    )
                    .apply {
                        maximumFractionDigits =
                            2
                    }

            return label +
                ": " +
                formatter.format(
                    reading.value
                ) +
                " " +
                reading.type.unit
        }
    }
}
