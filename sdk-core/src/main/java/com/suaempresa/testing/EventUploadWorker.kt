package com.suaempresa.testing

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Envia um evento armazenado pelo WorkManager.
 *
 * Em caso de falha temporária de conexão ou do servidor,
 * o WorkManager mantém o evento e tenta novamente.
 */
internal class EventUploadWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : Worker(
    appContext,
    workerParameters
) {

    override fun doWork(): Result {
        val packageName =
            inputData.getString(
                KEY_PACKAGE_NAME
            )

        val deviceId =
            inputData.getString(
                KEY_DEVICE_ID
            )

        /*
         * Eventos criados por versões anteriores do SDK
         * podem não possuir eventId. Nesse caso, o próprio
         * ID persistente do WorkManager é utilizado.
         */
        val eventId =
            inputData.getString(
                KEY_EVENT_ID
            ) ?: id.toString()

        val occurredAtEpochMs =
            inputData.getLong(
                KEY_OCCURRED_AT_EPOCH_MS,
                0L
            ).takeIf {
                it > 0L
            } ?: System.currentTimeMillis()

        val eventType =
            inputData.getString(
                KEY_EVENT_TYPE
            )

        val durationSeconds =
            inputData.getLong(
                KEY_DURATION_SECONDS,
                0L
            )

        val referrer =
            inputData.getString(
                KEY_REFERRER
            )

        val installSource =
            inputData.getString(
                KEY_INSTALL_SOURCE
            ) ?: InstallSourceDetector.UNKNOWN

        if (
            packageName.isNullOrBlank() ||
            deviceId.isNullOrBlank() ||
            eventType.isNullOrBlank()
        ) {
            Log.e(
                TAG,
                "Evento descartado porque os dados " +
                        "obrigatórios estão ausentes."
            )

            return Result.failure()
        }

        val payload =
            try {
                SessionEventPayload(
                    packageName = packageName,
                    deviceId = deviceId,
                    eventType = eventType,
                    durationSeconds =
                        durationSeconds,
                    referrer = referrer,
                    installSource =
                        installSource,
                    eventId = eventId,
                    occurredAtEpochMs = occurredAtEpochMs
                )
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Evento descartado porque os dados " +
                            "armazenados são inválidos.",
                    exception
                )

                return Result.failure()
            }

        return when (
            EventApiClient()
                .sendBlocking(payload)
        ) {
            EventSendResult.SUCCESS -> {
                Log.d(
                    TAG,
                    "Evento ${payload.eventType} " +
                            "confirmado pelo backend. " +
                            "Event ID: ${payload.eventId}"
                )

                Result.success()
            }

            EventSendResult.RETRY -> {
                Log.w(
                    TAG,
                    "Evento ${payload.eventType} " +
                            "será enviado novamente. " +
                            "Event ID: ${payload.eventId}"
                )

                Result.retry()
            }

            EventSendResult.FAILURE -> {
                Log.e(
                    TAG,
                    "Evento ${payload.eventType} " +
                            "foi recusado definitivamente. " +
                            "Event ID: ${payload.eventId}"
                )

                Result.failure()
            }
        }
    }

    companion object {
        internal const val KEY_PACKAGE_NAME =
            "package_name"

        internal const val KEY_DEVICE_ID =
            "device_id"

        internal const val KEY_OCCURRED_AT_EPOCH_MS =
            "occurred_at_epoch_ms"

        internal const val KEY_EVENT_ID =
            "event_id"

        internal const val KEY_EVENT_TYPE =
            "event_type"

        internal const val KEY_DURATION_SECONDS =
            "duration_seconds"

        internal const val KEY_REFERRER =
            "referrer"

        internal const val KEY_INSTALL_SOURCE =
            "install_source"

        private const val TAG =
            "TestingSDK"
    }
}