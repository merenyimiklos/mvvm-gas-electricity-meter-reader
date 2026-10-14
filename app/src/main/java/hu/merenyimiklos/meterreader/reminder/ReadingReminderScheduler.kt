package hu.merenyimiklos.meterreader.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReadingReminderScheduler {
    private const val WORK_NAME =
        "monthly_meter_reading_reminder"

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var nextRun =
            now.toLocalDate()
                .atTime(18, 0)

        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
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

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }
}
