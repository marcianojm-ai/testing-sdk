package com.suaempresa.testing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log

/**
 * Identifica a origem atual da instalação do aplicativo.
 *
 * O valor enviado pelo SDK serve para registro e exibição.
 * Em produção, a autorização definitiva será validada
 * pelo backend com Play Integrity.
 */
internal class InstallSourceDetector(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    /**
     * Retorna uma das origens aceitas pelo backend:
     *
     * - google_play
     * - development
     * - external
     * - unknown
     */
    fun detect(): String {
        return try {
            /*
             * Builds debuggable têm prioridade.
             * Assim, aplicativos executados pelo Android Studio
             * nunca serão confundidos com instalações da Play Store.
             */
            if (isDebuggableApplication()) {
                return DEVELOPMENT
            }

            val installerPackage =
                getInstallerPackageName()

            when (installerPackage) {
                GOOGLE_PLAY_PACKAGE -> {
                    GOOGLE_PLAY
                }

                null, "" -> {
                    EXTERNAL
                }

                else -> {
                    EXTERNAL
                }
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Não foi possível identificar a origem " +
                    "da instalação.",
                exception
            )

            UNKNOWN
        }
    }

    private fun getInstallerPackageName(): String? {
        val packageManager =
            applicationContext.packageManager

        val packageName =
            applicationContext.packageName

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {
            val installSourceInfo =
                packageManager.getInstallSourceInfo(
                    packageName
                )

            installSourceInfo.installingPackageName
                ?: installSourceInfo.initiatingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(
                packageName
            )
        }
    }

    /**
     * Aplicativos executados pelo Android Studio
     * normalmente possuem a flag debuggable.
     */
    private fun isDebuggableApplication(): Boolean {
        val flags =
            applicationContext.applicationInfo.flags

        return (
            flags and ApplicationInfo.FLAG_DEBUGGABLE
        ) != 0
    }

    companion object {
        const val GOOGLE_PLAY = "google_play"
        const val DEVELOPMENT = "development"
        const val EXTERNAL = "external"
        const val UNKNOWN = "unknown"

        private const val GOOGLE_PLAY_PACKAGE =
            "com.android.vending"

        private const val TAG =
            "TestingSDK"
    }
}