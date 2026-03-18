package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.ScoreState

/**
 * Handles score, combo and multiplier logic.
 */
class ScoreSystem {
  fun apply(state: ScoreState, judgement: Judgement): ScoreState {
    val base = when (judgement) {
      Judgement.PERFECT -> 100
      Judgement.GOOD -> 70
      Judgement.MISS -> 0
    }

    val newCombo = if (judgement == Judgement.MISS) 0 else state.combo + 1
    val newMaxCombo = maxOf(state.maxCombo, newCombo)
    val newMultiplier = (newCombo / 10).coerceAtMost(4).coerceAtLeast(1)

    return state.copy(
      score = state.score + base * newMultiplier,
      combo = newCombo,
      maxCombo = newMaxCombo,
      multiplier = newMultiplier
    )
  }
}