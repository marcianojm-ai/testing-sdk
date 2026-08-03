package com.suaempresa.testing

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Coloca eventos do SDK em uma fila persistente.
 *
 * Os eventos de cada dispositivo são mantidos em ordem:
 * session_start será processado antes de session_end.
 */
internal class EventWorkScheduler(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    /**
     * Agenda o envio de um evento ao backend.
     *
     * @param payload Evento criado pelo SDK.
     */
    fun enqueue(
        payload: SessionEventPayload
    ) {
        val inputData =
            createInputData(payload)

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val workRequest =
            OneTimeWorkRequestBuilder<EventUploadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    MINIMUM_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .addTag(WORK_TAG)
                .addTag(
                    "${WORK_TAG}_${payload.deviceId}"
                )
                .addTag(
                    "${WORK_TAG}_${payload.eventId}"
                )
                .build()

        /*
         * Cada aplicativo e dispositivo possuem uma fila própria.
         * APPEND_OR_REPLACE preserva a ordem dos eventos e evita
         * que uma falha definitiva bloqueie eventos futuros.
         */
        WorkManager
            .getInstance(applicationContext)
            .enqueueUniqueWork(
                createUniqueWorkName(payload),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )

        Log.d(
            TAG,
            "Evento ${payload.eventType} colocado " +
                    "na fila persistente. " +
                    "Event ID: ${payload.eventId}. " +
                    "Work ID: ${workRequest.id}"
        )
    }

    /**
     * Cria uma fila exclusiva para cada aplicativo
     * e dispositivo monitorado.
     */
    private fun createUniqueWorkName(
        payload: SessionEventPayload
    ): String {
        return (
                "${WORK_NAME_PREFIX}_" +
                        "${payload.packageName}_" +
                        payload.deviceId
                )
    }

    /**
     * Converte o evento em dados aceitos pelo WorkManager.
     */
    private fun createInputData(
        payload: SessionEventPayload
    ): Data {
        val builder =
            Data.Builder()
                .putString(
                    EventUploadWorker.KEY_PACKAGE_NAME,
                    payload.packageName
                )
                .putString(
                    EventUploadWorker.KEY_DEVICE_ID,
                    payload.deviceId
                )
                .putString(
                    EventUploadWorker.KEY_EVENT_ID,
                    payload.eventId
                )
                .putString(
                    EventUploadWorker.KEY_EVENT_TYPE,
                    payload.eventType
                )
                .putLong(
                    "occurred_at_epoch_ms",
                    payload.occurredAtEpochMs,
                )
                .putLong(
                    EventUploadWorker.KEY_DURATION_SECONDS,
                    payload.durationSeconds
                )
                .putString(
                    EventUploadWorker.KEY_INSTALL_SOURCE,
                    payload.installSource
                )

        if (payload.referrer != null) {
            builder.putString(
                EventUploadWorker.KEY_REFERRER,
                payload.referrer
            )
        }

        return builder.build()
    }

    companion object {
        private const val WORK_TAG =
            "testing_sdk_event_upload"

        private const val WORK_NAME_PREFIX =
            "testing_sdk_event_queue"

        private const val MINIMUM_BACKOFF_SECONDS =
            10L

        private const val TAG =
            "TestingSDK"
    }
}