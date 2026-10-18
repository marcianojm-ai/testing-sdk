package com.apppayback.sdk

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.util.Locale

/**
 * Dados técnicos enviados junto com cada chamada: versões do SDK, do app e
 * do Android, e o idioma do aparelho (para os avisos do check-in). Nenhum
 * dado pessoal é incluído.
 */
internal class ClientInfo(
    val appVersionCode: Long,
    private val appVersionName: String?
) {
    fun applyTo(target: JSONObject): JSONObject {
        target.put("sdk_version", SdkConfig.SDK_VERSION)
        target.put("android_sdk_int", Build.VERSION.SDK_INT)
        target.put("idioma", deviceLanguageTag())
        if (appVersionCode in 1L..Int.MAX_VALUE.toLong()) {
            target.put("app_version_code", appVersionCode)
        }
        appVersionName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(MAX_VERSION_NAME_LENGTH)
            ?.let { target.put("app_version_name", it) }
        return target
    }

    companion object {
        private const val MAX_VERSION_NAME_LENGTH = 64

        fun read(context: Context): ClientInfo =
            try {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
                val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
                ClientInfo(code, info.versionName)
            } catch (_: Exception) {
                ClientInfo(0L, null)
            }

        /** Somente idioma e região, por exemplo "pt-BR". */
        fun deviceLanguageTag(): String {
            val locale = Locale.getDefault()
            val language = locale.language.orEmpty()
            val country = locale.country.orEmpty()
            return when {
                language.isBlank() -> "en"
                country.isBlank() -> language
                else -> "$language-$country"
            }
        }
    }
}
