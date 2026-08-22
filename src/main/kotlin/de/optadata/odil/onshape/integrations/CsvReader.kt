package de.optadata.odil.onshape.integrations

/** Handgeschriebener RFC-4180-Parser (Gegenstueck zu [de.optadata.odil.onshape.progress.CsvWriter]
 * aus Epic Fortschritt) -- keine neue Abhaengigkeit. Zustandsautomat statt `split(",")`, weil
 * importierte Dateien (Hevy/Strong) angefuehrte Felder mit eingebetteten Kommas und Zeilenumbruechen
 * enthalten koennen (z.B. Notizfelder). */
object CsvReader {

    /** Reine Funktion: CSV-Text -> Zeilen aus rohen Feldern (kein Header-Handling hier). */
    fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        var rowHasContent = false

        fun endField() {
            row.add(field.toString())
            field = StringBuilder()
        }

        fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
            rowHasContent = false
        }

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = false
                    i++
                    continue
                }
                field.append(c)
                i++
                continue
            }
            when (c) {
                '"' -> { inQuotes = true; rowHasContent = true; i++ }
                ',' -> { endField(); rowHasContent = true; i++ }
                '\r' -> { i++ }
                '\n' -> { endRow(); i++ }
                else -> { field.append(c); rowHasContent = true; i++ }
            }
        }
        if (rowHasContent || field.isNotEmpty() || row.isNotEmpty()) endRow()

        return rows
    }

    /** CSV-Text mit Header-Zeile -> Liste von Zeilen als Spaltenname-zu-Wert-Map (fuer robustes
     * Auslesen per Spaltenname statt fragiler Positionsindizes -- Hevy/Strong-Exporte koennen ihre
     * Spaltenreihenfolge oder optionale Spalten (weight_kg vs. weight_lbs) aendern). */
    fun parseWithHeader(text: String): List<Map<String, String>> {
        val rows = parseRows(text).filter { it.isNotEmpty() && !(it.size == 1 && it[0].isBlank()) }
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1).map { cells -> header.indices.associate { idx -> header[idx] to (cells.getOrElse(idx) { "" }) } }
    }
}
