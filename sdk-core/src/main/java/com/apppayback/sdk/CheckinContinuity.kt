package com.apppayback.sdk

/** Relógio local apenas agenda pedidos; nunca decide se o dia foi validado. */
internal class CheckinContinuity {
    private var generationValue = 0L
    private var foregroundValue = false
    private var connectedValue = false

    val generation: Long
        get() = synchronized(this) { generationValue }

    val foreground: Boolean
        get() = synchronized(this) { foregroundValue }

    val connected: Boolean
        get() = synchronized(this) { connectedValue }

    @Synchronized
    fun snapshot(): CheckinContinuitySnapshot =
        CheckinContinuitySnapshot(
            generation = generationValue,
            foreground = foregroundValue,
            connected = connectedValue
        )

    @Synchronized
    fun update(
        foreground: Boolean,
        connected: Boolean
    ) {
        if (
            (foregroundValue && !foreground) ||
            (connectedValue && !connected)
        ) {
            generationValue++
        }
        foregroundValue = foreground
        connectedValue = connected
    }

    @Synchronized
    fun setForeground(value: Boolean) =
        update(value, connectedValue)

    @Synchronized
    fun setConnected(value: Boolean) =
        update(foregroundValue, value)

    @Synchronized
    fun accepts(generation: Long): Boolean =
        foregroundValue &&
            connectedValue &&
            generationValue == generation
}

internal data class CheckinContinuitySnapshot(
    val generation: Long,
    val foreground: Boolean,
    val connected: Boolean
)
