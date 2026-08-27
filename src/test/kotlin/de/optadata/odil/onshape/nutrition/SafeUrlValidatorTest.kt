package de.optadata.odil.onshape.nutrition

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class SafeUrlValidatorTest {

    @Test
    fun `oeffentliche ip-adresse wird akzeptiert`() {
        val url = SafeUrlValidator.validate("https://8.8.8.8/recipe")
        assertEquals("8.8.8.8", url.host)
    }

    @Test
    fun `loopback-adresse wird blockiert`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("http://127.0.0.1/actuator/env") }
    }

    @Test
    fun `localhost-hostname wird blockiert`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("http://localhost:8080/actuator/env") }
    }

    @Test
    fun `link-local-adresse wird blockiert`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("http://169.254.169.254/latest/meta-data") }
    }

    @Test
    fun `privates class-a-netz wird blockiert`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("http://10.0.0.5/") }
    }

    @Test
    fun `privates class-c-netz wird blockiert`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("http://192.168.1.1/") }
    }

    @Test
    fun `nicht-http-schema wird abgelehnt`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("ftp://example.com/recipe") }
    }

    @Test
    fun `file-schema wird abgelehnt`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("file:///etc/passwd") }
    }

    @Test
    fun `voellig ungueltige url wird abgelehnt`() {
        assertFailsWith<InvalidRecipeUrlException> { SafeUrlValidator.validate("not a url") }
    }
}
