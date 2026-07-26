package com.suaempresa.testing

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

class InstallReferrerManager(private val context: Context) {

    fun checkAndSendReferrer(installationId: String) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            // Sucesso! A Play Store respondeu. Vamos pegar a anotação:
                            val response = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer

                            // Aqui é onde nós capturamos o token mágico que veio do seu site
                            Log.d("TestingSDK", "Aparelho [$installationId] veio do link: $referrerUrl")

                            // No futuro, enviaremos essa dupla (Aparelho + Link) para o seu Banco de Dados

                        } catch (e: Exception) {
                            Log.e("TestingSDK", "Erro ao ler a Play Store", e)
                        } finally {
                            // Fechamos a porta educadamente para não gastar bateria do usuário
                            referrerClient.endConnection()
                        }
                    }
                    else -> {
                        Log.d("TestingSDK", "Sem anotações da Play Store no momento.")
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Se a Play Store cair no meio do processo, ignoramos por enquanto.
            }
        })
    }
}