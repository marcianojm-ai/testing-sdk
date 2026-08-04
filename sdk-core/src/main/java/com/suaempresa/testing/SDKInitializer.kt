package com.suaempresa.testing

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import java.util.concurrent.Executors

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

        /*
         * O installation_id identifica somente a instalação atual.
         * Ele não representa a identidade global do testador.
         */
        val identityManager =
            IdentityManager(applicationContext)

        val installationId =
            identityManager.getOrCreateInstallationId()

        Log.d(
            TAG,
            "Installation ID carregado: $installationId"
        )

        /*
         * A consulta ao Install Referrer é assíncrona
         * e não bloqueia a abertura do aplicativo.
         */
        val referrerManager =
            InstallReferrerManager(applicationContext)

        referrerManager.checkAndSendReferrer(
            installationId
        )

        /*
         * A geração do fingerprint e a consulta HTTP
         * são executadas fora da thread principal.
         */
        SDK_EXECUTOR.execute {
            consultarStatusRemoto(
                applicationContext
            )
        }

        Log.d(
            TAG,
            "Inicialização local do SDK concluída."
        )
    }

    private fun consultarStatusRemoto(
        context: Context
    ) {
        try {
            val fingerprintProvider =
                AppFingerprintProvider(context)

            val packageName =
                fingerprintProvider.getPackageName()

            val appFingerprint =
                fingerprintProvider.getAppFingerprint()

            val status =
                SdkStatusClient().checkBlocking(
                    packageName = packageName,
                    appFingerprint = appFingerprint
                )

            when (status) {
                SdkRemoteStatus.ACTIVE -> {
                    Log.d(
                        TAG,
                        "SDK autorizado. O aplicativo está apto " +
                                "para iniciar o novo fluxo de check-in."
                    )

                    /*
                     * O novo gerenciador de check-in será
                     * conectado aqui depois que suas rotas
                     * autenticadas estiverem disponíveis.
                     */
                }

                SdkRemoteStatus.NEWLY_ASSOCIATED -> {
                    Log.d(
                        TAG,
                        "Aplicativo associado ao cadastro. " +
                                "Esta primeira abertura técnica " +
                                "não será contabilizada."
                    )
                }

                SdkRemoteStatus.INACTIVE -> {
                    Log.d(
                        TAG,
                        "SDK inativo para este aplicativo. " +
                                "Nenhum uso será contabilizado."
                    )
                }

                SdkRemoteStatus.UNAVAILABLE -> {
                    Log.w(
                        TAG,
                        "Não foi possível validar o SDK. " +
                                "Nenhum uso será contabilizado."
                    )
                }
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Erro durante a validação inicial do SDK. " +
                        "Nenhum uso será contabilizado.",
                exception
            )
        }
    }

    override fun dependencies():
            List<Class<out Initializer<*>>> {

        return emptyList()
    }

    companion object {
        private const val TAG =
            "TestingSDK"

        private val SDK_EXECUTOR =
            Executors.newSingleThreadExecutor()
    }
}