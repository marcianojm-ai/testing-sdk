package com.apppayback.sdk

/** Backoff exponencial limitado, com jitter injetável para testes determinísticos. */
internal class RequestBackoff(
    private val baseDelayMs: Long = 2_000L,
    private val maxDelayMs: Long = 5 * 60_000L,
    private val maxRetryAfterMs: Long = 15 * 60_000L,
    private val randomFraction: () -> Double = { Math.random() }
) {
    private var consecutiveFailures = 0

    init {
        require(baseDelayMs > 0L)
        require(maxDelayMs >= baseDelayMs)
        require(maxRetryAfterMs >= maxDelayMs)
    }

    fun reset() {
        consecutiveFailures = 0
    }

    fun delayAfterFailure(retryAfterMs: Long? = null): Long {
        val exponent = consecutiveFailures.coerceAtMost(20)
        consecutiveFailures++

        val exponential = minOf(
            maxDelayMs,
            baseDelayMs * (1L shl exponent)
        )
        val fraction = randomFraction().coerceIn(0.0, 1.0)
        val minimumDelay = (exponential * 0.8).toLong()
        val jitterRange = exponential - minimumDelay
        val calculated = minimumDelay +
            (jitterRange * fraction).toLong()
        val serverDelay = retryAfterMs
            ?.coerceIn(0L, maxRetryAfterMs)
            ?: 0L

        return maxOf(calculated, serverDelay)
    }
}
