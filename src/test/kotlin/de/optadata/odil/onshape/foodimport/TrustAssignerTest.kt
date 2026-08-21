package de.optadata.odil.onshape.foodimport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrustAssignerTest {

    @Test
    fun `bls usda and brand-verified are verified by default`() {
        assertEquals(TrustLevel.VERIFIED, TrustAssigner.assign(FoodSource.BLS))
        assertEquals(TrustLevel.VERIFIED, TrustAssigner.assign(FoodSource.USDA))
        assertEquals(TrustLevel.VERIFIED, TrustAssigner.assign(FoodSource.BRAND_VERIFIED))
    }

    @Test
    fun `off and user submissions are community by default`() {
        assertEquals(TrustLevel.COMMUNITY, TrustAssigner.assign(FoodSource.OFF))
        assertEquals(TrustLevel.COMMUNITY, TrustAssigner.assign(FoodSource.USER))
    }

    @Test
    fun `ai estimate is estimated`() {
        assertEquals(TrustLevel.ESTIMATED, TrustAssigner.assign(FoodSource.AI_ESTIMATE))
    }

    @Test
    fun `manual verification overrides the source default`() {
        assertEquals(TrustLevel.VERIFIED, TrustAssigner.assign(FoodSource.OFF, manuallyVerified = true))
    }

    @Test
    fun `verified outranks community outranks estimated`() {
        assertTrue(TrustLevel.VERIFIED > TrustLevel.COMMUNITY)
        assertTrue(TrustLevel.COMMUNITY > TrustLevel.ESTIMATED)
    }
}
