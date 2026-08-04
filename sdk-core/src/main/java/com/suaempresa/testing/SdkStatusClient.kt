package com.suaempresa.testing

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Resultado da consulta remota do SDK.
 */
internal enum class SdkRemoteStatus {
    ACTIVE,
    NEWLY_ASSOCIATED,
    INACTIVE,
    UNAVAILABLE
}

/**
 * Consulta o backend antes de iniciar
 * qualquer contabilização de uso.
 */
internal class SdkStatusClient {

    fun checkBlocking(
        packageName: String,
        appFingerprint: String
    ): SdkRemoteStatus {
        var connection: HttpURLConnection? = null

        return try {
            val encodedPackageName =
                URLEncoder.encode(
                    packageName,
                    StandardCharsets.UTF_8.name()
                )

            val encodedFingerprint =
                URLEncoder.encode(
                    appFingerprint,
                    StandardCharsets.UTF_8.name()
                )

            val url =
                URL(
                    "${SdkConfig.SDK_STATUS_ENDPOINT}" +
                            "?package_name=$encodedPackageName" +
                            "&app_fingerprint=$encodedFingerprint"
                )

            connection =
                url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"

            connection.connectTimeout =
                SdkConfig.CONNECTION_TIMEOUT_MS

            connection.readTimeout =
                SdkConfig.READ_TIMEOUT_MS

            connection.useCaches = false

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                Log.w(
                    TAG,
                    "Não foi possível consultar o status " +
                            "do SDK. HTTP $responseCode."
                )

                return SdkRemoteStatus.UNAVAILABLE
            }

            val responseText =
                connection.inputStream
                    .bufferedReader(
                        StandardCharsets.UTF_8
                    )
                    .use { reader ->
                        reader.readText()
                    }

            val responseJson =
                JSONObject(responseText)

            val successful =
                responseJson.optBoolean(
                    "sucesso",
                    false
                )

            if (!successful) {
                return SdkRemoteStatus.UNAVAILABLE
            }

            val active =
                responseJson.optBoolean(
                    "ativo",
                    false
                )

            val newlyAssociated =
                responseJson.optBoolean(
                    "nova_associacao",
                    false
                )

            when {
                active && newlyAssociated -> {
                    Log.d(
                        TAG,
                        "SDK associado automaticamente " +
                                "ao aplicativo. Esta abertura " +
                                "não será contabilizada como teste."
                    )

                    SdkRemoteStatus.NEWLY_ASSOCIATED
                }

                active -> {
                    Log.d(
                        TAG,
                        "Coleta do SDK autorizada pelo backend."
                    )

                    SdkRemoteStatus.ACTIVE
                }

                else -> {
                    Log.d(
                        TAG,
                        "Coleta do SDK desativada ou " +
                                "aplicativo ainda não autorizado."
                    )

                    SdkRemoteStatus.INACTIVE
                }
            }
        } catch (exception: Exception) {
            Log.w(
                TAG,
                "Status remoto do SDK indisponível. " +
                        "Nenhum uso será contabilizado.",
                exception
            )

            SdkRemoteStatus.UNAVAILABLE
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG =
            "TestingSDK"
    }
}