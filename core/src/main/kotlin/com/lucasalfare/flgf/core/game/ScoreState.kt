package com.lucasalfare.flgf.core.game

/**
 *
 * Tracks scoring-related state.
 *
 * @property score Total accumulated score.
 *
 * @property combo Current combo streak.
 *
 * @property multiplier Score multiplier based on combo thresholds.
 */
data class ScoreState(var score: Int = 0, var combo: Int = 0, var multiplier: Int = 1)
