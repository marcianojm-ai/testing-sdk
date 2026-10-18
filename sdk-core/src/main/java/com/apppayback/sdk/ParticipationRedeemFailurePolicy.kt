package com.apppayback.sdk

/**
 * Separa respostas terminais das respostas que podem ser resolvidas
 * pelo servidor sem pedir um novo convite ao testador.
 */
internal object ParticipationRedeemFailurePolicy {
    private val terminalCodes = setOf(
        "TOKEN_PARTICIPACAO_INVALIDO",
        "PACKAGE_NAME_INVALIDO",
        "APP_FINGERPRINT_INVALIDO",
        "INSTALLATION_ID_INVALIDO",
        "CREDENCIAL_PARTICIPACAO_INVALIDA",
        "TOKEN_PARTICIPACAO_NAO_ENCONTRADO",
        "TOKEN_PARTICIPACAO_EXPIRADO",
        "TOKEN_PARTICIPACAO_NAO_AUTORIZADO",
        "TOKEN_PARTICIPACAO_UTILIZADO",
        "APLICATIVO_NAO_CADASTRADO",
        "APP_FINGERPRINT_NAO_AUTORIZADO",
        "USUARIO_BLOQUEADO",
        "PARTICIPACAO_JA_EXISTENTE",
        "CREDENCIAL_PARTICIPACAO_EM_USO"
    )

    fun shouldClearPending(
        status: Int,
        errorCode: String
    ): Boolean {
        if (
            status == 408 ||
            status == 425 ||
            status == 429 ||
            status >= 500
        ) {
            return false
        }

        return errorCode in terminalCodes
    }
}
