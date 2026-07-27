package com.suaempresa.testing

/**
 * Configurações internas do SDK.
 *
 * Durante o desenvolvimento, o backend está sendo executado
 * localmente no computador.
 *
 * Antes da publicação definitiva do SDK, esta URL será
 * substituída por um endereço HTTPS de produção.
 */
internal object SdkConfig {

    const val SDK_VERSION = "1.0.0"

    const val API_BASE_URL =
        "http://192.168.100.3:5001/" +
        "android-tester-saas-prod/us-central1/api"

    const val EVENTS_ENDPOINT =
        "$API_BASE_URL/events"

    const val CONNECTION_TIMEOUT_MS = 15_000

    const val READ_TIMEOUT_MS = 15_000
}