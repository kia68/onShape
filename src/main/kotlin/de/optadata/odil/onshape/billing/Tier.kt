package de.optadata.odil.onshape.billing

import de.optadata.odil.onshape.onboarding.EnumWithDbValue

/** KONZEPT.md §15.1 Preisstruktur. */
enum class Tier(override val dbValue: String) : EnumWithDbValue {
    FREE("free"),
    PLUS("plus"),
    COACH("coach"),
}

enum class BillingPeriod(override val dbValue: String) : EnumWithDbValue {
    MONTHLY("monthly"),
    YEARLY("yearly"),
    /** BIZ-03: Einmalzahlung, kein wiederkehrendes Abo, kein `current_period_end`. */
    LIFETIME("lifetime"),
}

enum class SubscriptionStatus(override val dbValue: String) : EnumWithDbValue {
    ACTIVE("active"),
    CANCELED("canceled"),
}

/** Ein Kauf-Vorgang: welches Stripe-Preis-Objekt, welcher Tier/Zeitraum daraus folgt. Fuenf
 * feste Plaene statt freier tier+period-Kombination -- Stripe braucht ohnehin ein Preis-Objekt
 * pro Kombination, ein Enum macht das 1:1 explizit statt eine ungueltige Kombination erst zur
 * Laufzeit abzufangen. */
enum class CheckoutPlan(override val dbValue: String, val tier: Tier, val period: BillingPeriod) : EnumWithDbValue {
    PLUS_MONTHLY("plus_monthly", Tier.PLUS, BillingPeriod.MONTHLY),
    PLUS_YEARLY("plus_yearly", Tier.PLUS, BillingPeriod.YEARLY),
    COACH_MONTHLY("coach_monthly", Tier.COACH, BillingPeriod.MONTHLY),
    COACH_YEARLY("coach_yearly", Tier.COACH, BillingPeriod.YEARLY),
    /** BIZ-03 (§15.1: "129 € nach Hevys Vorbild"): KONZEPT nennt keinen Ziel-Tier fuer die
     * Einmalzahlung. Interpretation: 129 € liegt weit ueber dem Coach-Jahrespreis (69,99 €),
     * daher gewaehrt Lifetime den hoechsten Tier (COACH) dauerhaft -- analog zu Hevys Modell
     * (Lifetime = voller Pro-Zugang). */
    LIFETIME("lifetime", Tier.COACH, BillingPeriod.LIFETIME),
}
