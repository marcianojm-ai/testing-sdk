package com.suaempresa.testing

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * Cliente HTTP interno do SDK.
 *
 * Envia eventos de sessão ao backend sem bloquear
 * a thread principal do aplicativo.
 */
internal class EventApiClient {

    /**
     * Envia um evento de sessão ao backend.
     *
     * @param payload Dados já preparados pelo SDK.
     */
    fun send(payload: SessionEventPayload) {
        networkExecutor.execute {
            executeRequest(payload)
        }
    }

    private fun executeRequest(
        payload: SessionEventPayload
    ) {
        var connection: HttpURLConnection? = null

        try {
            val url = URL(SdkConfig.EVENTS_ENDPOINT)

            connection =
                url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout =
                SdkConfig.CONNECTION_TIMEOUT_MS
            connection.readTimeout =
                SdkConfig.READ_TIMEOUT_MS

            connection.doOutput = true
            connection.useCaches = false

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val requestBody =
                createRequestBody(payload).toString()

            val requestBytes =
                requestBody.toByteArray(
                    StandardCharsets.UTF_8
                )

            connection.setFixedLengthStreamingMode(
                requestBytes.size
            )

            connection.outputStream.use { outputStream ->
                outputStream.write(requestBytes)
                outputStream.flush()
            }

            val responseCode =
                connection.responseCode

            val responseText =
                readResponse(
                    connection,
                    responseCode
                )

            if (responseCode in 200..299) {
                Log.d(
                    TAG,
                    "Evento ${payload.eventType} enviado " +
                        "com sucesso. HTTP $responseCode. " +
                        responseText
                )
            } else {
                Log.w(
                    TAG,
                    "Backend recusou o evento " +
                        "${payload.eventType}. " +
                        "HTTP $responseCode. " +
                        responseText
                )
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Falha ao enviar o evento " +
                    "${payload.eventType}.",
                exception
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Converte o modelo interno para os nomes de campos
     * esperados pelo backend.
     */
    private fun createRequestBody(
        payload: SessionEventPayload
    ): JSONObject {
        return JSONObject().apply {
            put(
                "package_name",
                payload.packageName
            )

            put(
                "device_id",
                payload.deviceId
            )

            put(
                "tipo_evento",
                payload.eventType
            )

            put(
                "duracao_segundos",
                payload.durationSeconds
            )

            put(
                "sdk_version",
                SdkConfig.SDK_VERSION
            )
put(
    "install_source",
    payload.installSource
)

            if (payload.referrer != null) {
                put(
                    "referrer",
                    payload.referrer
                )
            } else {
                put(
                    "referrer",
                    JSONObject.NULL
                )
            }
        }
    }

    /**
     * Lê a resposta de sucesso ou de erro devolvida pelo backend.
     */
    private fun readResponse(
        connection: HttpURLConnection,
        responseCode: Int
    ): String {
        val inputStream =
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        if (inputStream == null) {
            return ""
        }

        return inputStream
            .bufferedReader(
                StandardCharsets.UTF_8
            )
            .use { reader ->
                reader.readText()
            }
    }

    companion object {
        private const val TAG =
            "TestingSDK"

        /**
         * Uma única fila de envio evita criar uma nova
         * thread para cada evento.
         */
        private val networkExecutor =
            Executors.newSingleThreadExecutor()
    }
}