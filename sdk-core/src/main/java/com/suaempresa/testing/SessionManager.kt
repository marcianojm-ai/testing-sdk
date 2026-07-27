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
 * Toda a coleta e o envio permanecem internos ao SDK.
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

    private val eventApiClient =
        EventApiClient()

    private var sessionStartTimeMs: Long = 0L

    /**
     * Chamado quando o aplicativo entra em primeiro plano.
     */
    override fun onStart(owner: LifecycleOwner) {
        if (sessionStartTimeMs > 0L) {
            return
        }

        sessionStartTimeMs =
            SystemClock.elapsedRealtime()

        val payload = SessionEventPayload(
            packageName = packageName,
            deviceId = deviceId,
            eventType =
                SessionEventPayload.SESSION_START,
            durationSeconds = 0L,
            referrer = referrerStore.get()
        )

        eventApiClient.send(payload)

        Log.d(
            TAG,
            "--> session_start: " +
                "O testador [$deviceId] abriu " +
                "o aplicativo [$packageName]."
        )
    }

    /**
     * Chamado quando o aplicativo vai para segundo plano.
     */
    override fun onStop(owner: LifecycleOwner) {
        val inicioSessao = sessionStartTimeMs

        if (inicioSessao <= 0L) {
            return
        }

        /*
         * Zeramos imediatamente para impedir que o mesmo
         * encerramento seja processado duas vezes.
         */
        sessionStartTimeMs = 0L

        val duracaoMillis =
            (
                SystemClock.elapsedRealtime() -
                    inicioSessao
                ).coerceAtLeast(0L)

        val duracaoSegundos =
            duracaoMillis / 1_000L

        val payload = SessionEventPayload(
            packageName = packageName,
            deviceId = deviceId,
            eventType =
                SessionEventPayload.SESSION_END,
            durationSeconds = duracaoSegundos,
            referrer = referrerStore.get()
        )

        eventApiClient.send(payload)

        Log.d(
            TAG,
            "<-- session_end: " +
                "O testador [$deviceId] usou " +
                "o aplicativo [$packageName] por " +
                "$duracaoSegundos segundos."
        )
    }

    companion object {
        private const val TAG = "TestingSDK"
    }
}