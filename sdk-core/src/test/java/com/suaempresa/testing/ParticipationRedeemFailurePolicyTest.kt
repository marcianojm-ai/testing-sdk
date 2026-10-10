package com.suaempresa.testing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipationRedeemFailurePolicyTest {
    @Test
    fun preservesPendingDataForTransientHttpResponses() {
        assertFalse(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                408,
                "TIMEOUT"
            )
        )
        assertFalse(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                429,
                "LIMITE_EXCEDIDO"
            )
        )
        assertFalse(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                503,
                "SERVICO_INDISPONIVEL"
            )
        )
    }

    @Test
    fun preservesPendingDataForUnavailableAppAndUnknownConflicts() {
        assertFalse(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                409,
                "APLICATIVO_INDISPONIVEL"
            )
        )
        assertFalse(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                409,
                "ESTADO_TRANSITORIO_FUTURO"
            )
        )
    }

    @Test
    fun clearsPendingDataOnlyForKnownTerminalCodes() {
        assertTrue(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                410,
                "TOKEN_PARTICIPACAO_EXPIRADO"
            )
        )
        assertTrue(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                403,
                "APP_FINGERPRINT_NAO_AUTORIZADO"
            )
        )
        assertTrue(
            ParticipationRedeemFailurePolicy.shouldClearPending(
                409,
                "TOKEN_PARTICIPACAO_UTILIZADO"
            )
        )
    }
}
