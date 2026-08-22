package de.optadata.odil.onshape.trainlog

/**
 * FR-97: "Epley + Brzycki, gemittelt". Beide Formeln werden mit steigenden Wiederholungen
 * zunehmend unzuverlaessig -- Brzycki hat bei 37 Wiederholungen sogar eine Polstelle (Nenner
 * `37 - reps`). Da KONZEPT.md keine explizite Wiederholungsobergrenze nennt, wird der Nenner
 * defensiv auf mindestens 1 geklemmt, statt Werte oberhalb von 36 Wiederholungen abzulehnen.
 */
object OneRepMaxCalculator {

    fun estimate(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps <= 0) return null
        if (reps == 1) return weightKg
        val epley = weightKg * (1 + reps / 30.0)
        val brzycki = weightKg * 36.0 / (37.0 - reps).coerceAtLeast(1.0)
        return (epley + brzycki) / 2.0
    }
}
