package com.apppayback.sdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckinRecoveryPolicyTest {
    @Test
    fun forgetsOnlyRecoverableStaleAttempts() {
        assertTrue(
            CheckinRecoveryPolicy.shouldForgetAttempt(
                409,
                "TENTATIVA_DESATUALIZADA",
                duringStart = true
            )
        )
        assertTrue(
            CheckinRecoveryPolicy.shouldForgetAttempt(
                404,
                "TENTATIVA_NAO_ENCONTRADA",
                duringStart = false
            )
        )
        assertFalse(
            CheckinRecoveryPolicy.shouldForgetAttempt(
                404,
                "APLICATIVO_NAO_CADASTRADO",
                duringStart = false
            )
        )
    }

    @Test
    fun classifiesPermanentAndTransientFailures() {
        assertTrue(
            CheckinRecoveryPolicy.isPermanentIdentityFailure(
                403,
                "PARTICIPACAO_INCONSISTENTE"
            )
        )
        assertTrue(CheckinRecoveryPolicy.isTransient(429))
        assertTrue(CheckinRecoveryPolicy.isTransient(503))
        assertFalse(CheckinRecoveryPolicy.isTransient(409))
    }
}
