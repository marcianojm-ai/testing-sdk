package com.suaempresa.testing

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

object TestingSDK {
    private var manager: DailyCheckinManager? = null
    private var installReferrerManager: InstallReferrerManager? = null
    private var referrerStartAttempt = 1
    private var exhaustedReferrerWindows = 0
    private var referrerWindowRetryScheduled = false
    private val referrerRetryPolicy = InstallReferrerRetryPolicy()
    private val main = Handler(Looper.getMainLooper())

    /** Continua opcional: o provedor do SDK chama automaticamente. */
    fun start(context: Context) {
        val app = context.applicationContext as Application
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { start(app) }
            return
        }
        val checkinManager = manager ?: try {
            DailyCheckinManager(app).also {
                it.attach()
                manager = it
            }
        } catch (_: Exception) {
            Log.w("TestingSDK", "Não foi possível completar a inicialização local do SDK.")
            return
        }

        if (installReferrerManager != null) return

        lateinit var candidate: InstallReferrerManager
        candidate = InstallReferrerManager(
            context = app,
            onParticipationToken = { token ->
                exhaustedReferrerWindows = 0
                checkinManager.acceptParticipationToken(token)
            },
            onRetryWindowExhausted = {
                if (installReferrerManager === candidate) {
                    installReferrerManager = null
                }
                scheduleReferrerWindowRetry(app)
            }
        )

        try {
            candidate.checkAndSendReferrer()
            installReferrerManager = candidate
            referrerStartAttempt = 1
        } catch (_: Exception) {
            // Mantém o monitor diário ativo e repete somente a conexão
            // do referrer, sem recriar callbacks de ciclo de vida.
            installReferrerManager = null
            val failedAttempt = referrerStartAttempt
            val delay =
                referrerRetryPolicy.delayAfterFailure(
                    failedAttempt
                )
            if (delay != null) {
                referrerStartAttempt = failedAttempt + 1
                main.postDelayed(
                    { start(app) },
                    delay
                )
            }
            Log.w(
                "TestingSDK",
                "Install Referrer indisponível; uma nova inicialização poderá tentar novamente."
            )
        }
    }

    private fun scheduleReferrerWindowRetry(app: Application) {
        if (
            referrerWindowRetryScheduled ||
            exhaustedReferrerWindows >= MAX_REFERRER_WINDOWS
        ) {
            return
        }

        exhaustedReferrerWindows++
        referrerWindowRetryScheduled = true
        val delay = REFERRER_WINDOW_RETRY_MS *
            exhaustedReferrerWindows
        main.postDelayed(
            {
                referrerWindowRetryScheduled = false
                start(app)
            },
            delay
        )
    }

    /** Convite temporário do backend. Callback na main thread: null = sucesso. */
    fun redeemParticipation(context: Context, temporaryToken: String, callback: (String?) -> Unit) {
        main.post {
            start(context)
            manager?.redeem(temporaryToken, callback) ?: callback("SDK_INDISPONIVEL")
        }
    }

    private const val MAX_REFERRER_WINDOWS = 3
    private const val REFERRER_WINDOW_RETRY_MS = 5 * 60_000L
}
