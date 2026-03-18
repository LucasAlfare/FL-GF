package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.ScoreState

/**
 * Handles score, combo and multiplier logic.
 */
class ScoreSystem {

  fun apply(state: ScoreState, judgement: Judgement): ScoreState {
    return when (judgement) {

      Judgement.MISS -> {
        state.copy(combo = 0, multiplier = 1)
      }

      Judgement.GOOD,
      Judgement.PERFECT -> {
        val newCombo = state.combo + 1
        val newMultiplier = calculateMultiplier(newCombo)

        val base = when (judgement) {
          Judgement.PERFECT -> 100
          Judgement.GOOD -> 70
        }

        val gained = base * newMultiplier

        state.copy(
          score = state.score + gained,
          combo = newCombo,
          maxCombo = maxOf(state.maxCombo, newCombo),
          multiplier = newMultiplier
        )
      }
    }
  }

  private fun calculateMultiplier(combo: Int): Int {
    return when {
      combo >= 30 -> 4
      combo >= 20 -> 3
      combo >= 10 -> 2
      else -> 1
    }
  }
}