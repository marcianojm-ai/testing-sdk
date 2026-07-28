package com.suaempresa.testing

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Monitora quando o aplicativo entra em primeiro plano
 * e quando deixa de estar visível.
 *
 * Os eventos são colocados em uma fila persistente
 * gerenciada pelo WorkManager.
 */
internal class SessionManager(
    context: Context,
    private val deviceId: String
) : DefaultLifecycleObserver {

    private val applicationContext =
        context.applicationContext

    private val packageName =
        applicationContext.packageName

    private val referrerStore =
        ReferrerStore(applicationContext)

    private val installSourceDetector =
        InstallSourceDetector(applicationContext)

    private val eventWorkScheduler =
        EventWorkScheduler(applicationContext)

    private var sessionStartTimeMs: Long = 0L

    /**
     * Chamado quando o aplicativo entra
     * em primeiro plano.
     */
    override fun onStart(
        owner: LifecycleOwner
    ) {
        if (sessionStartTimeMs > 0L) {
            return
        }

        sessionStartTimeMs =
            SystemClock.elapsedRealtime()

        val installSource =
            installSourceDetector.detect()

        val referrer =
            if (
                installSource ==
                InstallSourceDetector.GOOGLE_PLAY
            ) {
                referrerStore.get()
            } else {
                null
            }

        val payload =
            SessionEventPayload(
                packageName = packageName,
                deviceId = deviceId,
                eventType =
                    SessionEventPayload.SESSION_START,
                durationSeconds = 0L,
                referrer = referrer,
                installSource = installSource
            )

        eventWorkScheduler.enqueue(payload)

        Log.d(
            TAG,
            "--> session_start: " +
                "O testador [$deviceId] abriu " +
                "o aplicativo [$packageName]. " +
                "Origem: [$installSource]. " +
                "Evento adicionado à fila."
        )
    }

    /**
     * Chamado quando o aplicativo vai
     * para segundo plano.
     */
    override fun onStop(
        owner: LifecycleOwner
    ) {
        val inicioSessao =
            sessionStartTimeMs

        if (inicioSessao <= 0L) {
            return
        }

        /*
         * Zeramos imediatamente para impedir
         * que o mesmo encerramento seja
         * processado duas vezes.
         */
        sessionStartTimeMs = 0L

        val duracaoMillis =
            (
                SystemClock.elapsedRealtime() -
                    inicioSessao
                ).coerceAtLeast(0L)

        val duracaoSegundos =
            duracaoMillis / 1_000L

        val installSource =
            installSourceDetector.detect()

        val referrer =
            if (
                installSource ==
                InstallSourceDetector.GOOGLE_PLAY
            ) {
                referrerStore.get()
            } else {
                null
            }

        val payload =
            SessionEventPayload(
                packageName = packageName,
                deviceId = deviceId,
                eventType =
                    SessionEventPayload.SESSION_END,
                durationSeconds =
                    duracaoSegundos,
                referrer = referrer,
                installSource = installSource
            )

        eventWorkScheduler.enqueue(payload)

        Log.d(
            TAG,
            "<-- session_end: " +
                "O testador [$deviceId] usou " +
                "o aplicativo [$packageName] por " +
                "$duracaoSegundos segundos. " +
                "Origem: [$installSource]. " +
                "Evento adicionado à fila."
        )
    }

    companion object {
        private const val TAG =
            "TestingSDK"
    }
}