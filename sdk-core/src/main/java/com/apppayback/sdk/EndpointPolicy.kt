package com.apppayback.sdk

import java.net.URI

/** Impede que uma variante publicada envie dados para um endpoint inseguro. */
internal object EndpointPolicy {
    fun normalize(
        value: String,
        requireHttps: Boolean
    ): String {
        val normalized = value.trim().trimEnd('/')
        val uri = URI(normalized)

        require(uri.isAbsolute)
        require(uri.host?.isNotBlank() == true)
        require(uri.userInfo == null)
        require(uri.query == null)
        require(uri.fragment == null)
        require(
            uri.scheme == "https" ||
                (!requireHttps && uri.scheme == "http")
        )

        return normalized
    }
}
