package com.suaempresa.testing

import android.content.Context
import android.util.Log
import androidx.startup.Initializer

class SDKInicializer : Initializer<Unit> {

    override fun create(context: Context) {
        Log.d("TestingSDK", "SDK Iniciando de forma invisível...")

        // 1. Gera ou recupera o ID único (Blindagem contra reinstalação)
        val identityManager = IdentityManager(context)
        val deviceId = identityManager.getOrCreateInstallationId()
        Log.d("TestingSDK", "Identidade do celular garantida: $deviceId")

        // 2. Vai na Play Store ver se o usuário veio pelo seu site (O Token Mágico)
        val referrerManager = InstallReferrerManager(context)
        referrerManager.checkAndSendReferrer(deviceId)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}