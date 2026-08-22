package de.optadata.odil.onshape.progress

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvWriterTest {

    @Test
    fun `einfache zeilen ohne sonderzeichen`() {
        val csv = CsvWriter.write(listOf("a", "b"), listOf(listOf(1, "x"), listOf(2, "y")))
        assertEquals("a,b\r\n1,x\r\n2,y\r\n", csv)
    }

    @Test
    fun `felder mit komma werden in anfuehrungszeichen eingefasst`() {
        val csv = CsvWriter.write(listOf("name"), listOf(listOf("Müsli, Schoko")))
        assertEquals("name\r\n\"Müsli, Schoko\"\r\n", csv)
    }

    @Test
    fun `anfuehrungszeichen im feld werden verdoppelt`() {
        val csv = CsvWriter.write(listOf("note"), listOf(listOf("sagte \"hallo\"")))
        assertEquals("note\r\n\"sagte \"\"hallo\"\"\"\r\n", csv)
    }

    @Test
    fun `null werte werden als leeres feld geschrieben`() {
        val csv = CsvWriter.write(listOf("x"), listOf(listOf(null)))
        assertEquals("x\r\n\r\n", csv)
    }
}
