package com.apppayback.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityForegroundTrackerTest {
    @Test
    fun navigationBetweenActivitiesStaysForeground() {
        val tracker = ActivityForegroundTracker()

        assertEquals(true, tracker.onStarted())
        assertNull(tracker.onStarted())
        assertNull(tracker.onStopped(isChangingConfiguration = false))
        assertEquals(false, tracker.onStopped(isChangingConfiguration = false))
    }

    @Test
    fun configurationChangeDoesNotLeaveForeground() {
        val tracker = ActivityForegroundTracker()

        assertEquals(true, tracker.onStarted())
        assertNull(tracker.onStopped(isChangingConfiguration = true))
        assertNull(tracker.onStarted())
        assertEquals(false, tracker.onStopped(isChangingConfiguration = false))
    }
}
