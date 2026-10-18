package com.apppayback.sdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SdkStatusCachePolicyTest {
    private val day = SdkStatusCachePolicy.SUCCESS_TTL_MS

    @Test
    fun skipsWithinTwentyFourHoursForSameVersion() {
        assertTrue(SdkStatusCachePolicy.shouldSkipCheck(1_000L, 42L, 1_000L + day - 1L, 42L))
    }

    @Test
    fun checksAgainAfterTwentyFourHours() {
        assertFalse(SdkStatusCachePolicy.shouldSkipCheck(1_000L, 42L, 1_000L + day, 42L))
    }

    @Test
    fun checksAgainAfterAppUpdate() {
        assertFalse(SdkStatusCachePolicy.shouldSkipCheck(1_000L, 42L, 2_000L, 43L))
    }

    @Test
    fun checksWithoutPreviousSuccessOrWhenClockMovedBack() {
        assertFalse(SdkStatusCachePolicy.shouldSkipCheck(0L, 42L, 2_000L, 42L))
        assertFalse(SdkStatusCachePolicy.shouldSkipCheck(5_000L, 42L, 4_000L, 42L))
    }
}
