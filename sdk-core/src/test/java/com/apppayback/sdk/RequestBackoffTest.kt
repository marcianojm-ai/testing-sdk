package com.apppayback.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class RequestBackoffTest {
    @Test
    fun growsResetsAndHonorsRetryAfter() {
        val backoff = RequestBackoff(
            baseDelayMs = 1_000L,
            maxDelayMs = 8_000L,
            maxRetryAfterMs = 20_000L,
            randomFraction = { 1.0 }
        )

        assertEquals(1_000L, backoff.delayAfterFailure())
        assertEquals(2_000L, backoff.delayAfterFailure())
        assertEquals(8_000L, backoff.delayAfterFailure(8_000L))
        backoff.reset()
        assertEquals(1_000L, backoff.delayAfterFailure())
        assertEquals(20_000L, backoff.delayAfterFailure(60_000L))
    }

    @Test
    fun appliesBoundedJitter() {
        val backoff = RequestBackoff(
            baseDelayMs = 1_000L,
            maxDelayMs = 8_000L,
            maxRetryAfterMs = 20_000L,
            randomFraction = { 0.5 }
        )

        assertEquals(900L, backoff.delayAfterFailure())
    }

    @Test
    fun keepsClientsDistributedAtMaximumDelay() {
        val earliest = RequestBackoff(
            baseDelayMs = 1_000L,
            maxDelayMs = 8_000L,
            maxRetryAfterMs = 20_000L,
            randomFraction = { 0.0 }
        )
        val latest = RequestBackoff(
            baseDelayMs = 1_000L,
            maxDelayMs = 8_000L,
            maxRetryAfterMs = 20_000L,
            randomFraction = { 1.0 }
        )

        repeat(4) {
            earliest.delayAfterFailure()
            latest.delayAfterFailure()
        }

        assertEquals(6_400L, earliest.delayAfterFailure())
        assertEquals(8_000L, latest.delayAfterFailure())
    }
}
