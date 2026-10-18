package com.apppayback.sdk

import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale

/**
 * Avisos curtos ao testador: quando o check-in do dia começa e quando é
 * confirmado. O servidor envia os textos em todos os idiomas (e pode mudá-los
 * ou desligá-los); o idioma é escolhido aqui no aparelho e nunca é enviado.
 * Sem resposta do servidor, valem os textos padrão do SDK.
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

    /** idioma (pt, en, es) -> (início, confirmado), vindos do servidor. */
    @Volatile
    private var serverTexts: Map<String, Pair<String, String>> = emptyMap()

    private val enabledByDeveloper: Boolean by lazy { readManifestFlag() }

    fun updateFromServer(body: JSONObject) {
        val messages = body.optJSONObject("mensagens") ?: return
        serverAllows = messages.optBoolean("exibir", true)
        val texts = messages.optJSONObject("textos") ?: return
        val parsed = HashMap<String, Pair<String, String>>()
        for (key in CheckinMessageText.LANGUAGES) {
            val entry = texts.optJSONObject(key) ?: continue
            val started = CheckinMessageText.sanitize(entry.optString("inicio"))
            val confirmed = CheckinMessageText.sanitize(entry.optString("confirmado"))
            if (started != null && confirmed != null) {
                parsed[key] = Pair(started, confirmed)
            }
        }
        if (parsed.isNotEmpty()) serverTexts = parsed
    }

    fun showStarted() {
        val language = language()
        show(
            serverTexts[language]?.first
                ?: CheckinMessageText.defaultStarted(language)
        )
    }

    fun showConfirmed() {
        val language = language()
        show(
            serverTexts[language]?.second
                ?: CheckinMessageText.defaultConfirmed(language)
        )
    }

    private fun language(): String =
        CheckinMessageText.languageKey(Locale.getDefault().language)

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

    /** Idiomas com texto próprio; os demais usam inglês. */
    val LANGUAGES = listOf("pt", "en", "es")

    fun languageKey(language: String?): String {
        val key = language.orEmpty().lowercase(Locale.ROOT)
        return if (key in LANGUAGES) key else "en"
    }

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
