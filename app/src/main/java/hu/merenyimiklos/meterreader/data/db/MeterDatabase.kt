package hu.merenyimiklos.meterreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import hu.merenyimiklos.meterreader.data.UserDataScope

@Database(
    entities = [MeterReadingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MeterDatabase : RoomDatabase() {
    abstract fun meterReadingDao(): MeterReadingDao

    companion object {
        private val instances = mutableMapOf<String, MeterDatabase>()

        // The null scope opens only the existing, unowned pre-upgrade database.
        // New readings must always use a non-null Firebase UID.
        fun getInstance(
            context: Context,
            userUid: String? = null
        ): MeterDatabase {
            val name = if (userUid == null) {
                "meter_reader.db"
            } else {
                "meter_reader_${UserDataScope.storageId(userUid)}.db"
            }

            return synchronized(this) {
                instances.getOrPut(name) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        MeterDatabase::class.java,
                        name
                    ).build()
                }
            }
        }
    }
}
