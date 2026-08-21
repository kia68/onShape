package de.optadata.odil.onshape.foodimport

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class DeduplicatorTest {

    private val candidateOff = ImportedFood(
        source = FoodSource.OFF,
        sourceId = "off-1",
        barcode = "4000000000000",
        brand = "Testmarke",
        nameDe = "Testprodukt",
        nameEn = "Test product",
        kcal = 100.0,
        proteinG = 5.0,
        fatG = 2.0,
        carbsG = 15.0,
    )

    @Test
    fun `no match anywhere means insert new`() {
        val decision = Deduplicator.decide(candidateOff, sameSourceMatch = null, crossSourceMatch = null)
        assertIs<DedupDecision.InsertNew>(decision)
    }

    @Test
    fun `same source and source id means idempotent update`() {
        val existingId = UUID.randomUUID()
        val existing = ExistingFood(existingId, FoodSource.OFF, "off-1", TrustLevel.COMMUNITY)
        val decision = Deduplicator.decide(candidateOff, sameSourceMatch = existing, crossSourceMatch = null)
        assertIs<DedupDecision.UpdateExisting>(decision)
        kotlin.test.assertEquals(existingId, decision.existingId)
    }

    @Test
    fun `lower or equal trust cross-source match is skipped, never merged`() {
        // OFF candidate is COMMUNITY trust; an existing VERIFIED (e.g. BLS) row for the
        // same barcode outranks it -> candidate must be skipped, not merged into the BLS row.
        val existing = ExistingFood(UUID.randomUUID(), FoodSource.BLS, "bls-1", TrustLevel.VERIFIED)
        val decision = Deduplicator.decide(candidateOff, sameSourceMatch = null, crossSourceMatch = existing)
        assertIs<DedupDecision.SkipLowerTrustDuplicate>(decision)
    }

    @Test
    fun `higher trust candidate is inserted as its own row, not merged over the existing one`() {
        // A BLS candidate outranks an existing COMMUNITY (OFF) row with the same barcode.
        // Per the ODbL separation rule, the OFF row must stay untouched -> new row, no merge.
        val blsCandidate = candidateOff.copy(source = FoodSource.BLS, sourceId = "bls-9")
        val existingOff = ExistingFood(UUID.randomUUID(), FoodSource.OFF, "off-1", TrustLevel.COMMUNITY)
        val decision = Deduplicator.decide(blsCandidate, sameSourceMatch = null, crossSourceMatch = existingOff)
        assertIs<DedupDecision.InsertHigherTrustVariant>(decision)
    }
}
