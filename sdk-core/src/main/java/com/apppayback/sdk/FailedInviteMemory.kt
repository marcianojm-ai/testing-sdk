package com.apppayback.sdk

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Lembra convites recusados de vez pelo servidor (por exemplo, vencidos),
 * para não reenviá-los a cada abertura do app. Guarda só o hash.
 */
internal object FailedInviteMemory {
    const val MAX_ENTRIES = 5

    /**
     * Somente recusas que nunca mudam para o mesmo convite. Recusas que o
     * desenvolvedor pode corrigir (assinatura, cadastro) continuam sendo
     * tentadas de novo, como na versão anterior.
     */
    private val permanentCodes = setOf(
        "TOKEN_PARTICIPACAO_INVALIDO",
        "TOKEN_PARTICIPACAO_NAO_ENCONTRADO",
        "TOKEN_PARTICIPACAO_EXPIRADO",
        "TOKEN_PARTICIPACAO_NAO_AUTORIZADO",
        "TOKEN_PARTICIPACAO_UTILIZADO"
    )

    fun shouldRemember(status: Int, errorCode: String): Boolean =
        status in 400..499 &&
            status != 408 &&
            status != 425 &&
            status != 429 &&
            errorCode in permanentCodes

    fun hash(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun contains(stored: List<String>, token: String): Boolean =
        hash(token) in stored

    fun remember(stored: List<String>, token: String): List<String> {
        val tokenHash = hash(token)
        return (stored.filter { it != tokenHash } + tokenHash)
            .takeLast(MAX_ENTRIES)
    }
}
