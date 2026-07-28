package com.suaempresa.testing

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/**
 * Monitora quando o aplicativo entra em primeiro plano
 * e quando deixa de estar visível.
 *
 * Cada evento é armazenado no WorkManager antes da
 * tentativa imediata de envio. Assim, nenhuma informação
 * é perdida se a conexão falhar ou o processo for encerrado.
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

    /*
     * Um único executor mantém a ordem:
     * session_start sempre é tentado antes de session_end.
     */
    private val immediateExecutor =
        Executors.newSingleThreadExecutor()

    private var sessionStartTimeMs: Long = 0L

    private var currentSession:
            ImmediateSession? = null

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

        val startPayload =
            SessionEventPayload(
                packageName = packageName,
                deviceId = deviceId,
                eventType =
                    SessionEventPayload.SESSION_START,
                durationSeconds = 0L,
                referrer = referrer,
                installSource = installSource
            )

        val session =
            ImmediateSession(
                startPayload = startPayload
            )

        currentSession = session

        /*
         * Primeiro persistimos. Se o processo morrer,
         * o WorkManager ainda possuirá o evento.
         */
        eventWorkScheduler.enqueue(
            startPayload
        )

        /*
         * Depois tentamos enviar imediatamente.
         */
        immediateExecutor.execute {
            ensureStartSent(session)
        }

        Log.d(
            TAG,
            "--> session_start: " +
                    "O testador [$deviceId] abriu " +
                    "o aplicativo [$packageName]. " +
                    "Origem: [$installSource]. " +
                    "Evento salvo na fila e enviado " +
                    "imediatamente quando possível."
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

        val session =
            currentSession

        if (
            inicioSessao <= 0L ||
            session == null
        ) {
            return
        }

        sessionStartTimeMs = 0L
        currentSession = null

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

        val endPayload =
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

        /*
         * O encerramento também é persistido antes
         * de qualquer tentativa de conexão.
         */
        eventWorkScheduler.enqueue(
            endPayload
        )

        immediateExecutor.execute {
            /*
             * Antes de enviar o encerramento,
             * garantimos que a abertura foi confirmada.
             * Isso impede eventos fora de ordem.
             */
            val startConfirmed =
                ensureStartSent(session)

            if (!startConfirmed) {
                Log.w(
                    TAG,
                    "session_end permaneceu na fila " +
                            "porque session_start ainda " +
                            "não foi confirmado."
                )

                return@execute
            }

            when (
                EventApiClient()
                    .sendBlocking(endPayload)
            ) {
                EventSendResult.SUCCESS -> {
                    Log.d(
                        TAG,
                        "session_end enviado " +
                                "imediatamente. " +
                                "Event ID: " +
                                endPayload.eventId
                    )
                }

                EventSendResult.RETRY -> {
                    Log.w(
                        TAG,
                        "session_end não pôde ser " +
                                "enviado imediatamente e " +
                                "permanece na fila. " +
                                "Event ID: " +
                                endPayload.eventId
                    )
                }

                EventSendResult.FAILURE -> {
                    Log.e(
                        TAG,
                        "session_end foi recusado " +
                                "pelo backend. " +
                                "Event ID: " +
                                endPayload.eventId
                    )
                }
            }
        }

        Log.d(
            TAG,
            "<-- session_end: " +
                    "O testador [$deviceId] usou " +
                    "o aplicativo [$packageName] por " +
                    "$duracaoSegundos segundos. " +
                    "Origem: [$installSource]. " +
                    "Evento salvo na fila e enviado " +
                    "imediatamente quando possível."
        )
    }

    /**
     * Garante que o evento de abertura da sessão
     * seja confirmado antes do encerramento.
     */
    private fun ensureStartSent(
        session: ImmediateSession
    ): Boolean {
        if (session.startConfirmed) {
            return true
        }

        return when (
            EventApiClient()
                .sendBlocking(
                    session.startPayload
                )
        ) {
            EventSendResult.SUCCESS -> {
                session.startConfirmed = true

                Log.d(
                    TAG,
                    "session_start enviado " +
                            "imediatamente. " +
                            "Event ID: " +
                            session.startPayload.eventId
                )

                true
            }

            EventSendResult.RETRY -> {
                Log.w(
                    TAG,
                    "session_start não pôde ser " +
                            "enviado imediatamente e " +
                            "permanece na fila. " +
                            "Event ID: " +
                            session.startPayload.eventId
                )

                false
            }

            EventSendResult.FAILURE -> {
                Log.e(
                    TAG,
                    "session_start foi recusado " +
                            "pelo backend. " +
                            "Event ID: " +
                            session.startPayload.eventId
                )

                false
            }
        }
    }

    /**
     * Estado do envio imediato de uma sessão.
     */
    private class ImmediateSession(
        val startPayload:
        SessionEventPayload,
        var startConfirmed:
        Boolean = false
    )

    companion object {
        private const val TAG =
            "TestingSDK"
    }
}