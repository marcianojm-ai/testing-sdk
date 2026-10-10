package com.suaempresa.testing

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lê apenas o convite versionado do AppPayBack transportado pela Google Play.
 * Referrers comuns não são armazenados nem enviados.
 */
internal class InstallReferrerManager(
    context: Context,
    private val onParticipationToken: (String) -> Unit,
    private val onRetryWindowExhausted: () -> Unit
) {
    private val applicationContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val retryPolicy = InstallReferrerRetryPolicy()

    /**
     * Inicia uma janela limitada de tentativas. Uma falha síncrona inicial é
     * propagada para que TestingSDK possa tentar novamente sem recriar o monitor.
     */
    fun checkAndSendReferrer() {
        startAttempt(
            attempt = 1,
            propagateSynchronousFailure = true
        )
    }

    private fun startAttempt(
        attempt: Int,
        propagateSynchronousFailure: Boolean
    ) {
        val client = InstallReferrerClient
            .newBuilder(applicationContext)
            .build()
        val completed = AtomicBoolean(false)
        lateinit var watchdog: Runnable

        fun finishAttempt(retry: Boolean) {
            if (!completed.compareAndSet(false, true)) return

            main.removeCallbacks(watchdog)
            try {
                client.endConnection()
            } catch (_: Exception) {
                // A conexão pode ainda não ter sido iniciada ou já estar encerrada.
            }

            if (!retry) return
            val delay = retryPolicy.delayAfterFailure(attempt)
            if (delay == null) {
                main.post(onRetryWindowExhausted)
                return
            }
            main.postDelayed(
                {
                    startAttempt(
                        attempt = attempt + 1,
                        propagateSynchronousFailure = false
                    )
                },
                delay
            )
        }

        watchdog = Runnable {
            Log.d(TAG, "Install Referrer demorou para responder; nova tentativa agendada.")
            finishAttempt(retry = true)
        }
        main.postDelayed(watchdog, ATTEMPT_TIMEOUT_MS)

        try {
            client.startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        if (completed.get()) return

                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                try {
                                    processResponse(client)
                                    finishAttempt(retry = false)
                                } catch (_: Exception) {
                                    Log.w(TAG, "Não foi possível processar o Install Referrer.")
                                    finishAttempt(retry = true)
                                }
                            }

                            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                                Log.d(TAG, "Serviço do Install Referrer indisponível no momento.")
                                finishAttempt(retry = true)
                            }

                            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                                Log.d(TAG, "Install Referrer não é suportado neste aparelho.")
                                finishAttempt(retry = false)
                            }

                            else -> {
                                Log.d(TAG, "Install Referrer não disponível. Código: $responseCode")
                                finishAttempt(retry = false)
                            }
                        }
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        if (completed.get()) return
                        Log.d(TAG, "Serviço do Install Referrer desconectado.")
                        finishAttempt(retry = true)
                    }
                }
            )
        } catch (error: Exception) {
            main.removeCallbacks(watchdog)
            try {
                client.endConnection()
            } catch (_: Exception) {
                // A conexão pode não ter sido iniciada.
            }
            completed.set(true)

            if (propagateSynchronousFailure) throw error
            scheduleAfterSynchronousFailure(attempt)
        }
    }

    private fun scheduleAfterSynchronousFailure(attempt: Int) {
        val delay = retryPolicy.delayAfterFailure(attempt)
        if (delay == null) {
            main.post(onRetryWindowExhausted)
            return
        }
        main.postDelayed(
            {
                startAttempt(
                    attempt = attempt + 1,
                    propagateSynchronousFailure = false
                )
            },
            delay
        )
    }

    private fun processResponse(client: InstallReferrerClient) {
        val referrer = client.installReferrer.installReferrer
        if (!ParticipationReferrer.containsPlatformPayload(referrer)) {
            Log.d(TAG, "Nenhum convite do AppPayBack foi encontrado nesta instalação.")
            return
        }

        val token = ParticipationReferrer.extractToken(referrer)
        if (token == null) {
            Log.w(TAG, "Convite do AppPayBack inválido.")
            return
        }

        onParticipationToken(token)
        Log.d(TAG, "Convite do AppPayBack recebido.")
    }

    companion object {
        private const val TAG = "TestingSDK"
        private const val ATTEMPT_TIMEOUT_MS = 10_000L
    }
}
