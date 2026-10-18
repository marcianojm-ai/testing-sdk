package com.apppayback.sdk

import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale

/**
 * Avisos curtos ao testador: quando o check-in do dia começa e quando é
 * confirmado. Os textos vêm do servidor (que pode mudá-los ou desligá-los);
 * sem resposta do servidor, valem os textos padrão no idioma do aparelho.
 *
 * O desenvolvedor pode desligar os avisos no AndroidManifest.xml:
 * <meta-data android:name="com.apppayback.sdk.SHOW_CHECKIN_MESSAGES"
 *            android:value="false" />
 */
internal class CheckinMessages(
    private val app: Application,
    private val main: Handler
) {
    @Volatile
    private var serverAllows = true

    @Volatile
    private var serverStarted: String? = null

    @Volatile
    private var serverConfirmed: String? = null

    private val enabledByDeveloper: Boolean by lazy { readManifestFlag() }

    fun updateFromServer(body: JSONObject) {
        val messages = body.optJSONObject("mensagens") ?: return
        serverAllows = messages.optBoolean("exibir", true)
        CheckinMessageText.sanitize(messages.optString("inicio"))?.let {
            serverStarted = it
        }
        CheckinMessageText.sanitize(messages.optString("confirmado"))?.let {
            serverConfirmed = it
        }
    }

    fun showStarted() {
        show(serverStarted ?: CheckinMessageText.defaultStarted(language()))
    }

    fun showConfirmed() {
        show(serverConfirmed ?: CheckinMessageText.defaultConfirmed(language()))
    }

    private fun language(): String = Locale.getDefault().language.orEmpty()

    private fun show(text: String) {
        if (!enabledByDeveloper || !serverAllows) return
        main.post {
            try {
                Toast.makeText(app, text, Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                // Um aviso nunca pode atrapalhar o aplicativo.
            }
        }
    }

    private fun readManifestFlag(): Boolean =
        try {
            val info = app.packageManager.getApplicationInfo(
                app.packageName,
                PackageManager.GET_META_DATA
            )
            info.metaData?.getBoolean(META_SHOW_MESSAGES, true) ?: true
        } catch (_: Exception) {
            true
        }

    companion object {
        const val META_SHOW_MESSAGES = "com.apppayback.sdk.SHOW_CHECKIN_MESSAGES"
    }
}

/** Regras puras dos textos, testáveis fora do Android. */
internal object CheckinMessageText {
    const val MAX_LENGTH = 140

    fun sanitize(value: String?): String? {
        if (value == null) return null
        val text = value
            .filter { char -> !char.isISOControl() && !isInvisible(char) }
            .trim()
        return if (text.isEmpty() || text.length > MAX_LENGTH) null else text
    }

    fun defaultStarted(language: String): String =
        when (language.lowercase(Locale.ROOT)) {
            "pt" -> "AppPayBack: check-in iniciado. Mantenha o app aberto por 1 minuto."
            "es" -> "AppPayBack: check-in iniciado. Mantén la app abierta durante 1 minuto."
            else -> "AppPayBack: check-in started. Keep the app open for 1 minute."
        }

    fun defaultConfirmed(language: String): String =
        when (language.lowercase(Locale.ROOT)) {
            "pt" -> "AppPayBack: check-in de hoje confirmado ✓"
            "es" -> "AppPayBack: check-in de hoy confirmado ✓"
            else -> "AppPayBack: today's check-in confirmed ✓"
        }

    private fun isInvisible(char: Char): Boolean =
        char in '​'..'‏' ||
            char in ' '..'‮' ||
            char in '⁠'..'⁯' ||
            char == '﻿'
}
