package de.optadata.odil.onshape.partnerapi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PartnerApiKeyHasherTest {

    @Test
    fun `generated keys carry the expected prefix and are unique`() {
        val a = PartnerApiKeyHasher.generate()
        val b = PartnerApiKeyHasher.generate()
        assertTrue(a.startsWith("pak_live_"))
        assertTrue(b.startsWith("pak_live_"))
        assertNotEquals(a, b)
    }

    @Test
    fun `hash is deterministic for the same key`() {
        val key = PartnerApiKeyHasher.generate()
        assertEquals(PartnerApiKeyHasher.hash(key), PartnerApiKeyHasher.hash(key))
    }

    @Test
    fun `different keys hash differently`() {
        val a = PartnerApiKeyHasher.generate()
        val b = PartnerApiKeyHasher.generate()
        assertNotEquals(PartnerApiKeyHasher.hash(a), PartnerApiKeyHasher.hash(b))
    }

    @Test
    fun `display prefix never exposes the full key`() {
        val key = PartnerApiKeyHasher.generate()
        val prefix = PartnerApiKeyHasher.displayPrefix(key)
        assertTrue(prefix.length < key.length)
        assertTrue(key.startsWith(prefix))
    }
}
