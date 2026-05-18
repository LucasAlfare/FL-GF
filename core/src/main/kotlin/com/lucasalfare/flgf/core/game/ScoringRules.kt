package com.lucasalfare.flgf.core.game

/**
 * Single combo threshold that upgrades the score multiplier.
 */
data class ComboMultiplierTier(
  val comboThreshold: Int,
  val multiplier: Int
)

/**
 * Tunable rules for the special meter and special-active score behavior.
 */
data class SpecialScoringRules(
  val energyPerCompletedSpecialPhrase: Int = 25,
  val activationEnergyThreshold: Int = 50,
  val maximumEnergy: Int = 100,
  val drainPerSecond: Double = 25.0,
  val activeScoreMultiplier: Int = 2
)

/**
 * All score-related tuning knobs in one place.
 *
 * This keeps hit value, combo tiers, sustain value, and special meter behavior
 * centralized so gameplay balancing can be adjusted without touching the engine flow.
 */
data class ScoringRules(
  val pointsPerHit: Int = 50,
  val sustainPointsPerSecond: Double = 50.0,
  val comboMultiplierTiers: List<ComboMultiplierTier> = listOf(
    ComboMultiplierTier(comboThreshold = 10, multiplier = 2),
    ComboMultiplierTier(comboThreshold = 20, multiplier = 3),
    ComboMultiplierTier(comboThreshold = 30, multiplier = 4)
  ),
  val special: SpecialScoringRules = SpecialScoringRules()
) {
  fun multiplierFor(combo: Int): Int {
    return comboMultiplierTiers
      .sortedBy { it.comboThreshold }
      .lastOrNull { combo >= it.comboThreshold }
      ?.multiplier ?: 1
  }

  fun hitPoints(combo: Int, specialActive: Boolean): Int {
    return pointsPerHit * multiplierFor(combo) * specialScoreMultiplier(specialActive)
  }

  fun sustainPoints(deltaMs: Double, comboMultiplier: Int, specialActive: Boolean): Int {
    return (deltaMs / 1000.0 * sustainPointsPerSecond * comboMultiplier * specialScoreMultiplier(specialActive)).toInt()
  }

  fun canActivateSpecial(energy: Int): Boolean {
    return energy >= special.activationEnergyThreshold
  }

  fun drainSpecialEnergy(currentEnergy: Int, deltaMs: Long): Int {
    val drained = (special.drainPerSecond * deltaMs / 1000.0).toInt()
    return (currentEnergy - drained).coerceAtLeast(0)
  }

  fun gainSpecialEnergy(currentEnergy: Int): Int {
    return (currentEnergy + special.energyPerCompletedSpecialPhrase).coerceAtMost(special.maximumEnergy)
  }

  private fun specialScoreMultiplier(active: Boolean): Int {
    return if (active) special.activeScoreMultiplier else 1
  }
}
