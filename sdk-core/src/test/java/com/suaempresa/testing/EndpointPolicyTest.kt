package com.suaempresa.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EndpointPolicyTest {
    @Test
    fun normalizesProductionHttpsEndpoint() {
        assertEquals(
            "https://example.test/api",
            EndpointPolicy.normalize(
                " https://example.test/api/ ",
                requireHttps = true
            )
        )
    }

    @Test
    fun rejectsHttpForRelease() {
        assertThrows(IllegalArgumentException::class.java) {
            EndpointPolicy.normalize(
                "http://127.0.0.1:5001/api",
                requireHttps = true
            )
        }
    }

    @Test
    fun allowsLocalHttpOnlyWhenExplicitlyPermitted() {
        assertEquals(
            "http://127.0.0.1:5001/api",
            EndpointPolicy.normalize(
                "http://127.0.0.1:5001/api",
                requireHttps = false
            )
        )
    }

    @Test
    fun rejectsCredentialsQueryFragmentAndRelativeUrl() {
        listOf(
            "https://user@example.test/api",
            "https://example.test/api?token=secret",
            "https://example.test/api#fragment",
            "/relative/api"
        ).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                EndpointPolicy.normalize(value, requireHttps = true)
            }
        }
    }
}
