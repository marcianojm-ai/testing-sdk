package com.apppayback.sdk

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class CheckinReply(
    val status: Int,
    val body: JSONObject,
    val retryAfterMs: Long? = null
) {
    val successful get() = status in 200..299 && body.optBoolean("sucesso")
    val errorCode: String get() = body.optJSONObject("erro")?.optString("codigo").orEmpty()
}

internal class CheckinApiClient {
    fun post(path: String, body: JSONObject, credential: String? = null): CheckinReply {
        val connection = URL("${SdkConfig.API_BASE_URL}$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = SdkConfig.CONNECTION_TIMEOUT_MS
            connection.readTimeout = SdkConfig.READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            credential?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val retryAfterMs = connection
                .getHeaderField("Retry-After")
                ?.trim()
                ?.toLongOrNull()
                ?.takeIf { it >= 0L }
                ?.let { seconds ->
                    if (seconds > Long.MAX_VALUE / 1_000L) {
                        Long.MAX_VALUE
                    } else {
                        seconds * 1_000L
                    }
                }

            CheckinReply(
                status = status,
                body = if (text.isBlank()) JSONObject() else JSONObject(text),
                retryAfterMs = retryAfterMs
            )
        } finally { connection.disconnect() }
    }
}
