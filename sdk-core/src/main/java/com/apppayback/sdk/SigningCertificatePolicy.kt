package com.apppayback.sdk

/**
 * Seleciona os certificados que compõem a identidade estável do aplicativo.
 *
 * Uma linhagem com assinatura única conserva o certificado original no índice
 * zero, inclusive depois de uma rotação de chave. Aplicativos com múltiplos
 * signatários usam o conjunto atual completo em ordem determinística.
 */
internal object SigningCertificatePolicy {
    fun selectDigests(
        hasMultipleSigners: Boolean,
        currentDigests: List<String>,
        certificateHistoryDigests: List<String>
    ): List<String> {
        val selected = if (hasMultipleSigners) {
            currentDigests
        } else {
            certificateHistoryDigests.take(1)
        }

        return selected
            .distinct()
            .sorted()
    }
}
