package hu.merenyimiklos.meterreader.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val version: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    data class Available(
        val info: AppUpdateInfo
    ) : UpdateState

    data class Error(
        val message: String
    ) : UpdateState
}

class AppUpdateChecker {
    fun check(
        currentVersion: String
    ): Result<AppUpdateInfo?> =
        runCatching {
            val connection =
                URL(LATEST_RELEASE_API)
                    .openConnection()
                    as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            )
            connection.setRequestProperty(
                "User-Agent",
                "MeterReader-Android"
            )

            try {
                val responseCode =
                    connection.responseCode

                if (responseCode == 404) {
                    return@runCatching null
                }

                if (
                    responseCode !in
                    200..299
                ) {
                    error(
                        "GitHub válaszkód: " +
                            responseCode
                    )
                }

                val json =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            JSONObject(
                                it.readText()
                            )
                        }

                val tag =
                    json.getString(
                        "tag_name"
                    )
                        .removePrefix("v")

                if (
                    !VersionComparator
                        .isNewer(
                            currentVersion,
                            tag
                        )
                ) {
                    return@runCatching null
                }

                val assets =
                    json.optJSONArray(
                        "assets"
                    )

                var apkUrl: String? = null

                if (assets != null) {
                    for (
                        index in
                        0 until assets.length()
                    ) {
                        val asset =
                            assets.getJSONObject(
                                index
                            )
                        val name =
                            asset.optString(
                                "name"
                            )

                        if (
                            name.endsWith(
                                ".apk",
                                ignoreCase = true
                            )
                        ) {
                            apkUrl =
                                asset.optString(
                                    "browser_download_url"
                                )
                            break
                        }
                    }
                }

                val releasePage =
                    json.getString(
                        "html_url"
                    )

                AppUpdateInfo(
                    version = tag,
                    title =
                        json.optString(
                            "name"
                        )
                            .ifBlank {
                                "Mérőóra Napló " +
                                    tag
                            },
                    notes =
                        json.optString(
                            "body"
                        ),
                    downloadUrl =
                        apkUrl
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: releasePage,
                    releasePageUrl =
                        releasePage
                )
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/merenyimiklos/mvvm-gas-electricity-meter-reader/releases/latest"
    }
}
