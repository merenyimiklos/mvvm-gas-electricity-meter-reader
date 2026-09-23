package hu.merenyimiklos.meterreader.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import hu.merenyimiklos.meterreader.MainActivity
import hu.merenyimiklos.meterreader.R
import hu.merenyimiklos.meterreader.data.MeterRepository
import hu.merenyimiklos.meterreader.data.SettingsRepository
import hu.merenyimiklos.meterreader.data.db.MeterDatabase
import hu.merenyimiklos.meterreader.model.MeterType
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

class ReadingReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        evaluateAndNotify(applicationContext)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID =
            "meter_reading_reminders"
        const val NOTIFICATION_ID = 2301

        suspend fun evaluateAndNotify(
            context: Context
        ) {
            val settings =
                SettingsRepository(context)
                    .settings
                    .first()

            if (!settings.reminderEnabled) {
                cancelNotification(context)
                return
            }

            val today = LocalDate.now()

            if (today.dayOfMonth < 23) {
                cancelNotification(context)
                return
            }

            val repository =
                MeterRepository(
                    MeterDatabase.getInstance(context)
                )
            val month = YearMonth.from(today)

            val completedTypes =
                repository.getAll()
                    .filter {
                        YearMonth.from(
                            LocalDate.ofEpochDay(
                                it.dateEpochDay
                            )
                        ) == month
                    }
                    .map { it.type }
                    .toSet()

            val missing =
                MeterType.entries.filter {
                    it !in completedTypes
                }

            if (missing.isEmpty()) {
                cancelNotification(context)
                return
            }

            showNotification(
                context = context,
                missing = missing
            )
        }

        private fun showNotification(
            context: Context,
            missing: List<MeterType>
        ) {
            if (
                Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            ensureChannel(context)

            val intent =
                Intent(
                    context,
                    MainActivity::class.java
                ).apply {
                    action =
                        MainActivity.ACTION_OPEN_ADD
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    23,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            val names =
                missing.joinToString(", ") {
                    it.shortName
                }

            val text =
                if (missing.size == 1) {
                    "Még nincs e havi " +
                        names.lowercase() +
                        " mérőállás."
                } else {
                    "Még hiányzik: " +
                        names +
                        "."
                }

            val notification =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(
                        R.mipmap.ic_launcher
                    )
                    .setContentTitle(
                        "Ideje leolvasni a mérőórákat"
                    )
                    .setContentText(text)
                    .setStyle(
                        NotificationCompat
                            .BigTextStyle()
                            .bigText(
                                text +
                                    " Nyisd meg az alkalmazást és rögzítsd a havi mérőállásokat."
                            )
                    )
                    .setAutoCancel(true)
                    .setContentIntent(
                        pendingIntent
                    )
                    .setPriority(
                        NotificationCompat
                            .PRIORITY_DEFAULT
                    )
                    .build()

            NotificationManagerCompat
                .from(context)
                .notify(
                    NOTIFICATION_ID,
                    notification
                )
        }

        private fun ensureChannel(
            context: Context
        ) {
            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O
            ) {
                return
            }

            val manager =
                context.getSystemService(
                    NotificationManager::class.java
                )

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Mérőóra emlékeztetők",
                    NotificationManager
                        .IMPORTANCE_DEFAULT
                ).apply {
                    description =
                        "Havi mérőállás-leolvasási emlékeztetők."
                }

            manager.createNotificationChannel(
                channel
            )
        }

        private fun cancelNotification(
            context: Context
        ) {
            NotificationManagerCompat
                .from(context)
                .cancel(NOTIFICATION_ID)
        }
    }
}
