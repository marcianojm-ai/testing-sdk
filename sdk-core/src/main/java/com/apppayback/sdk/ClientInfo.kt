package com.apppayback.sdk

import android.content.Context
import android.os.Build
import org.json.JSONObject

/**
 * O único dado técnico enviado além do necessário para o check-in é a versão
 * do próprio SDK. A versão do app (versionCode) é usada só localmente, para
 * saber quando consultar o servidor de novo depois de uma atualização.
 */
internal class ClientInfo(
    val appVersionCode: Long
) {
    fun applyTo(target: JSONObject): JSONObject =
        target.put("sdk_version", SdkConfig.SDK_VERSION)

    companion object {
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
                ClientInfo(code)
            } catch (_: Exception) {
                ClientInfo(0L)
            }
    }
}
