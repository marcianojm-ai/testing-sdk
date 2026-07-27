package com.suaempresa.testing

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

/**
 * Consulta o Google Play Install Referrer e armazena
 * o resultado localmente para uso nos eventos de sessão.
 */
internal class InstallReferrerManager(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    private val referrerStore =
        ReferrerStore(applicationContext)

    /**
     * Consulta o Install Referrer da Play Store.
     *
     * O identificador é usado apenas nos registros do Logcat.
     */
    fun checkAndSendReferrer(
        installationId: String
    ) {
        val referrerClient =
            InstallReferrerClient
                .newBuilder(applicationContext)
                .build()

        referrerClient.startConnection(
            object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(
                    responseCode: Int
                ) {
                    try {
                        when (responseCode) {
                            InstallReferrerClient
                                .InstallReferrerResponse.OK -> {

                                val response =
                                    referrerClient.installReferrer

                                val referrerUrl =
                                    response.installReferrer

                                referrerStore.save(referrerUrl)

                                Log.d(
                                    TAG,
                                    "Install Referrer salvo para " +
                                        "o aparelho [$installationId]: " +
                                        referrerUrl
                                )
                            }

                            InstallReferrerClient
                                .InstallReferrerResponse
                                .FEATURE_NOT_SUPPORTED -> {

                                Log.d(
                                    TAG,
                                    "Install Referrer não é " +
                                        "suportado neste aparelho."
                                )
                            }

                            InstallReferrerClient
                                .InstallReferrerResponse
                                .SERVICE_UNAVAILABLE -> {

                                Log.d(
                                    TAG,
                                    "Serviço do Install Referrer " +
                                        "indisponível no momento."
                                )
                            }

                            else -> {
                                Log.d(
                                    TAG,
                                    "Install Referrer não disponível. " +
                                        "Código: $responseCode"
                                )
                            }
                        }
                    } catch (exception: Exception) {
                        Log.e(
                            TAG,
                            "Erro ao consultar o Install Referrer.",
                            exception
                        )
                    } finally {
                        try {
                            referrerClient.endConnection()
                        } catch (_: Exception) {
                            // A conexão já pode estar encerrada.
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.d(
                        TAG,
                        "Serviço do Install Referrer desconectado."
                    )
                }
            }
        )
    }

    companion object {
        private const val TAG = "TestingSDK"
    }
}