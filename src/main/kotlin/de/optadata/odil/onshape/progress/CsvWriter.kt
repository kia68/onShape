package de.optadata.odil.onshape.progress

/** Minimaler CSV-Writer nach RFC 4180 (Anfuehrungszeichen verdoppeln, Feld in Anfuehrungszeichen
 * einfassen wenn es Komma/Anfuehrungszeichen/Zeilenumbruch enthaelt). Absichtlich ohne
 * Bibliothek -- die Anforderung ist zu klein, um eine neue Abhaengigkeit zu rechtfertigen. */
object CsvWriter {
    fun write(header: List<String>, rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { it.escape() }).append("\r\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { (it?.toString() ?: "").escape() }).append("\r\n")
        }
        return sb.toString()
    }

    private fun String.escape(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${replace("\"", "\"\"")}\"" else this
}
