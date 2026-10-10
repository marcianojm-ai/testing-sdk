package com.suaempresa.testing

import java.net.URLDecoder

/** Extrai somente convites emitidos pela plataforma, sem expor o referrer bruto. */
internal object ParticipationReferrer {
    private const val MAX_REFERRER_LENGTH = 512
    private const val TOKEN_KEY = "ats_token"
    private const val VERSION_KEY = "ats_v"
    private const val SUPPORTED_VERSION = "1"
    private val tokenPattern = Regex("^[A-Za-z0-9_-]{43}$")

    fun containsPlatformPayload(referrer: String?): Boolean {
        if (referrer.isNullOrBlank()) return false

        if (
            referrer.contains(TOKEN_KEY) ||
            referrer.contains(
                "ats%5ftoken",
                ignoreCase = true
            ) ||
            decode(referrer)?.contains(TOKEN_KEY) == true
        ) {
            return true
        }

        return referrer.split("&").any { component ->
            val rawKey = component.substringBefore("=", component)
            val decodedKey = decode(rawKey)
            decodedKey == TOKEN_KEY ||
                decodedKey == VERSION_KEY
        }
    }

    fun extractToken(referrer: String?): String? {
        if (referrer.isNullOrBlank() || referrer.length > MAX_REFERRER_LENGTH) return null

        var token: String? = null
        var version: String? = null

        for (component in referrer.split("&")) {
            if (component.isBlank() || !component.contains("=")) return null
            val rawKey = component.substringBefore("=")
            val rawValue = component.substringAfter("=", "")
            val key = decode(rawKey) ?: return null
            val value = decode(rawValue) ?: return null

            when (key) {
                TOKEN_KEY -> {
                    if (token != null || !tokenPattern.matches(value)) return null
                    token = value
                }
                VERSION_KEY -> {
                    if (version != null) return null
                    version = value
                }
            }
        }

        return token?.takeIf { version == SUPPORTED_VERSION }
    }

    private fun decode(value: String): String? =
        try {
            URLDecoder.decode(value, Charsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            null
        }
}
