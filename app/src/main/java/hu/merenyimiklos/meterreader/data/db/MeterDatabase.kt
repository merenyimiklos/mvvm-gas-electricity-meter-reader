package hu.merenyimiklos.meterreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MeterReadingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MeterDatabase : RoomDatabase() {
    abstract fun meterReadingDao(): MeterReadingDao

    companion object {
        @Volatile
        private var instance: MeterDatabase? = null

        fun getInstance(context: Context): MeterDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeterDatabase::class.java,
                    "meter_reader.db"
                ).build().also { instance = it }
            }
    }
}
