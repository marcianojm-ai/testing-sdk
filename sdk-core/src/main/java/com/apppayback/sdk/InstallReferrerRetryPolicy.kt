package com.apppayback.sdk

/** Política pura para permitir testes JVM do limite e do backoff. */
internal class InstallReferrerRetryPolicy(
    private val maxAttempts: Int = 8,
    private val baseDelayMs: Long = 1_000L,
    private val maxDelayMs: Long = 60_000L
) {
    init {
        require(maxAttempts >= 1)
        require(baseDelayMs >= 0L)
        require(maxDelayMs >= baseDelayMs)
    }

    fun delayAfterFailure(completedAttempt: Int): Long? {
        if (completedAttempt < 1 || completedAttempt >= maxAttempts) {
            return null
        }

        val exponent = (completedAttempt - 1).coerceAtMost(30)
        return minOf(
            maxDelayMs,
            baseDelayMs * (1L shl exponent)
        )
    }
}
