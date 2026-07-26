package com.suaempresa.testing

import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer

class SDKInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Log.d("TestingSDK", "SDK Iniciando de forma invisível...")

        // 1. Identidade do aparelho
        val identityManager = IdentityManager(context)
        val deviceId = identityManager.getOrCreateInstallationId()
        Log.d("TestingSDK", "Identidade do celular garantida: $deviceId")

        // 2. Leitura da Play Store
        val referrerManager = InstallReferrerManager(context)
        referrerManager.checkAndSendReferrer(deviceId)

        // 3. Inicia o monitoramento de tempo de uso (Sessão)
        val sessionManager = SessionManager(deviceId)
        ProcessLifecycleOwner.get().lifecycle.addObserver(sessionManager)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(androidx.lifecycle.ProcessLifecycleInitializer::class.java)
    }
}