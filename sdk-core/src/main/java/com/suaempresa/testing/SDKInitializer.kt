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
         * Identifica somente a instalação atual.
         * Não representa a identidade global do testador.
         */
        val installationId =
            IdentityManager(applicationContext)
                .getOrCreateInstallationId()

        Log.d(
            TAG,
            "Installation ID carregado: $installationId"
        )

        /*
         * Consulta o Install Referrer sem interferir
         * no novo fluxo de check-in.
         */
        InstallReferrerManager(applicationContext)
            .checkAndSendReferrer(
                installationId
            )

        /*
         * Fingerprint e requisição HTTP são processados
         * fora da thread principal.
         */
        SDK_EXECUTOR.execute {
            validarEPrepararSdk(
                context = applicationContext,
                installationId = installationId
            )
        }

        Log.d(
            TAG,
            "Inicialização local do SDK concluída."
        )
    }

    private fun validarEPrepararSdk(
        context: Context,
        installationId: String
    ) {
        try {
            val fingerprintProvider =
                AppFingerprintProvider(context)

            val packageName =
                fingerprintProvider.getPackageName()

            val appFingerprint =
                fingerprintProvider
                    .getAppFingerprint()

            val status =
                SdkStatusClient().checkBlocking(
                    packageName = packageName,
                    appFingerprint = appFingerprint
                )

            when (status) {
                SdkRemoteStatus.ACTIVE -> {
                    Log.d(
                        TAG,
                        "SDK autorizado pelo backend."
                    )

                    prepararMonitoramento(
                        context = context,
                        packageName = packageName,
                        appFingerprint = appFingerprint,
                        installationId = installationId
                    )
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
                        "SDK inativo. Nenhum uso será contabilizado."
                    )
                }

                SdkRemoteStatus.UNAVAILABLE -> {
                    Log.w(
                        TAG,
                        "Status remoto indisponível. " +
                                "Nenhum uso será contabilizado."
                    )
                }
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Erro durante a validação inicial do SDK.",
                exception
            )
        }
    }

    /**
     * Ponto único de conexão do novo fluxo.
     *
     * O SessionManager antigo não deve ser utilizado aqui.
     */
    private fun prepararMonitoramento(
        context: Context,
        packageName: String,
        appFingerprint: String,
        installationId: String
    ) {
        Log.d(
            TAG,
            "Aplicativo preparado para o novo check-in: " +
                    "package=[$packageName], " +
                    "installation=[$installationId]."
        )

        /*
         * O novo DailyCheckinManager será conectado aqui
         * depois que as rotas autenticadas forem criadas.
         *
         * ProcessLifecycleOwner
         *     .get()
         *     .lifecycle
         *     .addObserver(dailyCheckinManager)
         */
    }

    override fun dependencies():
            List<Class<out Initializer<*>>> {

        return listOf(
            androidx.lifecycle
                .ProcessLifecycleInitializer::class.java
        )
    }

    companion object {
        private const val TAG =
            "TestingSDK"

        private val SDK_EXECUTOR =
            Executors.newSingleThreadExecutor()
    }
}