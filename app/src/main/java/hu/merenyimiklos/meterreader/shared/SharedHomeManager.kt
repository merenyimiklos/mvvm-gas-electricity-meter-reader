package hu.merenyimiklos.meterreader.shared

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class SharedHomeInfo(
    val homeId: String,
    val name: String,
    val createdAt: String
)

enum class SharedHomeAction {
    UPSERT,
    DELETE
}

data class SharedHomeEvent(
    val eventId: String,
    val homeId: String,
    val action: SharedHomeAction,
    val readingId: String,
    val timestamp: Long,
    val deviceId: String,
    val deviceName: String,
    val reading: MeterReading?
)

data class SharedHomeResolvedChange(
    val readingId: String,
    val action: SharedHomeAction,
    val reading: MeterReading?,
    val timestamp: Long,
    val deviceName: String
)

class SharedHomeManager(
    private val context: Context
) {
    fun createHome(
        folderUri: Uri,
        homeName: String
    ): SharedHomeInfo {
        val root = requireRoot(folderUri)

        require(
            root.findFile(MANIFEST_FILE) == null
        ) {
            "Ebben a mappában már van Közös otthon."
        }

        val info =
            SharedHomeInfo(
                homeId = UUID.randomUUID().toString(),
                name = homeName.trim().ifBlank {
                    "Otthon"
                },
                createdAt = Instant.now().toString()
            )

        val manifest =
            root.createFile(
                MIME_JSON,
                MANIFEST_FILE
            )
                ?: error(
                    "Nem sikerült létrehozni a közös otthon fájlját."
                )

        writeText(
            manifest,
            JSONObject()
                .put("schema", SCHEMA_VERSION)
                .put("homeId", info.homeId)
                .put("name", info.name)
                .put("createdAt", info.createdAt)
                .toString(2)
        )

        eventsFolder(root)

        return info
    }

    fun readHome(
        folderUri: Uri
    ): SharedHomeInfo {
        val root = requireRoot(folderUri)
        val manifest =
            root.findFile(MANIFEST_FILE)
                ?: error(
                    "A kiválasztott mappában nincs MeterReader Közös otthon."
                )

        val json =
            JSONObject(
                readText(manifest)
            )

        require(
            json.optInt("schema", 0) ==
                SCHEMA_VERSION
        ) {
            "Nem támogatott Közös otthon formátum."
        }

        val homeId =
            json.optString("homeId")
                .trim()
        require(homeId.isNotBlank()) {
            "A közös otthon azonosítója hiányzik."
        }

        return SharedHomeInfo(
            homeId = homeId,
            name =
                json.optString(
                    "name",
                    "Otthon"
                ),
            createdAt =
                json.optString(
                    "createdAt",
                    ""
                )
        )
    }

    fun publishUpsert(
        folderUri: Uri,
        homeId: String,
        deviceId: String,
        deviceName: String,
        reading: MeterReading
    ) {
        publish(
            folderUri = folderUri,
            event =
                SharedHomeEvent(
                    eventId =
                        UUID.randomUUID()
                            .toString(),
                    homeId = homeId,
                    action =
                        SharedHomeAction.UPSERT,
                    readingId = reading.id,
                    timestamp =
                        System.currentTimeMillis(),
                    deviceId = deviceId,
                    deviceName =
                        deviceName.ifBlank {
                            "Telefon"
                        },
                    reading =
                        reading.copy(
                            photoUri = null
                        )
                )
        )
    }

    fun publishDelete(
        folderUri: Uri,
        homeId: String,
        deviceId: String,
        deviceName: String,
        readingId: String
    ) {
        publish(
            folderUri = folderUri,
            event =
                SharedHomeEvent(
                    eventId =
                        UUID.randomUUID()
                            .toString(),
                    homeId = homeId,
                    action =
                        SharedHomeAction.DELETE,
                    readingId = readingId,
                    timestamp =
                        System.currentTimeMillis(),
                    deviceId = deviceId,
                    deviceName =
                        deviceName.ifBlank {
                            "Telefon"
                        },
                    reading = null
                )
        )
    }

    fun resolvedChanges(
        folderUri: Uri,
        expectedHomeId: String
    ): List<SharedHomeResolvedChange> {
        val root = requireRoot(folderUri)
        val folder =
            root.findFile(EVENTS_DIR)
                ?: return emptyList()

        val events =
            folder.listFiles()
                .asSequence()
                .filter {
                    it.isFile &&
                        it.name
                            ?.endsWith(".json")
                            == true
                }
                .mapNotNull { file ->
                    runCatching {
                        parseEvent(
                            readText(file)
                        )
                    }.getOrNull()
                }
                .filter {
                    it.homeId ==
                        expectedHomeId
                }
                .toList()

        return resolveEvents(events)
    }

    internal fun resolveEvents(
        events: List<SharedHomeEvent>
    ): List<SharedHomeResolvedChange> =
        events
            .groupBy {
                it.readingId
            }
            .mapNotNull {
                (_, readingEvents) ->
                readingEvents
                    .maxWithOrNull(
                        compareBy<SharedHomeEvent> {
                            it.timestamp
                        }.thenBy {
                            it.eventId
                        }
                    )
            }
            .map {
                SharedHomeResolvedChange(
                    readingId = it.readingId,
                    action = it.action,
                    reading = it.reading,
                    timestamp = it.timestamp,
                    deviceName = it.deviceName
                )
            }

    private fun publish(
        folderUri: Uri,
        event: SharedHomeEvent
    ) {
        val root = requireRoot(folderUri)
        val manifest =
            readHome(folderUri)

        require(
            manifest.homeId ==
                event.homeId
        ) {
            "A kiválasztott mappa másik Közös otthonhoz tartozik."
        }

        val folder =
            eventsFolder(root)
        val file =
            folder.createFile(
                MIME_JSON,
                "event-" +
                    event.timestamp +
                    "-" +
                    event.eventId +
                    ".json"
            )
                ?: error(
                    "Nem sikerült létrehozni a szinkronfájlt."
                )

        writeText(
            file,
            eventToJson(event)
                .toString()
        )
    }

    private fun eventToJson(
        event: SharedHomeEvent
    ): JSONObject {
        val json =
            JSONObject()
                .put("schema", SCHEMA_VERSION)
                .put("eventId", event.eventId)
                .put("homeId", event.homeId)
                .put("action", event.action.name)
                .put("readingId", event.readingId)
                .put("timestamp", event.timestamp)
                .put("deviceId", event.deviceId)
                .put("deviceName", event.deviceName)

        event.reading?.let {
            reading ->
            json.put(
                "reading",
                JSONObject()
                    .put("id", reading.id)
                    .put("type", reading.type.name)
                    .put("value", reading.value)
                    .put(
                        "dateEpochDay",
                        reading.dateEpochDay
                    )
                    .put("note", reading.note)
                    .put(
                        "createdAtMillis",
                        reading.createdAtMillis
                    )
            )
        }

        return json
    }

    private fun parseEvent(
        raw: String
    ): SharedHomeEvent {
        val json =
            JSONObject(raw)

        require(
            json.optInt("schema", 0) ==
                SCHEMA_VERSION
        )

        val action =
            SharedHomeAction.valueOf(
                json.getString(
                    "action"
                )
            )

        val reading =
            json.optJSONObject(
                "reading"
            )
                ?.let {
                    item ->
                    MeterReading(
                        id =
                            item.getString(
                                "id"
                            ),
                        type =
                            MeterType.valueOf(
                                item.getString(
                                    "type"
                                )
                            ),
                        value =
                            item.getDouble(
                                "value"
                            ),
                        dateEpochDay =
                            item.getLong(
                                "dateEpochDay"
                            ),
                        note =
                            item.optString(
                                "note",
                                ""
                            ),
                        photoUri = null,
                        createdAtMillis =
                            item.optLong(
                                "createdAtMillis",
                                System
                                    .currentTimeMillis()
                            )
                    )
                }

        return SharedHomeEvent(
            eventId =
                json.getString(
                    "eventId"
                ),
            homeId =
                json.getString(
                    "homeId"
                ),
            action = action,
            readingId =
                json.getString(
                    "readingId"
                ),
            timestamp =
                json.getLong(
                    "timestamp"
                ),
            deviceId =
                json.optString(
                    "deviceId",
                    ""
                ),
            deviceName =
                json.optString(
                    "deviceName",
                    ""
                ),
            reading = reading
        )
    }

    private fun requireRoot(
        folderUri: Uri
    ): DocumentFile =
        DocumentFile.fromTreeUri(
            context,
            folderUri
        )
            ?: error(
                "A közös mappa nem érhető el."
            )

    private fun eventsFolder(
        root: DocumentFile
    ): DocumentFile =
        root.findFile(EVENTS_DIR)
            ?: root.createDirectory(
                EVENTS_DIR
            )
            ?: error(
                "Nem sikerült létrehozni a szinkronmappát."
            )

    private fun readText(
        file: DocumentFile
    ): String =
        requireNotNull(
            context.contentResolver
                .openInputStream(
                    file.uri
                )
        ) {
            "Nem sikerült megnyitni a közös fájlt."
        }
            .bufferedReader(
                Charsets.UTF_8
            )
            .use {
                it.readText()
            }

    private fun writeText(
        file: DocumentFile,
        text: String
    ) {
        requireNotNull(
            context.contentResolver
                .openOutputStream(
                    file.uri,
                    "wt"
                )
        ) {
            "Nem sikerült megnyitni írásra a közös fájlt."
        }
            .bufferedWriter(
                Charsets.UTF_8
            )
            .use {
                it.write(text)
            }
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MANIFEST_FILE =
            "meterreader-home.json"
        const val EVENTS_DIR =
            "meterreader-events"
        const val MIME_JSON =
            "application/json"
    }
}
