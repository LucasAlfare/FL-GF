package com.lucasalfare.flgf.core.game

/**
 * Thin gameplay helper that applies [ScoringRules] to mutable score state.
 */
class ScoringSystem(
  private val rules: ScoringRules = ScoringRules()
) {
  fun multiplierFor(combo: Int): Int = rules.multiplierFor(combo)

  fun registerHit(score: ScoreState, specialActive: Boolean): Int {
    score.combo++
    score.multiplier = rules.multiplierFor(score.combo)
    val points = rules.hitPoints(score.combo, specialActive)
    score.score += points
    return points
  }

  fun registerSustain(score: ScoreState, deltaMs: Double, specialActive: Boolean): Int {
    val points = rules.sustainPoints(deltaMs, score.multiplier, specialActive)
    score.score += points
    return points
  }

  fun resetCombo(score: ScoreState) {
    score.combo = 0
    score.multiplier = 1
  }

  fun canActivateSpecial(energy: Int): Boolean = rules.canActivateSpecial(energy)

  fun gainSpecialEnergy(currentEnergy: Int): Int = rules.gainSpecialEnergy(currentEnergy)

  fun drainSpecialEnergy(currentEnergy: Int, deltaMs: Long): Int = rules.drainSpecialEnergy(currentEnergy, deltaMs)

  fun updateSpecialDrain(special: SpecialState, deltaMs: Long) {
    if (deltaMs <= 0L) return

    special.drainAccumulator += rules.special.drainPerSecond * deltaMs / 1000.0
    val drainedEnergy = special.drainAccumulator.toInt()

    if (drainedEnergy > 0) {
      special.energy = (special.energy - drainedEnergy).coerceAtLeast(0)
      special.drainAccumulator -= drainedEnergy
    }

    if (special.energy <= 0) {
      special.energy = 0
      special.active = false
      special.drainAccumulator = 0.0
    }
  }
}
