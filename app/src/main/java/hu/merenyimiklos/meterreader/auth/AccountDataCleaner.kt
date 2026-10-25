package hu.merenyimiklos.meterreader.auth

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import hu.merenyimiklos.meterreader.data.MeterRepository
import hu.merenyimiklos.meterreader.data.SettingsRepository
import hu.merenyimiklos.meterreader.data.UserDataScope
import hu.merenyimiklos.meterreader.data.db.MeterDatabase
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.reminder.ReadingReminderScheduler
import hu.merenyimiklos.meterreader.reminder.ReadingReminderWorker
import hu.merenyimiklos.meterreader.widget.MeterWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Only removes data owned by the deleted Firebase UID. Never deletes a
 * family/shared-home folder or another account's backup.
 *
 * Exported CSV/Excel/manual JSON files cannot be revoked by the application.
 */
class AccountDataCleaner(private val context: Context) {
    suspend fun deleteOwnedData(uid: String): Boolean = withContext(Dispatchers.IO) {
        require(uid.isNotBlank() && uid != UserDataScope.DEBUG_GUEST)

        val key = UserDataScope.storageId(uid)
        val settingsRepository = SettingsRepository(context, uid)
        val oldSettings = settingsRepository.settings.first()

        // Delete readings and settings for precisely this account.
        MeterRepository(MeterDatabase.getInstance(context, uid)).clear()
        settingsRepository.reset()

        val localBackup = File(context.filesDir, "backups/auto-backup-$key.json")
        if (localBackup.exists()) {
            check(localBackup.delete()) { "A helyi biztonsági mentés nem törölhető." }
        }

        // Users who approved migration own the former unscoped legacy files.
        // Never touch those files when they were claimed by a different UID.
        val migration = context.getSharedPreferences("meter_reader_migration", Context.MODE_PRIVATE)
        if (migration.getString("legacy_claimed_by_uid", null) == uid) {
            MeterRepository(MeterDatabase.getInstance(context)).clear()
            SettingsRepository(context).reset()
            context.getSharedPreferences("meter_reader_store", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }

        // The previously-selected external backup folder may no longer be
        // writable. Report this separately; don't touch a shared-home folder.
        val externalBackupDeleted = if (oldSettings.backupFolderUri.isBlank()) {
            true
        } else {
            runCatching {
                val root = DocumentFile.fromTreeUri(
                    context,
                    Uri.parse(oldSettings.backupFolderUri)
                )
                val file = root?.findFile("auto-backup-$key.json")
                file == null || file.delete()
            }.getOrDefault(false)
        }

        ReadingReminderScheduler.configure(
            context,
            BillingSettings(reminderEnabled = false)
        )
        runCatching { ReadingReminderWorker.evaluateAndNotify(context) }
        MeterWidgetProvider.updateAll(context)
        externalBackupDeleted
    }
}
