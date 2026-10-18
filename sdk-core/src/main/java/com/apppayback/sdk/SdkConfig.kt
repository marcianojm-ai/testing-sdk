package com.apppayback.sdk

/** Configuração compilada dentro de cada variante do SDK. */
internal object SdkConfig {
    const val LOG_TAG = "AppPayBack"

    val SDK_VERSION: String = BuildConfig.SDK_VERSION

    /**
     * Endereço do site do AppPayBack (/sdk). O Hosting repassa as chamadas ao
     * servidor, que pode mudar no futuro sem exigir uma nova versão do SDK.
     */
    val API_BASE_URL: String = EndpointPolicy.normalize(
        value = BuildConfig.API_BASE_URL,
        requireHttps = BuildConfig.BUILD_TYPE == "release"
    )

    val SDK_STATUS_ENDPOINT: String =
        "$API_BASE_URL/sdk-status"

    /** Tolerante a redes móveis lentas e à partida a frio do servidor. */
    const val CONNECTION_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 10_000
}
