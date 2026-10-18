package com.apppayback.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FailedInviteMemoryTest {
    private fun token(index: Int) = index.toString().padStart(43, 'a')

    @Test
    fun storesOnlyHashes() {
        val stored = FailedInviteMemory.remember(emptyList(), token(1))
        assertEquals(1, stored.size)
        assertEquals(64, stored[0].length)
        assertFalse(stored[0].contains(token(1)))
        assertTrue(FailedInviteMemory.contains(stored, token(1)))
        assertFalse(FailedInviteMemory.contains(stored, token(2)))
    }

    @Test
    fun keepsOnlyTheMostRecentEntriesWithoutDuplicates() {
        var stored = emptyList<String>()
        for (index in 1..7) stored = FailedInviteMemory.remember(stored, token(index))
        stored = FailedInviteMemory.remember(stored, token(7))
        assertEquals(FailedInviteMemory.MAX_ENTRIES, stored.size)
        assertFalse(FailedInviteMemory.contains(stored, token(2)))
        assertTrue(FailedInviteMemory.contains(stored, token(3)))
        assertTrue(FailedInviteMemory.contains(stored, token(7)))
    }

    @Test
    fun remembersOnlyPermanentRefusals() {
        assertTrue(FailedInviteMemory.shouldRemember(410, "TOKEN_PARTICIPACAO_EXPIRADO"))
        assertTrue(FailedInviteMemory.shouldRemember(409, "TOKEN_PARTICIPACAO_UTILIZADO"))
        assertFalse(FailedInviteMemory.shouldRemember(403, "APP_FINGERPRINT_NAO_AUTORIZADO"))
        assertFalse(FailedInviteMemory.shouldRemember(404, "APLICATIVO_NAO_CADASTRADO"))
        assertFalse(FailedInviteMemory.shouldRemember(409, "PARTICIPACAO_JA_EXISTENTE"))
        assertFalse(FailedInviteMemory.shouldRemember(429, "TOKEN_PARTICIPACAO_EXPIRADO"))
        assertFalse(FailedInviteMemory.shouldRemember(503, "TOKEN_PARTICIPACAO_EXPIRADO"))
    }
}
