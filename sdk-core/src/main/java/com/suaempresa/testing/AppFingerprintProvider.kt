package com.suaempresa.testing

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Gera uma identificação automática e determinística
 * para o aplicativo que incorporou o SDK.
 *
 * O desenvolvedor não precisa modificar ou configurar
 * manualmente o SDK.
 */
internal class AppFingerprintProvider(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    private val packageName =
        applicationContext.packageName

    /**
     * Retorna o package_name real do aplicativo
     * que está executando o SDK.
     */
    fun getPackageName(): String {
        return packageName
    }

    /**
     * Gera o fingerprint a partir da combinação:
     *
     * package_name + certificados de assinatura do APK.
     *
     * Aplicativos com o mesmo package_name, mas assinados
     * com chaves diferentes, terão fingerprints diferentes.
     */
    fun getAppFingerprint(): String {
        val certificateDigests =
            getSigningCertificateDigests()

        require(certificateDigests.isNotEmpty()) {
            "Nenhum certificado de assinatura foi encontrado."
        }

        val fingerprintSource =
            buildString {
                append(packageName)
                append("|")

                append(
                    certificateDigests
                        .sorted()
                        .joinToString(",")
                )
            }

        return sha256(
            fingerprintSource.toByteArray(
                StandardCharsets.UTF_8
            )
        )
    }

    /**
     * Obtém os certificados usados para assinar
     * o aplicativo instalado.
     */
    @Suppress("DEPRECATION")
    private fun getSigningCertificateDigests():
            List<String> {

        val packageManager =
            applicationContext.packageManager

        val packageInfo =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

        val signatures =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {
                val signingInfo =
                    requireNotNull(
                        packageInfo.signingInfo
                    ) {
                        "Informações de assinatura indisponíveis."
                    }

                if (
                    signingInfo.hasMultipleSigners()
                ) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo
                        .signingCertificateHistory
                }
            } else {
                packageInfo.signatures
            }

        return signatures
            .orEmpty()
            .map { signature ->
                sha256(
                    signature.toByteArray()
                )
            }
            .distinct()
    }

    /**
     * Calcula um hash SHA-256 hexadecimal.
     */
    private fun sha256(
        value: ByteArray
    ): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value)

        return digest.joinToString("") { byte ->
            "%02x".format(
                byte.toInt() and 0xff
            )
        }
    }
}