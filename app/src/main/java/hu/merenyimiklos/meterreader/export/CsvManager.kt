package hu.merenyimiklos.meterreader.export

import android.content.ContentResolver
import android.net.Uri
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import java.time.LocalDate
import java.util.UUID

class CsvManager(
    private val contentResolver: ContentResolver
) {
    fun export(
        uri: Uri,
        readings: List<MeterReading>
    ) {
        val output = requireNotNull(
            contentResolver.openOutputStream(uri)
        ) {
            "Nem sikerült megnyitni a CSV fájlt."
        }

        output.bufferedWriter(
            Charsets.UTF_8
        ).use { writer ->
            writer.appendLine(
                "id;date;type;value;note;createdAtMillis"
            )

            readings
                .sortedWith(
                    compareBy<MeterReading> {
                        it.dateEpochDay
                    }.thenBy {
                        it.type.name
                    }
                )
                .forEach { reading ->
                    writer.appendLine(
                        listOf(
                            reading.id,
                            LocalDate
                                .ofEpochDay(
                                    reading.dateEpochDay
                                )
                                .toString(),
                            reading.type.name,
                            reading.value
                                .toString(),
                            reading.note,
                            reading.createdAtMillis
                                .toString()
                        ).joinToString(";") {
                            escape(it)
                        }
                    )
                }
        }
    }

    fun import(
        uri: Uri
    ): List<MeterReading> {
        val input = requireNotNull(
            contentResolver.openInputStream(uri)
        ) {
            "Nem sikerült megnyitni a CSV fájlt."
        }

        val text =
            input.bufferedReader(
                Charsets.UTF_8
            ).use {
                it.readText()
            }

        val rows =
            parseRows(text)

        if (rows.isEmpty()) {
            return emptyList()
        }

        val header =
            rows.first()
                .map {
                    it.trim()
                }

        val idIndex =
            header.indexOf("id")
        val dateIndex =
            header.indexOf("date")
        val typeIndex =
            header.indexOf("type")
        val valueIndex =
            header.indexOf("value")
        val noteIndex =
            header.indexOf("note")
        val createdIndex =
            header.indexOf(
                "createdAtMillis"
            )

        require(
            dateIndex >= 0 &&
                typeIndex >= 0 &&
                valueIndex >= 0
        ) {
            "A CSV fejléc nem megfelelő."
        }

        return rows
            .drop(1)
            .mapNotNull { row ->
                runCatching {
                    val date =
                        LocalDate.parse(
                            row.getOrElse(
                                dateIndex
                            ) {
                                ""
                            }.trim()
                        )

                    MeterReading(
                        id =
                            if (
                                idIndex >= 0
                            ) {
                                row.getOrElse(
                                    idIndex
                                ) {
                                    ""
                                }
                                    .trim()
                                    .ifBlank {
                                        UUID.randomUUID()
                                            .toString()
                                    }
                            } else {
                                UUID.randomUUID()
                                    .toString()
                            },
                        type =
                            MeterType.valueOf(
                                row.getOrElse(
                                    typeIndex
                                ) {
                                    ""
                                }.trim()
                            ),
                        value =
                            row.getOrElse(
                                valueIndex
                            ) {
                                ""
                            }
                                .replace(
                                    ',',
                                    '.'
                                )
                                .toDouble(),
                        dateEpochDay =
                            date.toEpochDay(),
                        note =
                            if (
                                noteIndex >= 0
                            ) {
                                row.getOrElse(
                                    noteIndex
                                ) {
                                    ""
                                }
                            } else {
                                ""
                            },
                        photoUri = null,
                        createdAtMillis =
                            if (
                                createdIndex >=
                                0
                            ) {
                                row.getOrElse(
                                    createdIndex
                                ) {
                                    ""
                                }
                                    .toLongOrNull()
                                    ?: System
                                        .currentTimeMillis()
                            } else {
                                System
                                    .currentTimeMillis()
                            }
                    )
                }.getOrNull()
            }
    }

    private fun escape(
        value: String
    ): String {
        val escaped =
            value.replace(
                """,
                """"
            )

        return if (
            value.contains(';') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        ) {
            """ +
                escaped +
                """
        } else {
            escaped
        }
    }

    private fun parseRows(
        text: String
    ): List<List<String>> {
        val rows =
            mutableListOf<List<String>>()
        val row =
            mutableListOf<String>()
        val field =
            StringBuilder()

        var quoted = false
        var index = 0

        fun finishField() {
            row += field.toString()
            field.clear()
        }

        fun finishRow() {
            finishField()
            if (
                row.any {
                    it.isNotBlank()
                }
            ) {
                rows += row.toList()
            }
            row.clear()
        }

        while (index < text.length) {
            val char = text[index]

            when {
                char == '"' &&
                    quoted &&
                    index + 1 <
                    text.length &&
                    text[index + 1] ==
                    '"' -> {
                    field.append('"')
                    index++
                }

                char == '"' ->
                    quoted = !quoted

                char == ';' &&
                    !quoted ->
                    finishField()

                (
                    char == '\n' ||
                    char == '\r'
                    ) &&
                    !quoted -> {
                    if (
                        char == '\r' &&
                        index + 1 <
                        text.length &&
                        text[index + 1] ==
                        '\n'
                    ) {
                        index++
                    }
                    finishRow()
                }

                else ->
                    field.append(char)
            }

            index++
        }

        if (
            field.isNotEmpty() ||
            row.isNotEmpty()
        ) {
            finishRow()
        }

        return rows
    }

    companion object {
        const val MIME_TYPE =
            "text/csv"
    }
}
