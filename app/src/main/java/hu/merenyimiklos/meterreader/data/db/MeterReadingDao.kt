package hu.merenyimiklos.meterreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY dateEpochDay ASC, createdAtMillis ASC")
    fun observeAll(): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings ORDER BY dateEpochDay ASC, createdAtMillis ASC")
    suspend fun getAll(): List<MeterReadingEntity>

    @Query("SELECT COUNT(*) FROM meter_readings")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reading: MeterReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(readings: List<MeterReadingEntity>)

    @Query("DELETE FROM meter_readings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM meter_readings")
    suspend fun deleteAll()
}
