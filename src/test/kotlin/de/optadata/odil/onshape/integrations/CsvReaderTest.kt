package de.optadata.odil.onshape.integrations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvReaderTest {

    @Test
    fun `doppelte anfuehrungszeichen im feld werden zu einem einzelnen zeichen entschaerft`() {
        val rows = CsvReader.parseRows("\"say \"\"hi\"\"\"\n")
        assertEquals(listOf(listOf("say \"hi\"")), rows)
    }

    @Test
    fun `carriage return vor newline wird ignoriert (crlf-zeilenenden)`() {
        val rows = CsvReader.parseRows("a,b\r\n1,2\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun `eingebettete kommas in angefuehrten feldern werden nicht als trenner gewertet`() {
        val rows = CsvReader.parseRows("\"22 Dec 2025, 08:00\",normal\n")
        assertEquals(listOf(listOf("22 Dec 2025, 08:00", "normal")), rows)
    }

    @Test
    fun `leerer text ergibt bei parseWithHeader eine leere liste statt eines fehlers`() {
        assertEquals(emptyList(), CsvReader.parseWithHeader(""))
        assertEquals(emptyList(), CsvReader.parseWithHeader("\n"))
    }

    @Test
    fun `nur eine headerzeile ohne datenzeilen ergibt eine leere liste`() {
        assertEquals(emptyList(), CsvReader.parseWithHeader("a,b,c\n"))
    }

    @Test
    fun `zu kurze zeilen bekommen fuer fehlende spalten einen leeren wert statt eines fehlers`() {
        val result = CsvReader.parseWithHeader("a,b,c\n1,2\n")
        assertEquals(mapOf("a" to "1", "b" to "2", "c" to ""), result.single())
    }

    @Test
    fun `eine einzelne nicht-leere zelle ohne komma wird nicht faelschlich als leerzeile gefiltert`() {
        val result = CsvReader.parseWithHeader("a\nx\n")
        assertEquals(listOf(mapOf("a" to "x")), result)
    }

    @Test
    fun `mehrere datenzeilen werden vollstaendig auf spaltennamen gemappt`() {
        val result = CsvReader.parseWithHeader("a,b\n1,2\n3,4\n")
        assertTrue(result.size == 2)
        assertEquals(mapOf("a" to "1", "b" to "2"), result[0])
        assertEquals(mapOf("a" to "3", "b" to "4"), result[1])
    }
}
