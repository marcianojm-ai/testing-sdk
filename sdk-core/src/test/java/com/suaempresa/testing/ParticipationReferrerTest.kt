package com.suaempresa.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipationReferrerTest {
    private val token = "A".repeat(43)

    @Test
    fun extractsVersionedTokenInAnyOrder() {
        assertEquals(
            token,
            ParticipationReferrer.extractToken(
                "utm_source=platform&ats_token=$token&ats_v=1"
            )
        )
    }

    @Test
    fun acceptsEncodedKeys() {
        assertEquals(
            token,
            ParticipationReferrer.extractToken(
                "ats%5Fv=1&ats%5Ftoken=$token"
            )
        )
        assertTrue(
            ParticipationReferrer.containsPlatformPayload(
                "ats%5Ftoken=$token"
            )
        )
    }

    @Test
    fun rejectsDuplicateOrUnsupportedFields() {
        assertNull(
            ParticipationReferrer.extractToken(
                "ats_v=1&ats_token=$token&ats_token=$token"
            )
        )
        assertNull(
            ParticipationReferrer.extractToken(
                "ats_v=2&ats_token=$token"
            )
        )
        assertNull(
            ParticipationReferrer.extractToken(
                "ats_token=$token"
            )
        )
    }

    @Test
    fun rejectsMalformedInvalidOrOversizedPayloads() {
        assertNull(
            ParticipationReferrer.extractToken(
                "ats_v=1&ats_token=${"B".repeat(42)}"
            )
        )
        assertNull(
            ParticipationReferrer.extractToken(
                "ats_v=1&ats_token=%ZZ"
            )
        )
        assertNull(
            ParticipationReferrer.extractToken(
                "ats_v=1&ats_token=$token&padding=${"x".repeat(512)}"
            )
        )
    }

    @Test
    fun distinguishesOrdinaryAttribution() {
        assertFalse(
            ParticipationReferrer.containsPlatformPayload(
                "utm_source=google&utm_campaign=test"
            )
        )
        assertTrue(
            ParticipationReferrer.containsPlatformPayload(
                "ats_v=1&ats_token=$token"
            )
        )
        assertTrue(
            ParticipationReferrer.containsPlatformPayload(
                "campaign=ats%5Ftoken%3D$token"
            )
        )
    }
}
