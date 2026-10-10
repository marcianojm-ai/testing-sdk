package com.suaempresa.testing

internal object CheckinRecoveryPolicy {
    private val staleStartCodes = setOf(
        "TENTATIVA_NAO_ENCONTRADA",
        "TENTATIVA_DESATUALIZADA",
        "CHECKIN_ATUAL_EXISTENTE",
        "CHECKIN_JA_VALIDADO"
    )

    fun shouldForgetAttempt(
        status: Int,
        errorCode: String,
        duringStart: Boolean
    ): Boolean {
        if (duringStart && errorCode in staleStartCodes) return true
        return status == 404 && errorCode == "TENTATIVA_NAO_ENCONTRADA"
    }

    fun isPermanentIdentityFailure(
        status: Int,
        errorCode: String
    ): Boolean =
        status == 401 ||
            status == 403 ||
            errorCode in setOf(
                "PARTICIPACAO_NAO_ENCONTRADA",
                "APLICATIVO_NAO_CADASTRADO"
            )

    fun isTransient(status: Int): Boolean =
        status == 408 ||
            status == 425 ||
            status == 429 ||
            status >= 500
}
