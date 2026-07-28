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
                        installSource
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
                        "confirmado pelo backend."
                )

                Result.success()
            }

            EventSendResult.RETRY -> {
                Log.w(
                    TAG,
                    "Evento ${payload.eventType} " +
                        "será enviado novamente."
                )

                Result.retry()
            }

            EventSendResult.FAILURE -> {
                Log.e(
                    TAG,
                    "Evento ${payload.eventType} " +
                        "foi recusado definitivamente."
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