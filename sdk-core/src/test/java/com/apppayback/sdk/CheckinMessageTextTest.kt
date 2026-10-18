package com.apppayback.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckinMessageTextTest {
    @Test
    fun keepsNormalServerText() {
        assertEquals(
            "AppPayBack: check-in de hoje confirmado ✓",
            CheckinMessageText.sanitize("  AppPayBack: check-in de hoje confirmado ✓ ")
        )
    }

    @Test
    fun removesControlAndInvisibleCharacters() {
        assertEquals(
            "AppPayBack ok",
            CheckinMessageText.sanitize("App\u0000Pay​Back‮ ok\n")
        )
    }

    @Test
    fun rejectsEmptyOrTooLongText() {
        assertNull(CheckinMessageText.sanitize(null))
        assertNull(CheckinMessageText.sanitize("   "))
        assertNull(CheckinMessageText.sanitize("a".repeat(CheckinMessageText.MAX_LENGTH + 1)))
        assertEquals(
            CheckinMessageText.MAX_LENGTH,
            CheckinMessageText.sanitize("a".repeat(CheckinMessageText.MAX_LENGTH))?.length
        )
    }

    @Test
    fun defaultsFollowDeviceLanguageWithEnglishFallback() {
        assertTrue(CheckinMessageText.defaultStarted("pt").contains("Mantenha o app aberto"))
        assertTrue(CheckinMessageText.defaultStarted("es").contains("Mantén la app abierta"))
        assertTrue(CheckinMessageText.defaultStarted("de").contains("Keep the app open"))
        assertTrue(CheckinMessageText.defaultConfirmed("PT").contains("confirmado"))
        assertTrue(CheckinMessageText.defaultConfirmed("").contains("confirmed"))
    }
}
