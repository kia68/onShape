package de.optadata.odil.onshape.onboarding

interface EnumWithDbValue {
    val dbValue: String
}

/** Spiegelt die Postgres-Enums aus V1__extensions_users_profile.sql. */
enum class Sex(override val dbValue: String) : EnumWithDbValue {
    MALE("male"), FEMALE("female"), OTHER("other"), UNSPECIFIED("unspecified"),
}

enum class Goal(override val dbValue: String) : EnumWithDbValue {
    LOSE("lose"), GAIN_MUSCLE("gain_muscle"), GAIN_WEIGHT("gain_weight"),
    STRENGTH("strength"), MAINTAIN("maintain"), RECOMP("recomp"),
}

enum class Experience(override val dbValue: String) : EnumWithDbValue {
    NONE("none"), BEGINNER("beginner"), INTERMEDIATE("intermediate"), ADVANCED("advanced"),
}

enum class UnitSystem(override val dbValue: String) : EnumWithDbValue {
    METRIC("metric"), IMPERIAL("imperial"),
}
