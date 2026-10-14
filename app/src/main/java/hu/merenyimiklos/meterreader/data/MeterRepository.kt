package hu.merenyimiklos.meterreader.data

import androidx.room.withTransaction
import hu.merenyimiklos.meterreader.data.db.MeterDatabase
import hu.merenyimiklos.meterreader.data.db.toEntity
import hu.merenyimiklos.meterreader.data.db.toModel
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MeterRepository(
    private val database: MeterDatabase
) {
    private val dao = database.meterReadingDao()

    val readings: Flow<List<MeterReading>> = dao.observeAll()
        .map { rows -> rows.map { it.toModel() } }

    suspend fun getAll(): List<MeterReading> = dao.getAll().map { it.toModel() }

    suspend fun count(): Int = dao.count()

    suspend fun add(
        type: MeterType,
        value: Double,
        dateEpochDay: Long,
        note: String,
        photoUri: String?
    ) {
        dao.upsert(
            MeterReading(
                id = UUID.randomUUID().toString(),
                type = type,
                value = value,
                dateEpochDay = dateEpochDay,
                note = note.trim(),
                photoUri = photoUri
            ).toEntity()
        )
    }

    suspend fun update(reading: MeterReading) {
        dao.upsert(reading.copy(note = reading.note.trim()).toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun upsertAll(
        readings: List<MeterReading>
    ) {
        if (readings.isNotEmpty()) {
            dao.upsertAll(
                readings.map {
                    it.toEntity()
                }
            )
        }
    }

    suspend fun replaceAll(readings: List<MeterReading>) {
        database.withTransaction {
            dao.deleteAll()
            if (readings.isNotEmpty()) {
                dao.upsertAll(readings.map { it.toEntity() })
            }
        }
    }

    suspend fun clear() {
        dao.deleteAll()
    }
}
