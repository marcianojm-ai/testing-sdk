package com.suaempresa.testing

/** Configuração compilada dentro de cada variante do SDK. */
internal object SdkConfig {
    val SDK_VERSION: String = BuildConfig.SDK_VERSION

    val API_BASE_URL: String = EndpointPolicy.normalize(
        value = BuildConfig.API_BASE_URL,
        requireHttps = BuildConfig.BUILD_TYPE == "release"
    )

    val SDK_STATUS_ENDPOINT: String =
        "$API_BASE_URL/sdk-status"

    const val CONNECTION_TIMEOUT_MS = 3_000
    const val READ_TIMEOUT_MS = 3_000
}
