package com.apppayback.sdk

/**
 * Depois que o servidor confirma a associação do app, instalações sem
 * participação não precisam consultar o servidor a cada abertura: uma
 * consulta por dia basta. Uma nova versão do app consulta de novo.
 */
internal object SdkStatusCachePolicy {
    const val SUCCESS_TTL_MS = 24L * 60L * 60L * 1_000L

    fun shouldSkipCheck(
        lastSuccessAtMs: Long,
        cachedVersionCode: Long,
        nowMs: Long,
        currentVersionCode: Long
    ): Boolean =
        lastSuccessAtMs > 0L &&
            nowMs >= lastSuccessAtMs &&
            nowMs - lastSuccessAtMs < SUCCESS_TTL_MS &&
            cachedVersionCode == currentVersionCode
}
