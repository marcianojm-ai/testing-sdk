package com.suaempresa.testing

import android.content.Context
import android.os.Build
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
     * Consulta o Install Referrer somente quando o aplicativo
     * atual foi instalado ou atualizado pela Google Play.
     */
    fun checkAndSendReferrer(
        installationId: String
    ) {
        if (!foiInstaladoPelaGooglePlay()) {
            referrerStore.clear()

            Log.d(
                TAG,
                "Aplicativo não foi instalado pela Google Play. " +
                    "Referrer local removido para o aparelho " +
                    "[$installationId]."
            )

            return
        }

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

                                referrerStore.clear()

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

    /**
     * Verifica o instalador registrado pelo Android.
     */
    private fun foiInstaladoPelaGooglePlay(): Boolean {
        return try {
            val packageManager =
                applicationContext.packageManager

            val packageName =
                applicationContext.packageName

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {
                val installSourceInfo =
                    packageManager.getInstallSourceInfo(
                        packageName
                    )

                val instalador =
                    installSourceInfo.installingPackageName

                val iniciador =
                    installSourceInfo.initiatingPackageName

                instalador == GOOGLE_PLAY_PACKAGE ||
                    iniciador == GOOGLE_PLAY_PACKAGE
            } else {
                @Suppress("DEPRECATION")
                val instalador =
                    packageManager.getInstallerPackageName(
                        packageName
                    )

                instalador == GOOGLE_PLAY_PACKAGE
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Não foi possível identificar a origem " +
                    "da instalação.",
                exception
            )

            false
        }
    }

    companion object {
        private const val TAG = "TestingSDK"

        private const val GOOGLE_PLAY_PACKAGE =
            "com.android.vending"
    }
}