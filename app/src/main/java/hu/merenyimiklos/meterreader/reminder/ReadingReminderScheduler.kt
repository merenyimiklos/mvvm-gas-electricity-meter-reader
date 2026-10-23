package hu.merenyimiklos.meterreader.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import hu.merenyimiklos.meterreader.model.BillingSettings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReadingReminderScheduler {
    private const val WORK_NAME =
        "monthly_meter_reading_reminder"

    fun configure(
        context: Context,
        settings: BillingSettings
    ) {
        val workManager =
            WorkManager.getInstance(context)

        if (!settings.reminderEnabled) {
            workManager.cancelUniqueWork(
                WORK_NAME
            )
            return
        }

        val now = LocalDateTime.now()
        var nextRun =
            now.toLocalDate()
                .atTime(
                    settings.reminderHour
                        .coerceIn(0, 23),
                    settings.reminderMinute
                        .coerceIn(0, 59)
                )

        if (!nextRun.isAfter(now)) {
            nextRun =
                nextRun.plusDays(1)
        }

        val initialDelay =
            Duration.between(
                now,
                nextRun
            ).toMillis()

        val request =
            PeriodicWorkRequestBuilder<
                ReadingReminderWorker
            >(
                24,
                TimeUnit.HOURS
            )
                .setInitialDelay(
                    initialDelay,
                    TimeUnit.MILLISECONDS
                )
                .build()

        workManager
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }
}
