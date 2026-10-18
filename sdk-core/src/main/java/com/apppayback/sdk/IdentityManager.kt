package com.apppayback.sdk

import android.content.Context
import android.util.Log
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Gerencia o identificador da instalação atual.
 * Ele muda quando o aplicativo é desinstalado ou seus dados são apagados.
 */
internal class IdentityManager(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    private val packageName =
        applicationContext.packageName

    private val installationIdFile =
        File(
            applicationContext.noBackupFilesDir,
            INSTALLATION_ID_FILE
        )

    private val legacyPreferences =
        applicationContext.getSharedPreferences(
            LEGACY_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Recupera ou cria o identificador exclusivo
     * da instalação atual do aplicativo.
     */
    fun getOrCreateInstallationId(): String {
        synchronized(INSTALLATION_LOCK) {
            readValidInstallationId(
                installationIdFile
            )?.let { existingId ->
                return existingId
            }

            /*
             * Preserva o ID usado pelas versões anteriores
             * somente quando detectamos uma atualização do app.
             *
             * Em uma instalação nova, um ID restaurado por
             * backup não será reaproveitado.
             */
            val migratedId =
                if (isLikelyApplicationUpdate()) {
                    readLegacyInstallationId()
                } else {
                    null
                }

            val installationId =
                migratedId
                    ?: UUID.randomUUID().toString()

            installationIdFile.parentFile?.mkdirs()

            installationIdFile.writeText(
                installationId,
                StandardCharsets.UTF_8
            )

            legacyPreferences
                .edit()
                .remove(LEGACY_INSTALLATION_ID_KEY)
                .apply()

            return installationId
        }
    }

    /**
     * Lê e valida um UUID armazenado no arquivo.
     */
    private fun readValidInstallationId(
        file: File
    ): String? {
        if (!file.exists()) {
            return null
        }

        return try {
            val storedId =
                file.readText(
                    StandardCharsets.UTF_8
                ).trim()

            UUID.fromString(storedId)

            storedId
        } catch (exception: Exception) {
            Log.w(
                TAG,
                "O installation_id armazenado é inválido " +
                        "e será recriado.",
                exception
            )

            null
        }
    }

    /**
     * Recupera o identificador utilizado pelo SDK antigo.
     */
    private fun readLegacyInstallationId(): String? {
        val legacyId =
            legacyPreferences.getString(
                LEGACY_INSTALLATION_ID_KEY,
                null
            )?.trim()

        if (legacyId.isNullOrBlank()) {
            return null
        }

        return try {
            UUID.fromString(legacyId)
            legacyId
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Diferencia uma atualização normal de uma
     * instalação nova ou reinstalação.
     */
    private fun isLikelyApplicationUpdate(): Boolean {
        return try {
            val packageInfo =
                applicationContext.packageManager
                    .getPackageInfo(
                        packageName,
                        0
                    )

            packageInfo.lastUpdateTime >
                    packageInfo.firstInstallTime
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = SdkConfig.LOG_TAG

        private const val INSTALLATION_ID_FILE =
            "testing_sdk_installation_id"

        private const val LEGACY_PREFERENCES_NAME =
            "testing_sdk_preferences"

        private const val LEGACY_INSTALLATION_ID_KEY =
            "installation_id"

        private val INSTALLATION_LOCK =
            Any()
    }
}
