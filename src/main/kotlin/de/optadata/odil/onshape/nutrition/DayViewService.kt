package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.billing.SubscriptionService
import de.optadata.odil.onshape.billing.TierPolicy
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class DayViewService(
    private val foodEntryRepository: FoodEntryRepository,
    private val waterEntryRepository: WaterEntryRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val subscriptionService: SubscriptionService,
    private val rlsSession: RlsSession,
) {

    /** BIZ-01 (§15.1 "Mikronaehrstoffe: Basis (5)" im Free-Tier, siehe TierPolicy-KDoc). Tier
     * wird VOR dem `asUser`-Block ermittelt (der intern selbst `asUser` nutzt) -- verschachtelte
     * `SET LOCAL app.current_user_id`-Aufrufe in derselben Transaktion sind harmlos, aber
     * unnoetig. */
    fun forDate(userId: UUID, date: LocalDate, locale: String): DayView {
        val tier = subscriptionService.currentTier(userId)
        return rlsSession.asUser(userId) {
            val entries = foodEntryRepository.findByDateWithNames(userId, date, locale)
            val slots = MealSlot.entries.map { slot ->
                val slotEntries = entries.filter { it.entry.slot == slot }
                SlotSummary(
                    slot = slot,
                    entries = slotEntries,
                    kcal = slotEntries.sumOf { it.entry.kcal },
                    proteinG = slotEntries.sumOf { it.entry.proteinG },
                    fatG = slotEntries.sumOf { it.entry.fatG },
                    carbsG = slotEntries.sumOf { it.entry.carbsG },
                )
            }
            val waterMl = waterEntryRepository.findByDate(userId, date).sumOf { it.amountMl }
            val target = nutritionTargetRepository.findLatest(userId)?.result

            DayView(
                date = date,
                slots = slots,
                totalKcal = entries.sumOf { it.entry.kcal },
                totalProteinG = entries.sumOf { it.entry.proteinG },
                totalFatG = entries.sumOf { it.entry.fatG },
                totalCarbsG = entries.sumOf { it.entry.carbsG },
                totalMicros = TierPolicy.filterMicros(tier, MicroNutrients.sum(entries.map { it.entry.micros })),
                waterMl = waterMl,
                target = target,
            )
        }
    }
}
