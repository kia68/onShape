package de.optadata.odil.onshape.foodimport

import de.optadata.odil.onshape.integrations.CsvReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Schweizer Naehrwertdatenbank (Bundesamt fuer Lebensmittelsicherheit und Veterinaerwesen BLV,
 * naehrwertdaten.ch) -- KONZEPT.md §16 SCALE-02, erste von vier genannten Regionaldatenbanken
 * (AT/CH/NL/ES). Nur CH ist hier umgesetzt, siehe docs/progress.md fuer die Recherche/Begruendung
 * zu AT (kein Download/API dokumentiert, Lizenz unklar), NL/NEVO (Lizenz ist klares CC-BY-4.0,
 * aber das exakte Export-Spaltenlayout ist ohne portalseitige Zustimmung/Registrierung nicht
 * verifizierbar) und ES/BEDCA (Nutzungsbedingungen-PDF technisch nicht lesbar, keine offizielle
 * API, nur ein inoffizieller Drittanbieter-Scraper) -- gleiches Vertagungsmuster wie MyFitnessPal/
 * Yazio in Epic #10 ("Format bzw. Lizenz konnte nicht mit ausreichender Sicherheit verifiziert werden").
 *
 * Lizenz fuer CH ist dagegen eindeutig geklaert (anders als BLS, siehe LEGAL-02/Issue #120, dort
 * noch offen): Download ist kostenlos und ausdruecklich auch fuer kommerzielle Zwecke erlaubt
 * (naehrwertdaten.ch/en/downloads). Kein oeffentliches API -- Bezug laeuft wie beim BLS-Reader
 * ueber einen Datei-Export (generic-foods.csv bzw. branded-foods.csv), lokal/im Deploy-Storage
 * abgelegt.
 *
 * Spaltenlayout nach dem oeffentlich dokumentierten Schema des GitHub-Mirrors
 * foodopendata/food-composition-ch (UTF-8, Komma-getrennt mit Header-Zeile: u.a. "ID", "name D",
 * "name E", "category D", dann je Naehrstoff eine Spaltengruppe mit dem Naehrstoffnamen selbst
 * als eindeutiger Spalte plus wiederholten generischen Geschwisterspalten "unit"/"matrix
 * unit"/"value type"/"source" -- deshalb per SpaltenNAME aufgeloest ([CsvReader.parseWithHeader])
 * statt fester Positionsindizes wie beim BLS-Reader: robuster gegen Spaltenverschiebung, aber
 * bei wiederholten generischen Spaltennamen wird nur die letzte Vorkommnis behalten, weshalb hier
 * NUR eindeutig benannte Kern-Naehrwertspalten gelesen werden (kcal/Eiweiss/Fett/Kohlenhydrate/
 * Ballaststoffe -- kein Zucker/Salz/Mikros: Salz z.B. staende nur als "sodium (Na)" in mg zur
 * Verfuegung, eine mg->g-Umrechnung ohne verifizierte Einheit waere ein Vollfaktor-Fehlrisiko wie
 * bei der Strong-Distanz in Epic #10 -- lieber weglassen als falsch).
 *
 * WICHTIG (wie beim BLS-Reader): das Spaltenlayout stammt aus einer Sekundaerquelle (GitHub-
 * Mirror), nicht aus einer selbst heruntergeladenen Originaldatei, und MUSS dagegen verifiziert
 * werden, bevor dieser Reader produktiv laeuft.
 */
@Component
class SwissFoodCompositionReader(
    @Value("\${swissfood.import-file-path:}") private val importFilePath: String,
) : FoodSourceClient {

    override val source = FoodSource.NAEHRWERTDATEN_CH

    override fun fetchDelta(): List<ImportedFood> {
        if (importFilePath.isBlank()) return emptyList()
        val path = Path.of(importFilePath)
        if (!Files.exists(path)) return emptyList()
        return CsvReader.parseWithHeader(path.readText(Charsets.UTF_8))
            .mapNotNull(::toImportedFood)
    }

    private fun toImportedFood(row: Map<String, String>): ImportedFood? {
        fun field(name: String): String? = row[name]?.trim()?.takeIf { it.isNotEmpty() }
        fun number(name: String): Double? = field(name)?.replace(',', '.')?.toDoubleOrNull()

        val id = field("ID") ?: return null
        val nameDe = field("name D") ?: return null
        val nameEn = field("name E") ?: nameDe
        val kcal = number("energy kcal") ?: return null

        return ImportedFood(
            source = FoodSource.NAEHRWERTDATEN_CH,
            sourceId = id,
            barcode = null,
            brand = null,
            nameDe = nameDe,
            nameEn = nameEn,
            category = field("category D"),
            kcal = kcal,
            proteinG = number("protein") ?: 0.0,
            fatG = number("fat, total") ?: 0.0,
            carbsG = number("carbohydrates, available") ?: 0.0,
            fiberG = number("dietary fibres"),
        )
    }
}
