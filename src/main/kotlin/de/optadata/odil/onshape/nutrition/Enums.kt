package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.onboarding.EnumWithDbValue

/** Spiegelt `meal_slot_t` aus V3__nutrition_log.sql. */
enum class MealSlot(override val dbValue: String) : EnumWithDbValue {
    BREAKFAST("breakfast"), LUNCH("lunch"), DINNER("dinner"),
    SNACK("snack"), PRE_WORKOUT("pre_workout"), POST_WORKOUT("post_workout"),
}

/** Spiegelt `entry_method_t` aus V3__nutrition_log.sql. */
enum class EntryMethod(override val dbValue: String) : EnumWithDbValue {
    SEARCH("search"), BARCODE("barcode"), PHOTO("photo"), VOICE("voice"),
    RECIPE("recipe"), QUICK_ADD("quick_add"), COPY("copy"),
}
