package com.apppayback.sdk

/**
 * Distingue uma troca interna de Activity de uma saída real do aplicativo.
 * Retorna true/false somente quando o estado global realmente muda.
 */
internal class ActivityForegroundTracker {
    private var startedActivities = 0
    private var changingConfiguration = false

    fun onStarted(): Boolean? {
        startedActivities++
        val enteredForeground =
            startedActivities == 1 && !changingConfiguration
        changingConfiguration = false
        return if (enteredForeground) true else null
    }

    fun onStopped(isChangingConfiguration: Boolean): Boolean? {
        if (startedActivities > 0) startedActivities--
        changingConfiguration = isChangingConfiguration
        return if (
            startedActivities == 0 &&
            !changingConfiguration
        ) {
            false
        } else {
            null
        }
    }
}
