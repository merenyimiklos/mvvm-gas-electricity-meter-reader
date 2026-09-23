package hu.merenyimiklos.meterreader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType

@Entity(tableName = "meter_readings")
data class MeterReadingEntity(
    @PrimaryKey val id: String,
    val type: String,
    val value: Double,
    val dateEpochDay: Long,
    val note: String,
    val photoUri: String?,
    val createdAtMillis: Long
)

fun MeterReadingEntity.toModel(): MeterReading = MeterReading(
    id = id,
    type = runCatching { MeterType.valueOf(type) }.getOrDefault(MeterType.ELECTRICITY),
    value = value,
    dateEpochDay = dateEpochDay,
    note = note,
    photoUri = photoUri,
    createdAtMillis = createdAtMillis
)

fun MeterReading.toEntity(): MeterReadingEntity = MeterReadingEntity(
    id = id,
    type = type.name,
    value = value,
    dateEpochDay = dateEpochDay,
    note = note,
    photoUri = photoUri,
    createdAtMillis = createdAtMillis
)
