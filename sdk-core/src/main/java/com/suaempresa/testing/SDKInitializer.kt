package com.suaempresa.testing

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer

/**
 * Inicializa automaticamente o SDK quando o aplicativo é aberto.
 *
 * O aplicativo hospedeiro não precisa chamar nenhum método manualmente.
 */
class SDKInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val applicationContext =
            context.applicationContext

        Log.d(
            TAG,
            "SDK iniciando automaticamente."
        )

        // 1. Gera ou recupera a identidade persistente do aparelho.
        val identityManager =
            IdentityManager(applicationContext)

        val deviceId =
            identityManager.getOrCreateInstallationId()

        Log.d(
            TAG,
            "Identidade do aparelho: $deviceId"
        )

        // 2. Consulta e armazena o Google Play Install Referrer.
        val referrerManager =
            InstallReferrerManager(applicationContext)

        referrerManager.checkAndSendReferrer(deviceId)

        // 3. Monitora e envia as sessões de uso.
        val sessionManager = SessionManager(
            context = applicationContext,
            deviceId = deviceId
        )

        ProcessLifecycleOwner
            .get()
            .lifecycle
            .addObserver(sessionManager)

        Log.d(
            TAG,
            "SDK inicializado com sucesso."
        )
    }

    override fun dependencies():
        List<Class<out Initializer<*>>> {

        return listOf(
            androidx.lifecycle
                .ProcessLifecycleInitializer::class.java
        )
    }

    companion object {
        private const val TAG = "TestingSDK"
    }
}