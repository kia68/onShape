package de.optadata.odil.onshape.foodimport

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

/**
 * BLS 4.0 (Bundeslebensmittelschluessel, Max Rubner-Institut) — KONZEPT.md §10.4/§11.1:
 * seit v4.0 lizenzfrei, aber kein oeffentliches API. Der Bezug laeuft ueber einen
 * Datei-Export von blsdb.de, der lokal/im Deploy-Storage abgelegt wird.
 *
 * WICHTIG (siehe KONZEPT.md §11.1-Tabelle): "Prüfen: exakte Nutzungsbedingungen von
 * blsdb.de vor Produktivnutzung." Das Spaltenlayout unten deckt die in §8.2 benoetigten
 * Kernfelder ab (Semikolon-getrennt: SBLS-Schluessel;Bezeichnung De;Bezeichnung En;
 * kcal;Eiweiss_g;Fett_g;Kohlenhydrate_g;Ballaststoffe_g je 100 g) und MUSS gegen das
 * tatsaechliche BLS-Exportformat verifiziert werden, bevor dieser Reader produktiv laeuft.
 */
@Component
class BlsFileImportReader(
    @Value("\${bls.import-file-path:}") private val importFilePath: String,
) : FoodSourceClient {

    override val source = FoodSource.BLS

    override fun fetchDelta(): List<ImportedFood> {
        if (importFilePath.isBlank()) return emptyList()
        val path = Path.of(importFilePath)
        if (!Files.exists(path)) return emptyList()
        return Files.readAllLines(path)
            .asSequence()
            .drop(1) // Kopfzeile
            .filter { it.isNotBlank() }
            .mapNotNull(::parseLine)
            .toList()
    }

    private fun parseLine(line: String): ImportedFood? {
        val cols = line.split(';')
        if (cols.size < 8) return null
        return ImportedFood(
            source = FoodSource.BLS,
            sourceId = cols[0].trim(),
            barcode = null,
            brand = null,
            nameDe = cols[1].trim(),
            nameEn = cols[2].trim(),
            kcal = cols[3].trim().replace(',', '.').toDoubleOrNull() ?: return null,
            proteinG = cols[4].trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
            fatG = cols[5].trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
            carbsG = cols[6].trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
            fiberG = cols[7].trim().replace(',', '.').toDoubleOrNull(),
        )
    }
}
