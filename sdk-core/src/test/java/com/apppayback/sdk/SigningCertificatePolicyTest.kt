package com.apppayback.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SigningCertificatePolicyTest {
    @Test
    fun singleSignerKeepsOriginalCertificateAfterRotation() {
        assertEquals(
            listOf("original"),
            SigningCertificatePolicy.selectDigests(
                hasMultipleSigners = false,
                currentDigests = listOf("rotated"),
                certificateHistoryDigests =
                    listOf("original", "rotated")
            )
        )
    }

    @Test
    fun multipleSignersUseCurrentDeterministicSet() {
        assertEquals(
            listOf("current-a", "current-b"),
            SigningCertificatePolicy.selectDigests(
                hasMultipleSigners = true,
                currentDigests = listOf(
                    "current-b",
                    "current-a",
                    "current-b"
                ),
                certificateHistoryDigests =
                    listOf("old")
            )
        )
    }

    @Test
    fun missingCertificatesRemainInvalidForProviderValidation() {
        assertTrue(
            SigningCertificatePolicy.selectDigests(
                hasMultipleSigners = false,
                currentDigests = emptyList(),
                certificateHistoryDigests = emptyList()
            ).isEmpty()
        )
    }
}
