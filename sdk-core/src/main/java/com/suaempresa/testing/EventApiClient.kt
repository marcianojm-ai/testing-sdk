package com.suaempresa.testing

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Resultado de uma tentativa de envio ao backend.
 */
internal enum class EventSendResult {
    SUCCESS,
    RETRY,
    FAILURE
}

/**
 * Cliente HTTP interno do SDK.
 *
 * O envio é executado pelo EventUploadWorker
 * em uma thread de segundo plano.
 */
internal class EventApiClient {

    /**
     * Executa o envio do evento ao backend.
     *
     * Esse método é chamado pelo WorkManager.
     */
    fun sendBlocking(
        payload: SessionEventPayload
    ): EventSendResult {
        var connection: HttpURLConnection? = null

        return try {
            val url =
                URL(SdkConfig.EVENTS_ENDPOINT)

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

            connection.outputStream.use {
                    outputStream ->

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

            when {
                responseCode in 200..299 -> {
                    Log.d(
                        TAG,
                        "Evento ${payload.eventType} enviado " +
                                "com sucesso. HTTP $responseCode. " +
                                responseText
                    )

                    EventSendResult.SUCCESS
                }

                deveTentarNovamente(
                    responseCode
                ) -> {
                    Log.w(
                        TAG,
                        "Falha temporária ao enviar " +
                                "${payload.eventType}. " +
                                "HTTP $responseCode. " +
                                "Uma nova tentativa será realizada. " +
                                responseText
                    )

                    EventSendResult.RETRY
                }

                else -> {
                    Log.w(
                        TAG,
                        "Backend recusou definitivamente " +
                                "o evento ${payload.eventType}. " +
                                "HTTP $responseCode. " +
                                responseText
                    )

                    EventSendResult.FAILURE
                }
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Falha de conexão ao enviar o evento " +
                        "${payload.eventType}. " +
                        "Uma nova tentativa será realizada.",
                exception
            )

            EventSendResult.RETRY
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Define quais respostas HTTP são temporárias.
     */
    private fun deveTentarNovamente(
        responseCode: Int
    ): Boolean {
        return (
                responseCode == 408 ||
                        responseCode == 425 ||
                        responseCode == 429 ||
                        responseCode in 500..599
                )
    }

    /**
     * Converte o evento para o JSON esperado
     * pelo backend.
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
     * Lê a resposta retornada pelo backend.
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
    }
}