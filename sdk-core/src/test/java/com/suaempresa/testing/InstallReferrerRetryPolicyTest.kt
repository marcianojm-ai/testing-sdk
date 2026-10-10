package com.suaempresa.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallReferrerRetryPolicyTest {
    @Test
    fun retriesWithBoundedExponentialBackoff() {
        val policy = InstallReferrerRetryPolicy()

        assertEquals(1_000L, policy.delayAfterFailure(1))
        assertEquals(2_000L, policy.delayAfterFailure(2))
        assertEquals(4_000L, policy.delayAfterFailure(3))
        assertEquals(32_000L, policy.delayAfterFailure(6))
        assertEquals(60_000L, policy.delayAfterFailure(7))
        assertNull(policy.delayAfterFailure(8))
    }

    @Test
    fun neverRetriesInvalidAttemptNumbers() {
        val policy = InstallReferrerRetryPolicy()

        assertNull(policy.delayAfterFailure(0))
        assertNull(policy.delayAfterFailure(9))
    }

    @Test
    fun validatesConfiguration() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            InstallReferrerRetryPolicy(maxAttempts = 0)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            InstallReferrerRetryPolicy(baseDelayMs = 2, maxDelayMs = 1)
        }
    }
}
